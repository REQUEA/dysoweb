// ========================================================================
// Copyright 2007 Requea.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.requea.dysoweb.processor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.searchpolicy.ContentClassLoader;
import org.apache.felix.moduleloader.IContentLoader;
import org.apache.felix.moduleloader.IURLPolicy;
import org.apache.jasper.Constants;
import org.apache.jasper.servlet.JspServlet;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.processor.IServletDefinition;
import com.requea.dysoweb.WebAppService;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.defaultservlet.DefaultServlet;
import com.requea.dysoweb.processor.definitions.ContextParam;
import com.requea.dysoweb.processor.definitions.FilterDefinition;
import com.requea.dysoweb.processor.definitions.ListenerDefinition;
import com.requea.dysoweb.processor.definitions.ServletDefinition;
import com.requea.dysoweb.servlet.jasper.DysowebCompilerAdapter;
import com.requea.dysoweb.servlet.jasper.EmbeddedServletOptions;
import com.requea.dysoweb.servlet.wrapper.FilterMappingDefinition;
import com.requea.dysoweb.servlet.wrapper.ServletMappingDefinition;
import com.requea.dysoweb.servlet.wrapper.ServletWrapper;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;
import com.requea.webenv.IWebProcessor;
import com.requea.webenv.IWebProcessorListener;
import com.requea.webenv.WebContext;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

public class RequestProcessor implements IWebProcessor {

	public static final Object NULL = new Integer(0);

	private DefaultServlet fDefaultServlet;
	private ServletContext fServletContext;
	
	private Map fActiveServicesMap = new ConcurrentHashMap();
	private List fEventListeners = new CopyOnWriteArrayList();
	private List fActiveBundles = new CopyOnWriteArrayList();

	private Map fTagLocations = new ConcurrentHashMap();
	private Map fTagBundles = new ConcurrentHashMap();

	private List fActiveDefinitions = new ArrayList();
	private Map fTaglibs = new ConcurrentHashMap();
	private RequestMapper fRequestMapper;
	private Map fEntries = new ConcurrentHashMap();

	private ConcurrentHashMap fContextParams;

	private List fContextListeners;
	private List fSessionListeners;
	private List fContextAttributeListeners;
	private List fSessionAttributeListeners;

	private Map       fJspWrappers;

	private Map       fServletContextWrappers;
	
    private static Log fLog = LogFactory.getLog(RequestProcessor.class);
	
	
	public RequestProcessor(ServletContext servletContext) {
		fServletContext = servletContext;
	}
	
	
	public void init() throws ServletException {
	
		// create the default servlet and initialize it
		fDefaultServlet = new DefaultServlet(this);
		fDefaultServlet.init(new Config());

		// create the required directories for the cache
		File fBase = new File(getScratchDir(), "jsp");
		fBase.mkdirs();
		
		fContextParams = new ConcurrentHashMap();
		fJspWrappers = new ConcurrentHashMap();
		fServletContextWrappers = new ConcurrentHashMap();
	}
	
	public void loadWebDescriptor(WebAppService service, long bundleId, URL url) throws WebAppException {
		
		try {
			InputStream is = url.openStream();
			Document doc = XMLUtils.parse(is);
			is.close();
			
			// scan the xml content and load the context descriptor
			Element el = XMLUtils.getFirstChild(doc.getDocumentElement());
			while(el != null) {
				String tagName = el.getTagName();
				if("display-name".equals(tagName)) {
					// ignore this one
				} else if("context-param".equals(tagName)) {
					String paramName = XMLUtils.getChildText(el, "param-name");
					String paramValue = XMLUtils.getChildText(el, "param-value");
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof ContextParam && paramName.equals(((ContextParam)item).getName())) {
							ContextParam def = (ContextParam)item;
							if(bundleId == def.getBundleId()) {
								// same bundle: do nothing
								found = true;
							} else {
								// same item, different bundles!
								throw new WebAppException("Cannot initialize twice the param " + paramName + " from bundle " + bundleId + " and bundle " + def.getBundleId());
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						ContextParam def = new ContextParam(bundleId, paramName, paramValue);
						fActiveDefinitions.add(def);
					}
				} else if("listener".equals(tagName)) {
					// this is a servlet context listener
					String className = XMLUtils.getChildText(el, "listener-class");
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof IListenerDefinition && className.equals(((IListenerDefinition)item).getClassName())) {
							IListenerDefinition def = (IListenerDefinition)item;
							if(bundleId == def.getBundleId()) {
								// same bundle: do nothing
								found = true;
							} else {
								// same item, different bundles!
								throw new WebAppException("Cannot initialize twice the listener " + className + " from bundle " + bundleId + " and bundle " + def.getBundleId());
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						IListenerDefinition def = createListenerDefinition(service, bundleId, className);
						fActiveDefinitions.add(def);
					}
				} else if("filter".equals(tagName)) {
					// this is a servlet filter definition
					String name = XMLUtils.getChildText(el, "filter-name");
					// check if we do not have already this one
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof IFilterDefinition && name.equals(((IFilterDefinition)item).getName())) {
							IFilterDefinition def = (IFilterDefinition)item;
							if(bundleId == def.getBundleId()) {
								// same bundle: do nothing
								found = true;
							} else {
								// same item, different bundles!
								throw new WebAppException("Cannot initialize twice the filter " + name + " from bundle " + bundleId + " and bundle " + def.getBundleId());
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						IFilterDefinition def = createFilterDefinition(service, bundleId, name, el);
						fActiveDefinitions.add(def);
					}
				} else if("filter-mapping".equals(tagName)) {
					// this is a filter mapping definition
					String filter = XMLUtils.getChildText(el, "filter-name");
					String pattern = XMLUtils.getChildText(el, "url-pattern");
					String servlet = XMLUtils.getChildText(el, "servlet-name");
					// check if we do not have already this one
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof FilterMappingDefinition) {
							FilterMappingDefinition fm = (FilterMappingDefinition)item;
							if(fm.getServletName() != null && 
									fm.getServletName().equals(servlet) &&
									fm.getFilterName().equals(filter))  {
								if(bundleId == fm.getBundleId()) {
									// same bundle, already registered: nothing to do
									found = true;
								} else {
									// same item, different bundles!
									throw new WebAppException("Cannot initialize twice the filter-mapping " + filter + ":" + servlet + " from bundle " + bundleId + " and bundle " + fm.getBundleId());
								}
							}
							if(fm.getURLPattern() != null && 
									fm.getURLPattern().equals(pattern) &&
									fm.getFilterName().equals(filter))  {
								FilterMappingDefinition def = (FilterMappingDefinition)item;
								if(bundleId == def.getBundleId()) {
									// same bundle, already registered: nothing to do
									found = true;
								} else {
									// same item, different bundles!
									throw new WebAppException("Cannot initialize twice the filter-mapping " + filter + ":" + servlet + " from bundle " + bundleId + " and bundle " + fm.getBundleId());
								}
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						FilterMappingDefinition def = new FilterMappingDefinition(bundleId, filter, servlet, pattern);
						fActiveDefinitions.add(def);
					}
				} else if("servlet".equals(tagName)) {
					// this is a servlet definition
					String name = XMLUtils.getChildText(el, "servlet-name");
					// check if we do not have already this one
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof IServletDefinition && name.equals(((IServletDefinition)item).getName())) {
							IServletDefinition def = (IServletDefinition)item;
							if(bundleId == def.getBundleId()) {
								// same bundle: do nothing
								found = true;
							} else {
								// same item, different bundles!
								throw new WebAppException("Cannot initialize twice the servlet " + name + " from bundle " + bundleId + " and bundle " + def.getBundleId());
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						IServletDefinition def = createServletDefinition(service, bundleId, name, el);
						fActiveDefinitions.add(def);
					}
				} else if("servlet-mapping".equals(tagName)) {
					// this is a servlet mapping definition
					String pattern = XMLUtils.getChildText(el, "url-pattern");
					String servlet = XMLUtils.getChildText(el, "servlet-name");
					// check if we do not have already this one
					boolean found = false;
					for(int i=0; i<fActiveDefinitions.size(); i++) {
						Object item = fActiveDefinitions.get(i);
						if(item instanceof ServletMappingDefinition && 
								((ServletMappingDefinition)item).getServletName().equals(servlet) &&
								((ServletMappingDefinition)item).getPattern().equals(pattern))  {
							ServletMappingDefinition def = (ServletMappingDefinition)item;
							if(bundleId == def.getBundleId()) {
								// same bundle, already registered: nothing to do
								found = true;
							} else {
								// same item, different bundles!
								throw new WebAppException("Cannot initialize twice the servlet-mapping " + pattern + "->" + servlet + " from bundle " + bundleId + " and bundle " + def.getBundleId());
							}
						}
					}
					// if not found in list, adds it the the list
					if(!found) {
						ServletMappingDefinition def = new ServletMappingDefinition(bundleId, pattern, servlet);
						fActiveDefinitions.add(def);
					}
				} else if("taglib".equals(tagName)) {
					// defined taglibs
					String uri = XMLUtils.getChildText(el, "taglib-uri");
					String location = XMLUtils.getChildText(el, "taglib-location");
					fTaglibs.put(uri, location);
				}
				// get the next element
				el = XMLUtils.getNext(el);
			}
		} catch(Exception e) {
			throw new WebAppException(e);
		}
	}
	
	
	
	class Config implements ServletConfig {

		public String getServletName() {
			return "default";
		}
		
		public String getInitParameter(String param) {
			return null;
		}

		public Enumeration getInitParameterNames() {
			return Collections.enumeration(Collections.EMPTY_LIST);
		}

		public ServletContext getServletContext() {
			return fServletContext;
		}

	}

	private class RequestWrapper extends HttpServletRequestWrapper {


		private String fPrefix;
		private String fContextPath;

		public RequestWrapper(HttpServletRequest request, String prefix) {
			super(request);
			fContextPath = request.getContextPath();
			fPrefix = prefix;
		}
		
		
		public String getContextPath() {
			return fContextPath + fPrefix;
		}


		public StringBuffer getRequestURL() {
			StringBuffer sb = super.getRequestURL();
			int length = fPrefix.length();
			if(sb.length() >= length && sb.substring(0, length-1).equals(fPrefix)) {
				StringBuffer sb2 = new StringBuffer(sb.length());
				sb2.append(fContextPath);
				sb2.append(sb.substring(length));
				return sb2;
			} else { 
				return sb;
			}
		}

		public String getServletPath() {
			String str = super.getServletPath();
			if(str.startsWith(fPrefix)) {
				return str.substring(fPrefix.length());
			} else {
				return str;
			}
		}
		public String getRealPath(String path) {
			return super.getRealPath(fPrefix+path);
		}

		public RequestDispatcher getRequestDispatcher(String path) {
			return super.getRequestDispatcher(fPrefix+path);
		}
		
	}
	
	/*
	 * Process the incoming Servlet request from the parent container
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void process(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		// retrieve the incoming URI to determine the appropriate servlet
		HttpServletRequest hsr = (HttpServletRequest)request;
		
        String uri = (String) request.getAttribute(Constants.INC_SERVLET_PATH);
        if (uri != null) {
            /*
             * Requested JSP has been target of
             * RequestDispatcher.include(). Its path is assembled from the
             * relevant javax.servlet.include.* request attributes
             */
            String pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");
			if (pathInfo != null) {
				uri += pathInfo;
			}
			// request included (with a request dispatcher)
	        String prefix = WebContext.getRequestPrefix();
	        if(prefix != null) {
	        	if(uri.startsWith(prefix)) {
		    		uri = uri.substring(prefix.length());
		    		request.setAttribute(Constants.INC_SERVLET_PATH, uri);
	        	}
	        	// wrap if not already done
	        	if(!(request instanceof RequestWrapper)) {
		        	request = new RequestWrapper((HttpServletRequest)request, prefix);
	        	}
	        }
        } else {
            /*
             * Requested JSP has not been the target of a 
             * RequestDispatcher.include(). Reconstruct its path from the
             * request's getServletPath() and getPathInfo()
             */
        	uri = hsr.getServletPath();
            String pathInfo = hsr.getPathInfo();
            if (pathInfo != null) {
                uri += pathInfo;
            }
            // remove the prefix and wrap if not already done
            String prefix = WebContext.getRequestPrefix();
            if(prefix != null && !(request instanceof RequestWrapper) && uri.startsWith(prefix)) {
        		uri = uri.substring(prefix.length());
            	request = new RequestWrapper((HttpServletRequest)request, prefix);
            }
        }
        
        Thread th = Thread.currentThread();
        ClassLoader cl = th.getContextClassLoader();
        try {
        	// first the filters associated to the URL are processed as a chain
        	IFilterDefinition[] filters = fRequestMapper.getFilters(uri);
			// remove the context path
			// get the servet wrapper for this request
			ServletWrapper wrapper = fRequestMapper.getServletWrapper(uri);
			// regular servlet?
			if(wrapper != null) {
				Long bundleId = (Long)request.getAttribute("com.requea.dysoweb.service");
				if(bundleId == null) {
					bundleId = new Long(wrapper.getBundleId());
					request.setAttribute("com.requea.dysoweb.service", bundleId);
				}
				// set the web app class loader as a thread context class loader
				if(wrapper.getProcessingContextClassLoader() == null)
					th.setContextClassLoader(WebContext.class.getClassLoader());
				else
					th.setContextClassLoader(wrapper.getProcessingContextClassLoader());
				
				ServletChain ac = ((ServletWrapper)wrapper).getChain();
				
				// build another chain with the filters
				if(filters != null && filters.length > 0) {
					FilterListChain fc = new FilterListChain(filters, ac);
					fc.doFilter(request, response);
				} else {
					ac.doFilter(request, response);
				}
				return;
			} else {
				// check if the request will be served by some static content
				EntryInfo ei = getEntryInfo(uri);
				if(ei != null) {
					// bundle static content
					fDefaultServlet.service(request, response);
					return;
				}
			}
        } finally {
        	th.setContextClassLoader(cl);
        }
        
		// regular container processing otherwise
        if(chain != null) {
        	chain.doFilter(request, response);
        }
	}

	private class ProxiedObject {
		private long fBundleId;
		private IWebProcessorListener fListener;
	}
	
	private class BundleInfo {

		private Bundle fBundle;
		private String fPath;
		private long fStartTime;
		private File fDevDir;

		public BundleInfo(Bundle bundle, String bundlePath, long l) {
			fBundle = bundle;
			fPath = bundlePath;
			fStartTime = l;
			// check if the bundle is in dev mode?
			fDevDir = null;
			String location = bundle.getLocation();
			if(location != null && location.startsWith("reference:file:")) {
				File f = new File(location.substring("reference:file:".length()));
				// check if there is a src
				if(f.getParentFile() != null) {
					File devDir = new File(f.getParentFile(), "src/main/webapp/");
					if(devDir.exists()) {
						fDevDir = devDir;
					}
				}
			}
		}

		public boolean isDev() {
			return fDevDir != null;
		}

		public File getDevDir() {
			return fDevDir;
		}
	}
	
	
	public class EntryInfo {

		private URL fURL;
		private long fLastModified;
		private long fBundleId;
		private URL fLocalURL;
		private String fPath;

		public EntryInfo(WebAppService service, long bundleId, URL url, long lastModified, String path) {
			fBundleId = bundleId;
			fURL = url;
			fLastModified = lastModified;
			fPath = path;
		}

		public long getLastModified() {
			if(fLastModified > 0) {
				return fLastModified;
			} else if(fURL != null) {
				// URL is a file?
				if("file".equals(fURL.getProtocol())) {
					File f = new File(fURL.toExternalForm().substring("file:".length()));
					if(f.exists()) {
						return f.lastModified();
					}
				}
				try {
					// open the cnx and check the date
					URLConnection cnx = fURL.openConnection();
					return cnx.getLastModified();
				} catch (IOException e) {
					// cannot get the date
					return 0L;
				}
			} else {
				return 0l;
			}
		}

		public URL getURL() {
			return fURL;
		}

		public URL getLocalURL() {
			if(fURL == null) {
				return null;
			} else if(fLocalURL != null) {
				return fLocalURL;
			} else if(!fURL.getProtocol().equals("bundle")) {
				return fURL;
			} else if(fPath != null && fPath.endsWith("web.xml")) {
				// never cache this one
				return fURL;
			} else {
				// not yet cached
				synchronized (this) {
					// try again if another thread has cached this one
					if(fLocalURL != null) 
						return fLocalURL;
					
					try {
						// cache the url content
						// build a cache name
						File file;
						if(fPath.startsWith("/"))
							file = new File(getScratchDir(), "/resources/"+fBundleId+fPath);
						else 
							file = new File(getScratchDir(), "/resources/"+fBundleId+"/"+fPath);
						
						// copy the file to the file directory
						file.getParentFile().mkdirs();
						URLConnection uc = fURL.openConnection();
						InputStream is = uc.getInputStream();
						FileOutputStream os = new FileOutputStream(file);
						byte[] buf = new byte[4096];
						int read;
						while ((read = is.read(buf)) > 0) {
							os.write(buf, 0, read);
						}
						os.close();
						is.close();
						fLocalURL = file.toURL();
						return fLocalURL;
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
					
				}
			}
		}
		public long getBundleId() {
			return fBundleId;
		}
	}
	

	/**
	 * Finds an entry in the list of webapp bundles registered 
	 * @param name
	 * @return
	 */
	public EntryInfo getEntryInfo(String name) throws IOException {
		// is this entry info already cached?
		Object obj = fEntries .get(name);
		if(obj != null) {
			return obj == RequestProcessor.NULL ? null : (EntryInfo)obj;
		} else {
			synchronized(fEntries) {
				// lookup again in case another thread has added the resource in the cache
				obj = fEntries .get(name);
				if(obj != null) {
					return obj == RequestProcessor.NULL ? null : (EntryInfo)obj;
				}
				// lookup into the bundles
				for(int i=0; i<fActiveBundles.size(); i++) {
					BundleInfo info = (BundleInfo)fActiveBundles.get(i);
					// get the service path
					String path = info.fPath;
					Bundle bundle = info.fBundle;
					WebAppService service = (WebAppService)fActiveServicesMap.get(new Long(bundle.getBundleId()));
					// is this bundle in dev mode?
					if(info.isDev()) {
						File f = new File(info.getDevDir(), name);
						if(f.exists()) {
							// return an entry info based on the direct src file
							EntryInfo ei = new EntryInfo(service, bundle.getBundleId(), f.toURL(), 0, name);
							return ei;
						}
					}
					// is there an entry for this bundle?
					String fullPath = path+name;
					URL url = bundle.getEntry(fullPath);
					if(url != null) {
						// For jasper, we must cache the jsp into the jsp directory
						// because felix will not return a correct time
						if(fullPath.endsWith(".jsp") || fullPath.endsWith("jspx")) {
							File fBase = new File(getScratchDir(), "jsp");
							// remove the leading character
							if(fullPath.startsWith("/") || fullPath.startsWith("\\")) {
								fullPath = fullPath.substring(1);
							}
							File f = new File(fBase, fullPath);
							// check if the file exists
							if(!f.exists() || f.lastModified() < info.fStartTime) {
								// copy the file to the file directory
								f.getParentFile().mkdirs();
								URLConnection uc = url.openConnection();
								InputStream is = uc.getInputStream();
								FileOutputStream os = new FileOutputStream(f);
								byte[] buf = new byte[4096];
								int read;
								while ((read = is.read(buf)) > 0) {
									os.write(buf, 0, read);
								}
								os.close();
								is.close();
								// set the time
								f.setLastModified(info.fStartTime);
							}
							url = new URL("file:"+f.getAbsolutePath());
						}
						// create the entry in the cache
						EntryInfo ei = new EntryInfo(service, bundle.getBundleId(), url, info.fStartTime, name);
						fEntries.put(name, ei);
						return ei;
					}
				}
				// nothing found
				fEntries.put(name, RequestProcessor.NULL);
				return null;
			}
		}
	}

	private class DysowebURLPolicy implements IURLPolicy {

		private IURLPolicy fParent;
		private long fBundleId;
		private Map fURLCache;
		private long fStartTime;
		
		DysowebURLPolicy(long bundleId, IURLPolicy parent, long lTime) {
			fParent = parent;
			fBundleId = bundleId;
			fURLCache = new ConcurrentHashMap();
			fStartTime = lTime;
		}
		
		public URL createURL(int id, String name) {
			int idx = name.indexOf('/');
			if(idx>0) {
				try {
					// get it from the cache?
					Object obj = fURLCache.get(name);
					if(obj == NULL) {
						return null;
					} else if(obj != null) {
						return (URL)obj;
					} else {
						// get the local URL
						URL osgiURL = fParent.createURL(id, name);
						if(osgiURL == null) {
							fURLCache.put(name, NULL);
							return null;
						} else if(!"bundle".equals(osgiURL.getProtocol())) {
							return osgiURL;
						} else if(name.startsWith("0/")) {
							return osgiURL;
						} else {
							synchronized (this) {
								// another thread has filled in the cache?
								obj = fURLCache.get(name);
								if(obj == NULL) {
									return null;
								} else if(obj != null) {
									return (URL)obj;
								}
								
								// do a local caching
								// locally cache the file
								File file;
								if(name.startsWith("/"))
									file = new File(getScratchDir(), "/resources/"+fBundleId+name);
								else 
									file = new File(getScratchDir(), "/resources/"+fBundleId+"/"+name);

								if(!file.exists() || file.lastModified() < fStartTime) {
									// get the parent file
									file.getParentFile().mkdirs();
									URLConnection uc = osgiURL.openConnection();
									InputStream is = uc.getInputStream();
									FileOutputStream os = new FileOutputStream(file);
									byte[] buf = new byte[4096];
									int read;
									while ((read = is.read(buf)) > 0) {
										os.write(buf, 0, read);
									}
									os.close();
									is.close();
								}
								URL localURL = file.toURL();
								fURLCache.put(name, localURL);
								return localURL;
							}
						}
					}
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				return fParent.createURL(id,name);
			}
		}
	}
	
	public synchronized void deploy(WebAppService service) throws WebAppException {
		
		if(service == null) {
			// do nothing
			return;
		}
		
		Bundle bundle = service.getBundle();
		Long key = new Long(bundle.getBundleId());
		
		if(fLog.isInfoEnabled()) {
			fLog.info("Deploying Dysoweb Application " + bundle.getBundleId() + 
					(bundle.getSymbolicName() == null ? "" : " ("+bundle.getSymbolicName()+")"));
		}
		
		fActiveServicesMap.put(key, service);
		
		// create a ServletContextWrapper
		fServletContextWrappers.put(key, new ServletContextWrapper(this, fServletContext, fContextParams));
		
		// normalize the web path
		String webPath = service.getWebPath();
		if(!webPath.endsWith("/")) {
			webPath = webPath + "/";
		}
		
		// update the search policy
		ClassLoader cl = service.getClass().getClassLoader();
		if(cl instanceof ContentClassLoader) {
	        IContentLoader contentLoader =
	            ((ContentClassLoader) cl).getContentLoader();
	        
	        // override the URL content policy handler 
	        contentLoader.setURLPolicy(new DysowebURLPolicy(bundle.getBundleId(), contentLoader.getURLPolicy(), System.currentTimeMillis()));
		}
		
		String bundlePath = "/"+webPath.substring(0, webPath.length()-1);
		fActiveBundles.add(new BundleInfo(bundle, bundlePath, System.currentTimeMillis()));

		// add the service elements Maps
		fServlets.put(key, new ConcurrentHashMap());
		fFilters.put(key, new ConcurrentHashMap());
		fListeners.put(key, new ConcurrentHashMap());
		
		// process WEB-INF files
		try {
			String path = webPath+"WEB-INF";
			processWebinfFiles(bundle, path, path);
		} catch (IOException e1) {
			throw new WebAppException(e1);
		}
		// process TLD in the class path
		try {
			processTLDinBundleClassPath(bundle);
		} catch (IOException e1) {
			throw new WebAppException(e1);
		}
		
		
		// load the web descriptor
		URL url = bundle.getEntry(webPath+"WEB-INF/web.xml");
		if(url != null) {
			// creates a copy of the existing mapper and add new entries into it
			loadWebDescriptor(service, bundle.getBundleId(), url);
		}

		
		loadAndInitElements();
		
		
		try {
			// creates a copy of the existing mapper and add new entries into it
			RequestMapper mapper = new RequestMapper(this, fRequestMapper);
			mapper.init(fActiveDefinitions);
			// set the mapper as the new mapper
			fRequestMapper = mapper;
		} catch (ServletException e) {
			throw new WebAppException(e);
		}
		
		fEntries.clear();
	}


	private void processTLDinBundleClassPath(Bundle bundle) throws IOException, WebAppException {
		// retrieve the bundle class path
		String classPath = (String) bundle.getHeaders().get("Bundle-ClassPath");
		if(classPath == null) 
			return; // nothing else to do
		
		StringTokenizer st = new StringTokenizer(classPath, ",");
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			// jar file?
			if(token.endsWith(".jar")) {
				// get the URL on the JAR
				URL entry = bundle.getEntry(token);
				if(entry != null) {
					InputStream is = entry.openStream();
					try {
						// get the stream as a zip file
						ZipInputStream zip = new ZipInputStream(is);
						ZipEntry ze = zip.getNextEntry();
						while ( ze != null ) {
							String name = ze.getName();
							if ( name.startsWith("META-INF/") && name.toLowerCase().endsWith(".tld")) {
								// this is a TLD!!!
								try {
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									byte[] buf = new byte[1024];
									int n;
									while ((n = zip.read(buf, 0, 1024)) > -1)
					                    bos.write(buf, 0, n);
									bos.close();
									
									ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
									Document doc = XMLUtils.parse(bis);
									String path = token+"$"+name.substring("META-INF/".length());
									if(!path.startsWith("/")) {
										path = "/" + path;
									}
									processTaglibDefinition(bundle.getBundleId(), doc, path);
								} catch(XMLException e) {
									throw new WebAppException(e);
								}
							}
							zip.closeEntry();
							ze = zip.getNextEntry();
						}
					} finally {
						is.close();
					}
				}
			}
			
		}
	}


	private void processWebinfFiles(Bundle bundle, String prefix, String path) throws IOException, WebAppException {
		int idx = path.lastIndexOf('/', path.length()-2);
		if(idx > 0) {
			if(path.charAt(idx+1) == '.') {
				// ignore this one
				return;
			}
		}
		// get all the entries in the web dir
		Enumeration e = bundle.getEntryPaths(path);
		while(e != null && e.hasMoreElements()) {
			String entry = (String)e.nextElement();
			String name = entry;
			if(name.startsWith(prefix)) {
				name = name.substring(prefix.length());
			}

			// deploy the entry
			processWebInfFiles(bundle, prefix, name);
			// add the entry and recurse if a directory
			if(entry.charAt(entry.length()-1) == '/') {
				processWebinfFiles(bundle, prefix, entry);
			}
		}
	}
	
	private void processWebInfFiles(Bundle bundle, String webPath, String path) throws IOException, WebAppException {
		
		
		String ext = null;
		int idx = path.lastIndexOf('.');
		if(idx > 0) {
			ext = path.substring(idx+1);
		}
		// list of things to ignore
		if(path.equalsIgnoreCase("web.xml")) {
			return;
		} else if(path.startsWith("classes") && "class".equalsIgnoreCase(ext)) {
			return;
		} else if(path.startsWith("lib") && "jar".equalsIgnoreCase(ext)) {
			return;
		}
		
		// get the corresponding path
		if(!path.endsWith("/") && "tld".equalsIgnoreCase(ext)) {
			URL url = bundle.getEntry(webPath+path);
			if(url == null)
				return;
			InputStream is = url.openStream();
			Document doc = null;
			try {
				doc = XMLUtils.parse(is);
			} catch(XMLException e) {
				throw new WebAppException(e);
			}
			is.close();
			// special treatment for not referenced taglib definition
			processTaglibDefinition(bundle.getBundleId(), doc, path);
		}	
	}

	private void processTaglibDefinition(long bundleId, Document doc, String path) throws WebAppException {
		try {
			// scan the xml content and load the context descriptor
			String tagURI = XMLUtils.getChildText(doc.getDocumentElement(), "uri");

			String baseName = "taglibs/bundle"+bundleId+path;
			File f = new File(getScratchDir(), baseName);
			f.getParentFile().mkdirs();
			// serialize the TLD 
	        String xml = XMLUtils.DocumentToString(doc);
	        // then write the content as utf-8
	        Writer w = new FileWriter(f);
	        w.write(xml);
	        w.close();
	        
	        if(tagURI != null) {
	        	fTagLocations.put(tagURI, "$scratch/"+baseName);
        		fTagLocations.put("/WEB-INF"+path, "$scratch/"+baseName);
	        	
	        	fTagBundles.put(tagURI, new Long(bundleId));
	        }

		} catch (IOException e) {
			throw new WebAppException(e);
		} catch (XMLException e) {
			throw new WebAppException(e);
		}
	}

	
	public synchronized void undeploy(Bundle bundle) throws WebAppException {

		long bundleId = bundle.getBundleId();
		if(fLog.isInfoEnabled()) {
			fLog.info("Undeploying Dysoweb Application " + bundle.getBundleId() + 
					(bundle.getSymbolicName() == null ? "" : " ("+bundle.getSymbolicName()+")"));
		}
		// remove from the list of active bundles
		for(int i=0; i<fActiveBundles.size(); ) {
			BundleInfo info = (BundleInfo)fActiveBundles.get(i);
			if(bundleId == info.fBundle.getBundleId()) {
				fActiveBundles.remove(i);
			} else {
				i++;
			}
		}
		
		Long bundleKey = new Long(bundleId);
		WebAppService service = (WebAppService)fActiveServicesMap.get(bundleKey);
		if(service != null) {
			fActiveServicesMap.remove(bundleKey);
			
			Map map = (Map)fServlets.remove(bundleKey);
			if(map != null) {
				Iterator iter = map.values().iterator();
				while(iter.hasNext()) {
					IServletDefinition servletDef = (IServletDefinition)iter.next();
					try {
						Servlet servlet = servletDef.getInstance();
						if(servlet != null)
							servlet.destroy();
					} catch(Exception e) {
						fLog.error("Unable to destroy servlet " + servletDef.getName());
					}
				}
			}
			
			map = (Map)fFilters.remove(bundleKey);
			if(map != null) {
				Iterator iter = map.values().iterator();
				while(iter.hasNext()) {
					IFilterDefinition filterDef = (IFilterDefinition)iter.next();
					try {
						Filter filter = filterDef.getInstance();
						if(filter != null) {
							filter.destroy();
						}
					} catch(Exception e) {
						fLog.error("Unable to destroy filter " + filterDef.getName());
					}
				}
			}

			// and same for listeners
			map = (Map)fListeners.remove(bundleKey);
			if(map != null) {
				Iterator iter = map.values().iterator();
				while(iter.hasNext()) {
					Object o = iter.next();
					if(o instanceof IListenerDefinition) {
						IListenerDefinition def = (IListenerDefinition)o;
						// notify the context
						if(def.isContextNotified() && def.getInstance() instanceof ServletContextListener) {
							ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(bundleKey);
							((ServletContextListener)def.getInstance()).contextDestroyed(new ServletContextEvent(contextWrapper));
							def.setContextNotified(false);
						}
					}
				}
			}

			// remove the jasper servlet as well
			fJspWrappers.remove(bundleKey);
			
			ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.remove(bundleKey);
			// cleanup context attributes left over
			if(contextWrapper != null) {
				contextWrapper.cleanupAttributes();
			}

		}
		
		
		// update taglib locations
		List lstTags = new ArrayList();
		Iterator iter = fTagBundles.keySet().iterator();
		while(iter.hasNext()) {
			String uri = (String)iter.next();
			Long b = (Long)fTagBundles.get(uri);
			if(b.equals(bundleKey)) {
				lstTags.add(uri);
			}
		}
		for(int i=0; i<lstTags.size(); i++) {
			String uri = (String)lstTags.get(i);
			fTagBundles.remove(uri);
			fTagLocations.remove(uri);
		}
		// notify all the listeners
		for(int i=0; i<fEventListeners.size(); ) {
			ProxiedObject p = (ProxiedObject)fEventListeners.get(i);
			if(p.fBundleId == bundleId) {
				p.fListener.onProxyUnloaded();
				fEventListeners.remove(i);
			} else {
				i++;
			}
		}
		

		// remove the definitions associated to this bundle
		removeBundleDefinitions(bundleId);
		
		// initialize the mapper
		try {
			// update the request mapper
			// creates a copy of the existing mapper and add new entries into it
			RequestMapper mapper = new RequestMapper(this, fRequestMapper);
			mapper.init(fActiveDefinitions);
			// set the mapper as the new mapper
			fRequestMapper = mapper;
		} catch (ServletException e) {
			throw new WebAppException(e);
		}
		
		// invalidate the resource cache
		fEntries.clear();
	}


	public ServletContext getServletContext() {
		return fServletContext;
	}

	private static final String TMP_DIR = "javax.servlet.context.tempdir";

	/*
	 * Retrieve the web container scratch directory
	 */
	public File getScratchDir() {
		// First try the Servlet 2.2 javax.servlet.context.tempdir property
		File scratchDir = (File) fServletContext.getAttribute(TMP_DIR);
		if (scratchDir == null) {
			// Not running in a Servlet 2.2 container.
			// Try to get the JDK 1.2 java.io.tmpdir property
			String dir = System.getProperty("java.io.tmpdir");
			if (dir != null)
				scratchDir = new File(dir);
		}
		return scratchDir;
	}

	/**
    * @return An array of two Strings: The first element denotes the real
    * path to the TLD. If the path to the TLD points to a jar file, then the
    * second element denotes the name of the TLD entry in the jar file.
    * Returns null if the uri is not associated with any tag library 'exposed'
    * in the web application.
	*/
	public String[] getTaglibLocation(String uri) {
		String path = getTaglib(uri);
		String relName = (String)fTagLocations.get(path);
		if(relName == null) {
			return null;
		} else {
			return new String[] { relName, null };
		}
	}
	
	
	private Map fServlets = new ConcurrentHashMap();
	private Map fFilters = new ConcurrentHashMap();
	private Map fListeners = new ConcurrentHashMap();

	private Map fServletWrappers;
	
	public IServletDefinition createServletDefinition(WebAppService service,
			long bundleId, String name, Element el) {
		
		// do we already have this servlet associated to the service?
		Map servlets = (Map)fServlets.get(new Long(bundleId));
		ServletDefinition def = (ServletDefinition)servlets.get(service);
		if(def != null) {
			// already got this one
			return def;
		}
		String cls = XMLUtils.getChildText(el, "servlet-class");
		String str = XMLUtils.getChildText(el, "load-on-startup");
		int loadOnStartup = 0;
		if(str != null) {
			loadOnStartup = Integer.parseInt(str);
		}
		def = new ServletDefinition(service, bundleId, name, cls, loadOnStartup);
		def.loadParams(el);
		servlets.put(name, def);
		return def;
	}

	public IFilterDefinition createFilterDefinition(WebAppService service, long bundleId, String name, Element el) {

		Map filters = (Map)fFilters.get(new Long(bundleId));
		FilterDefinition def = (FilterDefinition)filters.get(name);
		if(def != null) {
			return def;
		}
		
		String cls = XMLUtils.getChildText(el, "filter-class");
		def = new FilterDefinition(service, bundleId, name, cls);
		def.loadParams(el);
		filters.put(name, def);
		return def;
	}


	public IListenerDefinition createListenerDefinition(WebAppService service,
			long bundleId, String className) {

		Map listeners = (Map)fListeners.get(new Long(bundleId));
		ListenerDefinition def = (ListenerDefinition)listeners.get(className);
		if(def != null) {
			return def;
		}
		
		def = new ListenerDefinition(service, bundleId, className);
		listeners.put(className, def);
		return def;
	}


	public void processContextEvent(String eventType, ServletContextEvent event) {
		List listeners = fContextListeners;
		if(listeners == null || listeners.isEmpty()) {
			return;
		}
		for(int i=0; i<listeners.size(); i++) {
			ServletContextListener listener = (ServletContextListener)listeners.get(i);
			if("contextInitialized".equals(eventType)) {
				listener.contextInitialized(event);
			} else if("contextDestroyed".equals(eventType)) {
				listener.contextDestroyed(event);
			}
		}
	}

	public void processContextAttributeEvent(String eventType, ServletContextAttributeEvent event) {
		List listeners = fContextAttributeListeners;
		if(listeners == null || listeners.isEmpty()) {
			return;
		}
		for(int i=0; i<listeners.size(); i++) {
			ServletContextAttributeListener listener = (ServletContextAttributeListener)listeners.get(i);
			if("attributeAdded".equals(eventType)) {
				listener.attributeAdded(event);
			} else if("attributeRemoved".equals(eventType)) {
				listener.attributeRemoved(event);
			} else if("attributeReplaced".equals(eventType)) {
				listener.attributeReplaced(event);
			}
		}
	}

	public void processSessionAttributeEvent(String eventType, HttpSessionBindingEvent event) {
		List listeners = fSessionAttributeListeners;
		if(listeners == null || listeners.isEmpty()) {
			return;
		}
		for(int i=0; i<listeners.size(); i++) {
			HttpSessionAttributeListener listener = (HttpSessionAttributeListener)listeners.get(i);
			if("attributeAdded".equals(eventType)) {
				listener.attributeAdded(event);
			} else if("attributeRemoved".equals(eventType)) {
				listener.attributeRemoved(event);
			} else if("attributeReplaced".equals(eventType)) {
				listener.attributeReplaced(event);
			}
		}
	}


	public void processSessionEvent(String eventType, HttpSessionEvent event) {
		List listeners = fSessionListeners;
		if(listeners == null || listeners.isEmpty()) {
			return;
		}
		for(int i=0; i<listeners.size(); i++) {
			HttpSessionListener listener = (HttpSessionListener)listeners.get(i);
			if("sessionCreated".equals(eventType)) {
				listener.sessionCreated(event);
			} else if("sessionDestroyed".equals(eventType)) {
				listener.sessionDestroyed(event);
			}
		}
	}
	
	private void loadAndInitElements() {
		
		
		List contextListeners = new ArrayList();
		List contextAttributeListeners = new ArrayList();
		List sessionListeners = new ArrayList();
		List sessionAttributeListeners = new ArrayList();

		Thread th = Thread.currentThread();
		ClassLoader contextClassLoader = th.getContextClassLoader();
		// Phase0: initialize the listeners if not already done
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i);
			if(item instanceof ContextParam) {
				ContextParam param = (ContextParam)item;
				fContextParams.put(param.getName(), param.getValue());
			} else if(item instanceof IListenerDefinition) {
				IListenerDefinition def = (IListenerDefinition)item;
				try {
					th.setContextClassLoader(def.getService().getClass().getClassLoader());
					def.load();
					Object instance = def.getInstance();
					if(instance instanceof ServletContextListener) {
						contextListeners.add(instance);
						ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(new Long(def.getBundleId()));
						if(contextWrapper != null && !def.isContextNotified()) {
							fLog.info("Initializing Servlet Context listener " + def.getClassName());
							((ServletContextListener)instance).contextInitialized(new ServletContextEvent(contextWrapper));
							def.setContextNotified(true);
						}
					}
					if(instance instanceof ServletContextAttributeListener) {
						contextAttributeListeners.add(instance);
					}
					if(instance instanceof HttpSessionListener) {
						sessionListeners.add(instance);
					}
					if(instance instanceof HttpSessionAttributeListener) {
						sessionAttributeListeners.add(instance);
					}
				} catch(Exception e) {
					fLog.error("Unable to initialize a listener", e);
				}
			}
		}
		// Phase1 : initialize the filters
		Map filters = new HashMap();
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IFilterDefinition) {
				IFilterDefinition def = (IFilterDefinition)item;
				try {
					def.load();
					filters.put(def.getName(), def);
				} catch(Exception e) {
					fLog.error("Unable to initialize the filter " + def.getName(), e);
				}
			}
		}
		
		// Phase2: initialize the Jasper servlets
		// create the jasper servlets (one per active service)
		Iterator iter = fActiveServicesMap.keySet().iterator();
		while(iter.hasNext()) {
			Long l = (Long)iter.next();
			if(fJspWrappers.get(l) == null) {
				try {
					JspServlet jasperServlet = new JspServlet();
					ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(l);
					WebAppService service = (WebAppService)fActiveServicesMap.get(l);
					URL[] loaderURL = getLoaderURL(WebContext.class.getClassLoader());
					// set a URL class loader to make Jasper happy with dynamic class path compilation
					ClassLoader jspLoader = new URLClassLoader(loaderURL, new JasperLoader(service));
					th.setContextClassLoader(jspLoader);
					jasperServlet.init(new JasperConfig(contextWrapper));
					ServletWrapper jasperWrapper = new ServletWrapper(l.longValue(), contextWrapper, jasperServlet, jspLoader);
					jasperWrapper.setProcessingContextClassLoader(jspLoader);
					fJspWrappers.put(l, jasperWrapper);
				} catch(Exception e) {
					fLog.error("Unable to initialize the JSP servlet ", e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
				}
			}
		}
		
		// Phase2: initialize the other servlets and create the servlet wrappers
		Map servletWrappers = new HashMap();
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IServletDefinition) {
				IServletDefinition def = (IServletDefinition)item;
				ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(new Long(def.getBundleId()));
				ServletWrapper w = new ServletWrapper(def.getBundleId(), contextWrapper, def);
				try {
					def.load();
					servletWrappers.put(def.getName(), w);
				} catch(Exception e) {
					fLog.error("Unable to initialize the servlet " + def.getName(), e);
				}
			}
		}
		// Phase3: map the servlet wrappers and the filters that are mapped to a servlet
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof FilterMappingDefinition) {
				FilterMappingDefinition def = (FilterMappingDefinition)item;
				if(def.getServletName() != null) {
					// find the corresponding servlet wrapper
					ServletWrapper w = (ServletWrapper)servletWrappers.get(def.getServletName());
					// check if the servlet is on
					if(w != null) {
						IFilterDefinition flt = (IFilterDefinition)filters.get(def.getFilterName());
						if(flt == null) {
							fLog.error("Incorrect filter mapping definition. Unable to find filter "+def.getFilterName() + " for servlet " + def.getServletName());
						} else {
							w.addFilter(flt);
						}
					}
				}
			}
		}
		
		// Phase5: initialize the filters
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IFilterDefinition) {
				IFilterDefinition def = (IFilterDefinition)item;
				try {
					ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(new Long(def.getBundleId()));
					def.init(contextWrapper);
				} catch(Exception e) {
					fLog.error("Unable to initialize filter " + def.getName(), e);
				}
			}
		}
		
		// Phase6: initialize the servlets
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IServletDefinition) {
				IServletDefinition def = (IServletDefinition)item;
				try {
					if(def.getLoadOnStartup() > 0) {
						ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(new Long(def.getBundleId()));
						def.init(contextWrapper);
					}
				} catch(Exception e) {
					fLog.error("Unable to initialize servlet " + def.getName(), e);
				}
			}
		}	

		// activate the servlet wrappers and listeners
		fContextListeners = contextListeners;
		fContextAttributeListeners = contextAttributeListeners;
		fSessionListeners = sessionListeners;
		fSessionAttributeListeners = sessionAttributeListeners;
		
		fServletWrappers = servletWrappers;
	}


	private class JasperLoader extends ClassLoader {

		public JasperLoader(WebAppService service) {
			super(service.getClass().getClassLoader());
		}

		protected synchronized Class loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			try {
				return super.loadClass(name, resolve);
			} catch(ClassNotFoundException e) {
				// load from the processor loader 
				return this.getClass().getClassLoader().loadClass(name);
			}
		}
	}
	
	private URL[] getLoaderURL(ClassLoader webAppLoader) {
		URL[] parentURL;
		if(webAppLoader instanceof URLClassLoader) {
			// For TOMCAT the context class loader is a URLClass loader, ready to go for Jasper
			parentURL = ((URLClassLoader)webAppLoader).getURLs();
		} else {
			// for BEA WLS, that is another story 
			try {
				// add at least the core jar file to compile taglibs
				URL coreJar = fServletContext.getResource("/WEB-INF/lib/dysoweb-core-0.7.4-SNAPSHOT.jar");
				parentURL = new URL[] { coreJar };
			} catch (MalformedURLException e) {
				// use a default URL
				parentURL = new URL[0];
			}
		}
		IWebProcessor processor = WebContext.getProcessor();
		if(processor != null) {
			// try with the ProxyLoader if this happens to be a TagClass
			File f = WebContext.getScratchDir();
			URL[] proxyPath;
			try {
				proxyPath = new URL[] { f.toURL() };
			} catch (MalformedURLException e) {
				// cannot happen
				proxyPath = null;
			}
			URLClassLoader cl = new URLClassLoader(proxyPath, WebContext.class.getClassLoader());
			if(cl != null) {
				URL[] proxies = cl.getURLs();
				URL[] combined = new URL[parentURL.length+proxies.length];
				System.arraycopy(parentURL, 0, combined, 0, parentURL.length);
				System.arraycopy(proxies, 0, combined, parentURL.length, proxies.length);
				return combined;
			}
		}
		// otherwise, regular 
		return parentURL;
	}

	public String getTaglib(String uri) {
		String path = (String)fTaglibs.get(uri);
		if(path == null) {
			return uri;
		} else {
			return path;
		}
	}

	public void removeBundleDefinitions(long bundleId) {
		// remove the definitions for this bundle
		for(int i=0; i<fActiveDefinitions.size(); ) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IFilterDefinition && ((IFilterDefinition)item).getBundleId() == bundleId) {
				IFilterDefinition def = (IFilterDefinition)item;
				// destroy the filter
				def.unload();
				fActiveDefinitions.remove(i);
			} else if(item instanceof IServletDefinition && ((IServletDefinition)item).getBundleId() == bundleId) {
				IServletDefinition def = (IServletDefinition)item;
				// destroy the filter
				def.unload();
				fActiveDefinitions.remove(i);
			} else if(item instanceof FilterMappingDefinition && ((FilterMappingDefinition)item).getBundleId() == bundleId) {
				fActiveDefinitions.remove(i);
			} else if(item instanceof ServletMappingDefinition && ((ServletMappingDefinition)item).getBundleId() == bundleId) {
				fActiveDefinitions.remove(i);
			} else if(item instanceof IListenerDefinition && ((IListenerDefinition)item).getBundleId() == bundleId) {
				fActiveDefinitions.remove(i);
			} else {
				i++;
			}
		}
		
	}


	public ServletWrapper getJapserWrapper(Long bundleId) {
		return (ServletWrapper)fJspWrappers.get(bundleId);
	}
	
	class JasperConfig implements ServletConfig {
		
		// keep the servlet context wrapper at the JsaperConfig level for 
		// servlet initialization
		private ServletContextWrapper fServletContextWrapper;

		public JasperConfig(ServletContextWrapper contextWrapper) {
			fServletContextWrapper = contextWrapper;
		}
		
		
		public String getServletName() {
			return "jsp";
		}
		
		public String getInitParameter(String param) {
			if("engineOptionsClass".equals(param)) {
				return EmbeddedServletOptions.class.getName();
			} else if("compiler".equals(param)) {
				return DysowebCompilerAdapter.class.getName();
			} else {
				return null;
			}
		}

		public Enumeration getInitParameterNames() {
			return Collections.enumeration(Collections.EMPTY_LIST);
		}

		public ServletContext getServletContext() {
			return fServletContextWrapper;
		}

	}

	public IFilterDefinition getFilterByName(String name) {
		return (IFilterDefinition)fFilters.get(name);	
	}
	
	public ServletWrapper getServletWrapper(String name) {
		return (ServletWrapper)fServletWrappers.get(name);	
	}
	
	

}
