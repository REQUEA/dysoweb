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
import javax.servlet.http.HttpServletResponse;
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
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.processor.IServletDefinition;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.defaultservlet.DefaultServlet;
import com.requea.dysoweb.processor.definitions.ContextParam;
import com.requea.dysoweb.processor.definitions.FilterDefinition;
import com.requea.dysoweb.processor.definitions.JasperLoader;
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

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

public class RequestProcessor implements IWebProcessor {

	public static final Object NULL = new Integer(0);
	public static final java.lang.String BUNDLE_VERSION = "Bundle-Version";
	
	private DefaultServlet fDefaultServlet;
	private ServletContext fServletContext;
	
	private List fActiveBundleInfos = new CopyOnWriteArrayList();

	private Map fTagLocations = new ConcurrentHashMap();
	private Map fTagBundles = new ConcurrentHashMap();

	private List fActiveDefinitions = new ArrayList();
	private Map fTaglibs = new ConcurrentHashMap();
	private RequestMapper fRequestMapper;
	private Map fEntries = new ConcurrentHashMap();


	private List fContextListeners;
	private List fSessionListeners;
	private List fContextAttributeListeners;
	private List fSessionAttributeListeners;

	private Map  fContextParams = new ConcurrentHashMap();
	private Map  fJspWrappers = new ConcurrentHashMap();
	private Map  fServletContextWrappers = new ConcurrentHashMap();
	private Map  fFiltersByName = new ConcurrentHashMap();

	private String fPrefix;
	
    private static Log fLog = LogFactory.getLog(RequestProcessor.class);
	
	
	public RequestProcessor() {
	}
	
	
	public synchronized void activate(ServletContext context, String prefix) throws ServletException {
		
		fServletContext = context;
		
		fPrefix = prefix;
		
		// create the default servlet and initialize it
		fDefaultServlet = new DefaultServlet(this);
		fDefaultServlet.init(new Config());

		// create the required directories for the cache
		File fBase = new File(getScratchDir(), "jsp");
		fBase.mkdirs();
		
		fContextParams.clear();
		fJspWrappers.clear();
		fServletContextWrappers.clear();
		fFiltersByName.clear();
		
		// activate the bundles that have already been registered
		List lst = new ArrayList();
		for(int i=0; i<fActiveBundleInfos.size(); i++) {
			BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
			Long key = new Long(info.getBundle().getBundleId());
			if(fServletContextWrappers.get(key) == null) {
				// not deployed yet
				lst.add(info.getBundle());
			}
		}

		// activate the services that should be activated
		for(int i=0; i<lst.size(); i++) {
			Bundle bundle = (Bundle)lst.get(i);
			try {
				deploy(bundle);
			} catch(WebAppException e) {
				fLog.error("Unable to deploy service " + bundle.getBundleId(), e);
			}
		}
		
		// send the servlet context event
		processContextEvent("contextInitialized", new ServletContextEvent(context));
	}
	
	public synchronized void deactivate() {
		// send the servlet context event to the listeners
		processContextEvent("contextDestroyed", new ServletContextEvent(fServletContext));

		// deactivate the services that have been activated
		List lst = new ArrayList();
		for(int i=0; i<fActiveBundleInfos.size(); i++) {
			BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
			Long key = new Long(info.getBundle().getBundleId());
			if(fServletContextWrappers.get(key) != null) {
				// not deployed yet
				lst.add(info.getBundle());
			}
		}
		for(int i=0; i<lst.size(); i++) {
			Bundle bundle = (Bundle)lst.get(i);
			try {
				undeploy(bundle);
			} catch(WebAppException e) {
				fLog.error("Unable to deploy service " + bundle.getBundleId(), e);
			}
		}
	}
	
	public void loadWebDescriptor(BundleInfo bundleInfo, URL url) throws WebAppException {
		
		try {
			InputStream is = url.openStream();
			Document doc = XMLUtils.parse(is);
			is.close();

			long bundleId = bundleInfo.getBundle().getBundleId();
			
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
						IListenerDefinition def = createListenerDefinition(bundleInfo.getBundle(), className);
						if(def.getLoader() != null) {
							checkBundleInfoLoader(bundleInfo, def.getLoader());
						}
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
						IFilterDefinition def = createFilterDefinition(bundleInfo.getBundle(), name, el);
						if(def.getLoader() != null) {
							checkBundleInfoLoader(bundleInfo, def.getLoader());
						}
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
						IServletDefinition def = createServletDefinition(bundleInfo.getBundle(), name, el);
						if(def.getLoader() != null) {
							checkBundleInfoLoader(bundleInfo, def.getLoader());
						}
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
	
	
	
	private void checkBundleInfoLoader(BundleInfo bundleInfo, ClassLoader cl) {
		if(cl != null && bundleInfo.getContentLoader() == null) {
			// update the search policy
			if(cl instanceof ContentClassLoader) {
		        IContentLoader contentLoader =
		            ((ContentClassLoader) cl).getContentLoader();
		        
		        // override the URL content policy handler 
		        contentLoader.setURLPolicy(new DysowebURLPolicy(bundleInfo, contentLoader.getURLPolicy()));
			}
			bundleInfo.setContentLoader(cl);
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
	        if(fPrefix != null) {
	        	if(uri.startsWith(fPrefix)) {
		    		uri = uri.substring(fPrefix.length());
		    		request.setAttribute(Constants.INC_SERVLET_PATH, uri);
	        	}
	        	// wrap if not already done
	        	if(!(request instanceof RequestWrapper)) {
		        	request = new RequestWrapper((HttpServletRequest)request, fPrefix);
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
            if(fPrefix != null && !(request instanceof RequestWrapper) && uri.startsWith(fPrefix)) {
        		uri = uri.substring(fPrefix.length());
            	request = new RequestWrapper((HttpServletRequest)request, fPrefix);
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
					// build another chain with the filters
					if(filters != null && filters.length > 0) {
						DefaultServletChain fc = new DefaultServletChain(fDefaultServlet, filters);
						fc.doFilter(request, response);
					} else {
						// bundle static content
						fDefaultServlet.service(request, response);
					}
					return;
				}
			}
        } finally {
        	th.setContextClassLoader(cl);
        }
        
        // redirect to panel?
        HttpServletRequest hrequest = (HttpServletRequest)request;
        HttpServletResponse hresponse = (HttpServletResponse)response;
        if(uri.length() == 0) {
        	uri = "/";
        }
        if("/".equals(uri)) {
        	// is there a JSP page to forward to?
        	EntryInfo ei = getEntryInfo(uri+"index.jsp");
        	if(ei != null) {
        		RequestDispatcher rd = request.getRequestDispatcher(uri+"index.jsp");
        		rd.forward(request, response);
        		return;
        	} else {
        		// nothing to redirect to: we redirect to the panel
	        	hresponse.sendRedirect(hrequest.getContextPath()+"/dysoweb/panel/panel.jsp");
	        	return;
        	}
        } 

        if(chain != null) {
        	// regular chain processing
        	chain.doFilter(request, response);
        } else {
        	// not found
			hresponse.sendError(404);
        }
	}

	private class BundleInfo {

		private Bundle fBundle;
		private long   fLastModified;
		private File   fDevDir;
		private ClassLoader fLoader;

		public BundleInfo(Bundle bundle) {
			fBundle = bundle;
			fLastModified = bundle.getLastModified();
			// check if the bundle is in dev mode?
			fDevDir = null;
			String location = bundle.getLocation();
			if(location != null && location.startsWith("reference:file:")) {
				File f = new File(location.substring("reference:file:".length()));
				// check if there is a src
				if(f.getParentFile() != null) {
					// maven like structure?
					File devDir = new File(f.getParentFile(), "src/main");
					if(devDir.exists()) {
						fDevDir = devDir;
					}
				}
				if(fDevDir == null && f.exists() && f.isDirectory()) {
					// flat structure 
					fDevDir = f;
				}
			}
		}

		public ClassLoader getContentLoader() {
			return fLoader;
		}
		public void setContentLoader(ClassLoader loader) {
			fLoader = loader;
		}

		public boolean isDev() {
			return fDevDir != null;
		}

		public File getDevDir() {
			return fDevDir;
		}

		public Bundle getBundle() {
			return fBundle;
		}

		public long getLastModified() {
			return fLastModified;
		}
	}
	
	
	public class EntryInfo {

		private URL fURL;
		private long fLastModified;
		private long fBundleId;
		private URL fLocalURL;
		private String fPath;

		public EntryInfo(long bundleId, URL url, long lastModified, String path) {
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
				if(name.endsWith("/")) {
					// TODO: handle index file lists
					return null;
				} else {
					// lookup into the bundles
					for(int i=0; i<fActiveBundleInfos.size(); i++) {
						BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
						Bundle bundle = info.fBundle;
						// is this bundle in dev mode?
						if(info.isDev()) {
							File f = new File(info.getDevDir(), "webapp"+name);
							if(f.exists()) {
								// return an entry info based on the direct src file
								EntryInfo ei = new EntryInfo(bundle.getBundleId(), f.toURL(), 0, name);
								fEntries.put(name, ei);
								return ei;
							}
						}
						// is there an entry for this bundle?
						String fullPath = "/webapp"+name;
						URL url = bundle.getEntry(fullPath);
						if(url != null) {
							// For jasper, we must cache the jsp into the jsp directory
							// because felix will not return a correct time
							long lastModified = info.fLastModified;
							
							if(fullPath.endsWith(".jsp") || fullPath.endsWith("jspx")) {
								File fBase = new File(getScratchDir(), "jsp");
								// remove the leading character
								if(fullPath.startsWith("/") || fullPath.startsWith("\\")) {
									fullPath = fullPath.substring(1);
								}
								File f = new File(fBase, fullPath);
								// check if the file exists
								lastModified = f.lastModified();
								if(!f.exists() || lastModified < info.fLastModified) {
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
									lastModified = f.lastModified();
								}
								url = new URL("file:"+f.getAbsolutePath());
							}
							// create the entry in the cache
							EntryInfo ei = new EntryInfo(bundle.getBundleId(), url, lastModified, name);
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
	}

	private class DysowebURLPolicy implements IURLPolicy {

		private IURLPolicy fParent;
		private Map fURLCache;
		private BundleInfo fBundleInfo;
		
		DysowebURLPolicy(BundleInfo bundleInfo, IURLPolicy parent) {
			fParent = parent;
			fBundleInfo = bundleInfo;
			fURLCache = new ConcurrentHashMap();
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
						// check if we are in dev mode
						if(fBundleInfo.isDev()) {
							File f = new File(fBundleInfo.getDevDir(), name);
							if(f.exists()) {
								return f.toURL();
							}
						}
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
								
								// do a local caching when possible
								// locally cache the file
								File file;
								if(name.startsWith("/"))
									file = new File(getScratchDir(), "/resources/"+fBundleInfo.getBundle().getBundleId()+name);
								else 
									file = new File(getScratchDir(), "/resources/"+fBundleInfo.getBundle().getBundleId()+"/"+name);

								if(!file.exists() || file.lastModified() < fBundleInfo.getLastModified()) {
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
	
	public synchronized void deploy(Bundle bundle) throws WebAppException {
		
		if(bundle == null) {
			// do nothing
			return;
		}
		
		Long key = new Long(bundle.getBundleId());

		// check if the bundle does not try to be deployed when another bundle
		// with the same symbolic name is already deployed
		List lstToUndeploy = new ArrayList();
		String strVer = (String) bundle.getHeaders().get(BUNDLE_VERSION);
		if(strVer != null) {
			Version v = Version.parseVersion(strVer);
			for(int i=0; i<fActiveBundleInfos.size(); i++) {
				BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
				Bundle existingBundle = info.getBundle();
				if(existingBundle.getBundleId() != bundle.getBundleId() && existingBundle.getSymbolicName() != null && existingBundle.getSymbolicName().equals(bundle.getSymbolicName())) {
					String str = (String)existingBundle.getHeaders().get(BUNDLE_VERSION);
					// get the version
					if(str != null) {
						Version existingVersion = Version.parseVersion(str);
						if(existingVersion.compareTo(v) > 0) {
							// the existing version is better!
							// we ignore this one
							return;
						} else {
							// trying to deploy a better version
							lstToUndeploy.add(existingBundle);
						}
					}
				}					
			}
		}
		// is there a list of bundles to undeploy before to proceed with this one?
		for(int i=0; i<lstToUndeploy.size(); i++) {
			undeploy((Bundle)lstToUndeploy.get(i));
		}
		
		if(fLog.isInfoEnabled()) {
			fLog.info("Deploying Dysoweb Application " + bundle.getBundleId() + 
					(bundle.getSymbolicName() == null ? "" : " ("+bundle.getSymbolicName()+")"));
		}

		// create a bundle info for this bundle
		BundleInfo bundleInfo = new BundleInfo(bundle);
		fActiveBundleInfos.add(bundleInfo);
		
		if(fServletContext != null) {
			// create a ServletContextWrapper
			fServletContextWrappers.put(key, new ServletContextWrapper(this, fServletContext, fContextParams));
			
	
			// add the service elements Maps
			fServlets.put(key, new ConcurrentHashMap());
			fFilters.put(key, new ConcurrentHashMap());
			fListeners.put(key, new ConcurrentHashMap());
			
			// process WEB-INF files
			try {
				String path = "webapp/WEB-INF";
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
			URL url = bundle.getEntry("/webapp/WEB-INF/web.xml");
			if(url != null) {
				// creates a copy of the existing mapper and add new entries into it
				loadWebDescriptor(bundleInfo, url);
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
									
									processTaglibDefinition(bundle, entry, doc, path);
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
			// special treatment for not referenced taglib definition
			InputStream is = url.openStream();
			Document doc = null;
			try {
				doc = XMLUtils.parse(is);
			} catch(XMLException e) {
				throw new WebAppException(e);
			}
			is.close();
			
			processTaglibDefinition(bundle, url, doc, path);
		}	
	}

	private void processTaglibDefinition(Bundle bundle, URL url, Document doc, String path) throws WebAppException {
		try {
			if(url == null) {
				// nothing to process
			}
			long bundleId = bundle.getBundleId();
			String baseName = "taglibs/bundle"+bundleId+path;
			File fDst = new File(getScratchDir(), baseName);
			File fSrc = null;
			if("file".equals(url.getProtocol())) {
				fSrc = new File(url.toString().substring("file:".length()));
			}
			
			// scan the xml content and load the context descriptor
			String tagURI = XMLUtils.getChildText(doc.getDocumentElement(), "uri");

			fDst.getParentFile().mkdirs();
			// serialize the TLD 
	        String xml = XMLUtils.DocumentToString(doc);
	        // then write the content as utf-8
	        Writer w = new FileWriter(fDst);
	        w.write(xml);
	        w.close();
	        // set the last modified date as well
	        if(fSrc != null && fSrc.exists()) {
	        	fDst.setLastModified(fSrc.lastModified());
	        } else {
	        	fDst.setLastModified(bundle.getLastModified());
	        }
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
		// remove from the list of active bundles
		for(int i=0; i<fActiveBundleInfos.size(); ) {
			BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
			if(bundleId == info.fBundle.getBundleId()) {
				fActiveBundleInfos.remove(i);
				if(fLog.isInfoEnabled()) {
					fLog.info("Undeploying Dysoweb Application " + bundle.getBundleId() + 
							(bundle.getSymbolicName() == null ? "" : " ("+bundle.getSymbolicName()+")"));
				}
			} else {
				i++;
			}
		}
		
		Long bundleKey = new Long(bundleId);
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

	private Map fServletWrappers = new ConcurrentHashMap();
	
	public IServletDefinition createServletDefinition(Bundle bundle, String name, Element el) throws WebAppException {
		
		long bundleId = bundle.getBundleId();
		
		// do we already have this servlet associated to the service?
		Map servlets = (Map)fServlets.get(new Long(bundleId));
		ServletDefinition def = (ServletDefinition)servlets.get(name);
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
		def = new ServletDefinition(bundle.getBundleId(), name, cls, loadOnStartup);
		def.loadParams(el);
		
		def.loadClass(bundle);
		
		servlets.put(name, def);
		return def;
	}

	public IFilterDefinition createFilterDefinition(Bundle bundle, String name, Element el) throws WebAppException {

		long bundleId = bundle.getBundleId();
		
		Map filters = (Map)fFilters.get(new Long(bundleId));
		FilterDefinition def = (FilterDefinition)filters.get(name);
		if(def != null) {
			return def;
		}
		
		String cls = XMLUtils.getChildText(el, "filter-class");
		def = new FilterDefinition(bundle.getBundleId(), name, cls);
		def.loadParams(el);
		def.loadClass(bundle);
		
		filters.put(name, def);
		return def;
	}


	public IListenerDefinition createListenerDefinition(Bundle bundle, String className) throws WebAppException {

		long bundleId = bundle.getBundleId();
		Map listeners = (Map)fListeners.get(new Long(bundleId));
		ListenerDefinition def = (ListenerDefinition)listeners.get(className);
		if(def != null) {
			return def;
		}
		
		def = new ListenerDefinition(bundle.getBundleId(), className);
		def.loadClass(bundle);
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
					if(def.getLoader() != null)
						th.setContextClassLoader(def.getLoader());
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
				} catch(Throwable e) {
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
					if(def.getLoader() != null) {
						th.setContextClassLoader(def.getLoader());
					}
					def.load();
					filters.put(def.getName(), def);
				} catch(Throwable e) {
					fLog.error("Unable to initialize the filter " + def.getName(), e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
				}
			}
		}
		
		// Phase2: initialize the Jasper servlets
		// create the jasper servlets (one per active service)
		for(int i=0; i<fActiveBundleInfos.size(); i++) {
			BundleInfo info = (BundleInfo)fActiveBundleInfos.get(i);
			Long l = new Long(info.getBundle().getBundleId());
			ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(l);
			if(fJspWrappers.get(l) == null && contextWrapper != null) {
				try {
					JspServlet jasperServlet = new JspServlet();
					URL[] loaderURL = getLoaderURL(IWebProcessor.class.getClassLoader());
					// set a URL class loader to make Jasper happy with dynamic class path compilation
					ClassLoader jspLoader = new URLClassLoader(loaderURL, new JasperLoader(info.getBundle()));
					th.setContextClassLoader(jspLoader);
					jasperServlet.init(new JasperConfig(contextWrapper));
					ServletWrapper jasperWrapper = new ServletWrapper(l.longValue(), contextWrapper, jasperServlet, jspLoader);
					fJspWrappers.put(l, jasperWrapper);
				} catch(Throwable e) {
					fLog.error("Unable to initialize the JSP servlet ", e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
				}
			}
		}
		
		// Phase2: initialize the other servlets and create the servlet wrappers
		Map servletWrappers = new ConcurrentHashMap();
		for(int i=0; i<fActiveDefinitions.size(); i++) {
			Object item = fActiveDefinitions.get(i); 
			if(item instanceof IServletDefinition) {
				IServletDefinition def = (IServletDefinition)item;
				ServletContextWrapper contextWrapper = (ServletContextWrapper)fServletContextWrappers.get(new Long(def.getBundleId()));
				ServletWrapper w = new ServletWrapper(def.getBundleId(), contextWrapper, def);
				try {
					if(def.getLoader() != null) {
						th.setContextClassLoader(def.getLoader());
					}
					def.load();
					servletWrappers.put(def.getName(), w);
				} catch(Exception e) {
					fLog.error("Unable to initialize the servlet " + def.getName(), e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
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
					if(def.getLoader() != null) {
						th.setContextClassLoader(def.getLoader());
					}
					def.init(contextWrapper);
				} catch(Exception e) {
					fLog.error("Unable to initialize filter " + def.getName(), e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
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
						if(def.getLoader() != null) {
							th.setContextClassLoader(def.getLoader());
						}
						def.init(contextWrapper);
					}
				} catch(Throwable e) {
					fLog.error("Unable to initialize servlet " + def.getName(), e);
				} finally {
					th.setContextClassLoader(contextClassLoader);
				}
			}
		}	

		// activate the servlet wrappers and listeners
		fContextListeners = contextListeners;
		fContextAttributeListeners = contextAttributeListeners;
		fSessionListeners = sessionListeners;
		fSessionAttributeListeners = sessionAttributeListeners;
		
		fServletWrappers = servletWrappers;
		fFiltersByName = filters;
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
				URL coreJar = fServletContext.getResource("/WEB-INF/lib/dysoweb-core-1.0.4.jar");
				parentURL = new URL[] { coreJar };
			} catch (MalformedURLException e) {
				// use a default URL
				parentURL = new URL[0];
			}
		}
		// try with the ProxyLoader if this happens to be a TagClass
		File f = getScratchDir();
		URL[] proxyPath;
		try {
			proxyPath = new URL[] { f.toURL() };
		} catch (MalformedURLException e) {
			// cannot happen
			proxyPath = null;
		}
		URLClassLoader cl = new URLClassLoader(proxyPath, IWebProcessor.class.getClassLoader());
		if(cl != null) {
			URL[] proxies = cl.getURLs();
			URL[] combined = new URL[parentURL.length+proxies.length];
			System.arraycopy(parentURL, 0, combined, 0, parentURL.length);
			System.arraycopy(proxies, 0, combined, parentURL.length, proxies.length);
			return combined;
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
		return (IFilterDefinition)fFiltersByName.get(name);	
	}
	
	public ServletWrapper getServletWrapper(String name) {
		return (ServletWrapper)fServletWrappers.get(name);	
	}
	
	

}
