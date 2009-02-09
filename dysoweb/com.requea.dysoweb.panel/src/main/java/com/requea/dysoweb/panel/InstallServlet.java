package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.InstallServlet;
import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.monitor.AjaxProgressMonitor;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.Base64;
import com.requea.dysoweb.panel.utils.ISO8601DateTimeFormat;
import com.requea.dysoweb.panel.utils.Util;
import com.requea.dysoweb.service.obr.ClientAuthRepositoryAdmin;
import com.requea.dysoweb.service.obr.MonitoredResolver;
import com.requea.dysoweb.service.obr.IProgressMonitor;
import com.requea.dysoweb.service.obr.SubProgressMonitor;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;

public class InstallServlet extends HttpServlet {

	public static final String CATEGORIES = "com.requea.dysoweb.categories";
	public static final String INSTALLABLES = "com.requea.dysoweb.installables";

	public static final String CATEGORY = "com.requea.dysoweb.category";
	public static final String INSTALLABLE = "com.requea.dysoweb.installable";
	public static final String INSTALLED = "com.requea.dysoweb.installed";

	private static final String INSTALL_MONITOR = "com.requea.dysoweb.panel.install.monitor";
	private static final String INSTALL_STATUS = "com.requea.dysoweb.panel.install.status";
	
	public static final String DEFAULT_REPO = "https://repo.requea.com/dysoweb/repo/contents/repository.xml";
	

	private static final long serialVersionUID = -680556291402571674L;
	
    protected static final DateFormat format = new ISO8601DateTimeFormat();
	private static final int DEFAULT_REPO_REGISTRATION_TIMEOUT = 30000;
	
	private File fConfigDir;
	private SSLSocketFactory fSocketFactory;
	private Proxy fProxy;
	private String fProxyAuth;
	

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityFilter.getConfigDir(config.getServletContext());
		fConfigDir.mkdirs();
	}
	
	public static RepositoryAdmin initRepo(File configDir, Element elConfig) throws Exception {
		
		// get the repo
		RepositoryAdmin repo = Activator.getDefault().getRepo();
		if(repo == null) {
			// not initialized
			throw new Exception("No repository service available at this time");
		}

		// init proxy settings
		String proxy = XMLUtils.getChildText(elConfig, "Proxy");
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		String proxyAuth = XMLUtils.getChildText(elConfig, "ProxyAuth");
		
		// ssl client certificate and proxy settings
		if(repo instanceof ClientAuthRepositoryAdmin) {
			ClientAuthRepositoryAdmin clr = (ClientAuthRepositoryAdmin)repo;
			if("manual".equals(proxy)) {
				clr.setProxy(proxyHost, Integer.parseInt(proxyPort), proxyAuth);
			} else {
				try {
				    System.setProperty("java.net.useSystemProxies","true");
				} catch (SecurityException e) {
				    ; // failing to set this property isn't fatal
				}
			}
			clr.setSSLSocketFactory(initSocketFactory(configDir));
		} else {
			if("manual".equals(proxy)) {
				// force system wide proxy settings
				try {
				    System.setProperty("http.proxyHost",proxyHost);
				    System.setProperty("http.proxyPort",proxyPort);
				    System.setProperty("http.proxyAuth",proxyAuth);
				} catch (SecurityException e) {
				    ; // failing to set this property isn't fatal
				}
			} else {
				try {
				    System.setProperty("java.net.useSystemProxies","true");
				} catch (SecurityException e) {
				    ; // failing to set this property isn't fatal
				}
			}
		}
		
		// update the repo URL
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		String authKey = XMLUtils.getChildText(elConfig, "AuthKey");
		if(authKey == null || authKey.length() == 0) {
			authKey = "none";
		}
		// add the key to the auth?
		if(repoURL.startsWith("http")) {
			if(repoURL.indexOf("?") < 0) {
				repoURL += "?AuthKey=" + URLEncoder.encode(authKey, "UTF-8");
			} else {
				repoURL += "&AuthKey=" + URLEncoder.encode(authKey, "UTF-8");
			}
		}
		repo.addRepository(new URL(repoURL));
		
		return repo;
	}

	public void destroy() {
		super.destroy();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		process(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		process(request, response);
	}

	private synchronized SSLSocketFactory getSSLSocketFactory() {

		if(fSocketFactory != null) {
			return fSocketFactory;
		}
		fSocketFactory = initSocketFactory(fConfigDir);
		return fSocketFactory;
	}

	private void process(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String op = request.getParameter("op");
		Element elConfig = getServerConfig(fConfigDir);
		// first of all, check if the server is a registered server
		if (elConfig == null && !"register".equals(op)) {
			// include registration page
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure.jsp");
			rd.forward(request, response);
			return;
		}
		if("status".equals(op)) {
			handleStatusRequest(request, response);
			return;
		} else if ("register".equals(op)) {

			String ru = request.getParameter("ru");
			request.setAttribute("com.requea.dysoweb.panel.ru", ru == null ? ""
					: ru);

			if (!checkParameters(request, response)) {
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}

			// register the server and goes back to callers page
			try {
				String contactName = request.getParameter("ContactName");
				String contactEMail = request.getParameter("ContactEMail");
				String strPassword = request.getParameter("Password");
				updateServerRegistration(fConfigDir, contactName, contactEMail, strPassword);
				// reset the socket factory
				fSocketFactory = null;
				HttpSession session = request.getSession(true);
				session.removeAttribute(InstallServlet.INSTALLABLES);
				// from this point on, we are authenticated
				session.setAttribute(SecurityFilter.AUTH, Boolean.TRUE);
				session.setAttribute(SecurityFilter.SECURED, Boolean.TRUE);
				session.setAttribute("com.requea.dysoweb.shell.auth", Boolean.TRUE);
			} catch (Exception e) {
				request.setAttribute(ErrorTag.ERROR,
						"Unable to register server: " + e.getMessage());
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}

			// registration was ok, redirect
			if (ru != null && ru.length() > 0 && !"null".equals(ru)) {
				response.sendRedirect(response.encodeURL(ru));
				return;
			} else {
				response.sendRedirect(response.encodeURL(request.getContextPath()
						+ "/dysoweb/panel/panel.jsp"));
			}
			
		} else if("install".equals(op)) {
			// check if server certificate is there
			File file = new File(fConfigDir, "dysoweb.p12");
			if(!file.exists() && !"register".equals(op)) {
				try {
					updateServerRegistration(fConfigDir, null, null, null);
					// reset the socket factory
					fSocketFactory = null;
					
				} catch(Exception e) {
					// ignore
				}
			}
			
			handleInstallBundleRequest(request, response, elConfig);
		} else if("image".equals(op)) {
			// render the image
			String image = request.getParameter("image");
			if(image != null) {
				File f = new File(fConfigDir, "products/image/"+image+".jpg");
				if(!f.exists()) {
					response.sendError(404);
				} else {
					// output the image
					int length = (int)f.length();
					response.setContentLength(length);
					response.setContentType("img/jpeg");
					OutputStream os = response.getOutputStream();
					InputStream is = new FileInputStream(f);
		            byte[] buffer = new byte[4096];
		            int count = 0;
		            for (int len = is.read(buffer); len > 0; len = is.read(buffer))
		            {
		                count += len;
		                os.write(buffer, 0, len);
		            }
		            os.close();
		            is.close();
					return;
				}
			} else {
				response.sendError(404);
			}
		} else {
			try {
				handleInstallPanelRequest(request, response, elConfig);
			} catch (Exception e) {
				// show the error
				request.setAttribute(ErrorTag.ERROR, "Unable to retrieve installable application list: " + e.getMessage());
				RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/install.jsp");
				rd.forward(request, response);
			}
		}
	}

	private boolean checkParameters(HttpServletRequest request,
			HttpServletResponse response) {
		// check the parameters
		String pass1 = request.getParameter("Password");
		if (pass1 == null) {
			request.setAttribute(ErrorTag.ERROR, "Password is required");
			return false;
		}
		String pass2 = request.getParameter("Password2");
		if (!pass1.equals(pass2)) {
			request.setAttribute(ErrorTag.ERROR, "Passwords do not match");
			return false;
		}
		return true;
	}
	
	private void handleStatusRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// retrieve the monitor
		HttpSession session = request.getSession();
		Object objMonitor = session.getAttribute(INSTALL_MONITOR);
		Object objStatus = session.getAttribute(INSTALL_STATUS);
		
		if(!(objMonitor instanceof AjaxProgressMonitor) || !(objStatus instanceof Status)) {
			return;
		}
		// retrieve the monitor result
		Element el = XMLUtils.newElement("div");

		// set the status
		Status status = (Status)objStatus;
		AjaxProgressMonitor monitor = (AjaxProgressMonitor)objMonitor;
		if(status.getStatus() == Status.ERROR) {
			el.setAttribute("status", "error");
			// set the error message
			Element elMsg = XMLUtils.addElement(el, "div");
			elMsg.setAttribute("class", "rqerror");
			if(status.getException() != null) {
				String msg = status.getException().getMessage();
				XMLUtils.setText(elMsg, msg);
			} else {
				XMLUtils.setText(elMsg, "Error");
			}
			
		} else if(status.getStatus() == Status.DONE) {
			el.setAttribute("status", "done");
			Element elMsg = XMLUtils.addElement(el, "div");
			elMsg.setAttribute("class", "rqtask");
			XMLUtils.setText(elMsg, "Application installed successfully");
		} else if(status.getStatus() == Status.NEW) {
			// still waiting to start
			Element elMsg = XMLUtils.addElement(el, "div");
			XMLUtils.setText(elMsg, "Installation in progress. Please wait");
			Element elWait = XMLUtils.addElement(el, "div");
			elWait.setAttribute("class", "rqwait");
			XMLUtils.addElement(elWait, "span", "Please wait");
		} else {
			monitor.renderProgress(el);
		}

		// render the response as an ajax packet
		String xml = "";
		try {
			xml = XMLUtils.DocumentToString(el.getOwnerDocument());
		} catch(Exception e) {
			// ignore this one
		}
        response.setContentType("text/xml");
        String encoding = null;
        if(!"false".equals(System.getProperty("com.requea.dynpage.compressoutput"))) {
        	encoding = request.getHeader("Accept-Encoding");
        }
        boolean supportsGzip = false;
        if (encoding != null) {
	    	if (encoding.toLowerCase().indexOf("gzip") > -1) {
	    		supportsGzip = true;
    		}
    	}
        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
        OutputStream os = response.getOutputStream();
        if(supportsGzip) {
        	os = new GZIPOutputStream(os);
        	response.setHeader("Content-Encoding", "gzip");
        }
        Writer w = new OutputStreamWriter(os, "UTF-8");
        w.write(xml);
        w.close();
	}

	private void handleInstallBundleRequest(HttpServletRequest request,
			HttpServletResponse response, Element elConfig) throws IOException, ServletException {

		
		BundleContext context = Activator.getDefault().getContext();

		try {
			Installable[] all = getInstallables(request, response, elConfig);
			
			ArrayList lst = new ArrayList();

			Installable installedResource = null;
			
			// get the list of resources that need to be installed
			Map map = request.getParameterMap();
			Iterator iter = map.keySet().iterator();
			while(iter.hasNext()) {
				String strParam = (String)iter.next();
				if(strParam.startsWith("inst_")) {
					Object value = map.get(strParam);
					if(value instanceof String[]) {
						String[] vals = (String[])value;
						if(vals.length > 0)
							value = vals[0];
					}
					if("install".equals(value)) {
						String strInstallable = strParam.substring("inst_".length());
						if(strInstallable.length() > 0) {
							// find the requested resource
							Installable res = findResource(all, strInstallable);
							if(res == null) {
								throw new Exception("Unable to find resource '"+strInstallable+"'. Cannot deploy");
							}
							installedResource = res;

							// find dependent features
							getDependentFeatures(lst, all, res);

							if(!lst.contains(res)) {
								// adds at the end
								lst.add(res);
							}
						}
					}
				}
			}
			
			// turn it as an array
			Installable[] installables = (Installable[])lst.toArray(new Installable[lst.size()]);
			
			// once we have the repo, we ask for deployment
			
			// create the progress monitor
			AjaxProgressMonitor monitor = new AjaxProgressMonitor();
			Status status = new Status();
			request.getSession().setAttribute(INSTALL_MONITOR, monitor);
			request.getSession().setAttribute(INSTALL_STATUS, status);

			RepositoryAdmin repo = initRepo(fConfigDir, elConfig);
			// launch the trhead to install the bundles
			Thread th = new Thread(new Installer(context, repo, installables, monitor, status));
			th.start();
			
			request.setAttribute(INSTALLABLE, installedResource);
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/installing.jsp");
			rd.forward(request, response);
		} catch (Exception e) {
			// show the error
			request.setAttribute(ErrorTag.ERROR, "Unable to install resources: " + e.getMessage());
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/install.jsp");
			rd.forward(request, response);
		}
	}

	private void getDependentFeatures(List lst, Installable[] allFeatures,
			Installable feature) {

		List depends = feature.getDependsOn();
		for(int i=0; depends != null && i<depends.size(); i++) {
			String sysId = (String)depends.get(i);
			
			// find the feature
			Installable f = null;
			for(int j=0; f == null && j<allFeatures.length; j++) {
				if(sysId.equals(allFeatures[j].getSysId())) {
					f = allFeatures[j];
				}
			}
			if(f != null && !lst.contains(f)) {
				// adds at the top
				lst.add(0, f);
				// get dependents features for this one
				getDependentFeatures(lst, allFeatures, f);
			}
		}
	}
	
	private class Status implements Serializable {

		private static final long serialVersionUID = 7322042123009546894L;
		
		private Exception fException;
		private int fStatus = NEW;
		
		public void setError(Exception e) {
			fException = e;
			fStatus = ERROR;
		}

		public Exception getException() {
			return fException;
		}
		
		public static final int NEW = 0;
		public static final int STARTED = 4;
		public static final int DONE = 1;
		public static final int ERROR = 2;
		public static final int CANCEL = 3;
		
		public int getStatus() {
			return fStatus;
		}

		public void setStatus(int status) {
			fStatus = status;
		}
		
	}

	private class Installer implements Runnable {
		
		
		private RepositoryAdmin fRepo;
		private BundleContext fContext;
		private Installable[] fResources;
		private IProgressMonitor fProgressMonitor;
		private Status fStatus;
		
		Installer(BundleContext context, RepositoryAdmin repo, Installable[] resources, IProgressMonitor monitor, Status status) {
			fContext = context;
			fResources = resources;
			fRepo = repo;
			fProgressMonitor = monitor;
			fStatus = status;
		}
		
		public void run() {
			
			try {
				fStatus.setStatus(Status.STARTED);
				try {
					installResources(fContext, fRepo, fResources);
				} catch(Exception e) {
					fStatus.setError(e);
	        		// abort
	        		return;
				}
				fStatus.setStatus(Status.DONE);
			} catch(Exception e) {
				fStatus.setError(e);
			} finally {
				fProgressMonitor.done();
			}
		}
		
		
		private void installResources(BundleContext context, RepositoryAdmin repo, Installable[] installables) throws Exception {

			// create an XML document for the resource
	        Resolver resolver = (Resolver)repo.resolver();
	        for(int i=0; i<installables.length; i++) {
	        	
	        	Installable installable = installables[i];
	        	
				// retrieve the bundles id
				if("feature".equals(installable.getType())) {
					String bundles = installable.getBundleList();
					
					// retrieve the bundles id
					StringTokenizer st = new StringTokenizer(bundles,",");
			        while(st.hasMoreTokens()) {
			        	String bundleId = st.nextToken();
			            // Find the target's bundle resource.
			            Resource resource = selectNewestVersion(
			                searchRepository(context, repo, bundleId, null));
			            if (resource != null)
			            {
			                resolver.add(resource);
			            }
			            else
			            {
			                throw new Exception("Unknown bundle - " + bundleId);
			            }
			        }
				} else {		
			        // Find the target's bundle resource.
			        Resource[] res = searchRepository(context, repo, installable.getName(), installable.getVersion());
			        if (res != null && res.length > 0)
			        {
			        	for(int j=0; j<res.length; j++)
			        		resolver.add(res[j]);
			        }
			        else
			        {
			            throw new Exception("Unknown resource - " + installable.getName()+" ("+installable.getVersion()+")");
			        }
				}
	        }
	        
	        if ((resolver.getAddedResources() != null) &&
	            (resolver.getAddedResources().length > 0))
	        {
	            if (resolver.resolve())
	            {
	    			long lSize = 0;
	    			Map deployMap = new HashMap();
	            	Resource[] resources = resolver.getAddedResources();
	        		if(resources != null && resources.length > 0) {
	        			for(int i=0; i<resources.length; i++) {
	        				deployMap.put(resources[i], resources[i]);
	        			}
	        		}
	            	resources = resolver.getRequiredResources();
	        		if(resources != null && resources.length > 0) {
	        			for(int i=0; i<resources.length; i++) {
	        				deployMap.put(resources[i], resources[i]);
	        			}
	        		}
	        		/* do not deploy optional resources
	        		resources = resolver.getOptionalResources();
	        		if(resources != null && resources.length > 0) {
	        			for(int i=0; i<resources.length; i++) {
	        				deployMap.put(resources[i], resources[i]);
	        			}
	        		}
	        		*/
	        		Iterator iter = deployMap.values().iterator();
	        		while(iter.hasNext()) {
	        			Resource resource = (Resource)iter.next();
	    				Long size = (Long)resource.getProperties().get("size");
	    				if(size != null) {
	    					lSize += size.longValue();
	    				}
	        		}
	        		try {
						fProgressMonitor.beginTask("Installing selected resources ", lSize == 0 ? IProgressMonitor.UNKNOWN : (int)lSize);
		                try
		                {
		            		if(resolver instanceof MonitoredResolver) {
			            		SubProgressMonitor subMonitor = new SubProgressMonitor(fProgressMonitor, (int)lSize);
		            			((MonitoredResolver)resolver).deploy(true, subMonitor);
		            		} else {
		            			resolver.deploy(true);
		            		}
		                }
		                catch (IllegalStateException ex)
		                {
		                    throw ex;
		                }
	        		} finally {
	        			fProgressMonitor.done();
	        		}
	            }
	            else
	            {
	                Requirement[] reqs = resolver.getUnsatisfiedRequirements();
	                if ((reqs != null) && (reqs.length > 0))
	                {
	                	StringBuffer sb = new StringBuffer();
	            		sb.append("Unsatisfied requirement(s):\n");
	                    for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
	                    {
		            		sb.append("   " + reqs[reqIdx].getFilter()+"\n");
	                        Resource[] resources = resolver.getResources(reqs[reqIdx]);
	                        for (int resIdx = 0; resIdx < resources.length; resIdx++)
	                        {
	                        	sb.append("      " + resources[resIdx].getPresentationName()+"\n");
	                        }
	                    }
	                    throw new Exception(sb.toString());
	                }
	                else
	                {
	                	// cannot retrieve a better error message
	                	throw new Exception("Missing requirements");
	                }
	            }
	        }
	        // register installed applications
	        for(int i=0; i<installables.length; i++) {
	        	
	        	Installable installable = installables[i];
	        	
				Element elResource = XMLUtils.newElement("resource");
				elResource.setAttribute("id", installable.getID());
				String version = installable.getVersion();
				if(version != null) {
					elResource.setAttribute("version", version);
				}
				// store the resource info
				XMLUtils.addElement(elResource, "title", installable.getName());
				Element elDesc = XMLUtils.addElement(elResource, "description");
				XMLUtils.setCDATA(elDesc, installable.getDescription());
				
				elDesc = XMLUtils.addElement(elResource, "documentation");
				XMLUtils.setCDATA(elDesc, installable.getLongDesc());
				synchronized (format) {
					elResource.setAttribute("date", format.format(new Date()));
				}
	            // register the new application if something was installed
	        	registerInstalledApplication(installable, elResource);
	        }
		}
				
	}
	

    public Resource selectNewestVersion(Resource[] resources)
    {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            if (i == 0)
            {
                idx = 0;
                v = resources[i].getVersion();
            }
            else
            {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0)
                {
                    idx = i;
                    v = vtmp;
                }
            }
        }

        return (idx < 0) ? null : resources[idx];
    }
	
	
	private void registerInstalledApplication(Installable installable, Element elResource) throws Exception {
		
    	Element elConfig = getServerConfig(fConfigDir);
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL != null) {
			URL url = new URL(repoURL);
			if(!url.getProtocol().startsWith("http") || !"repo.requea.com".equals(url.getHost())) {
				// not registrable
				return;
			}
		}
		String fileName = installable.getID();
		String version = installable.getVersion();
		if(version != null) {
			fileName += "-"+version;
		}
		fileName += ".xml";
		
		File file = new File(fConfigDir, "install/"+fileName);
		file.getParentFile().mkdirs();
		
		// get the image
		Element elImage = XMLUtils.getChild(elResource, "rqImage");
		if(elImage != null) {
			String sysId = elImage.getAttribute("sysId");
			String strURL = elImage.getAttribute("url");
			// copy the image on the local directory
			try {
				File fileImage = new File(fConfigDir, "install/"+sysId+".jpg");
				URL url = new URL(strURL);
	            OutputStream os = new FileOutputStream(fileImage);
	            URLConnection conn = fProxy == null ? url.openConnection() : url.openConnection(fProxy);
                // Support for http proxy authentication
                if ((fProxyAuth != null) && (fProxyAuth.length() > 0))
                {
                    if ("http".equals(url.getProtocol()) ||
                        "https".equals(url.getProtocol()))
                    {
                        String base64 = Util.base64Encode(fProxyAuth);
                        conn.setRequestProperty(
                            "Proxy-Authorization", "Basic " + base64);
                    }
                }
	            InputStream is = conn.getInputStream();
	            byte[] buffer = new byte[4096];
	            int count = 0;
	            for (int len = is.read(buffer); len > 0; len = is.read(buffer))
	            {
	                count += len;
	                os.write(buffer, 0, len);
	            }
	            os.close();
	            is.close();
	            
	            // update the image
	            elResource.removeChild(elImage);
	            XMLUtils.addElement(elResource, "image", sysId+".jpg");
			} catch(Exception e) {
				
			}
		}
		
		// store the content as XML
        String xml = XMLUtils.ElementToString(elResource, true);
        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        w.write(xml);
        w.close();
        
        // post install info the the repo server
        try {
    		// retrieve the list of applications that can be installed
        	URLConnection connection = openConnection(elConfig, "contents/registerinstall");
			connection.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(
	                connection.getOutputStream());
			
			// send name and some platform info for support and OBR repository
			osw.write("feature=" + installable.getSysId());
			osw.write("&LocalIP=" + URLEncoder.encode(InetAddress.getLocalHost().getHostAddress(), "UTF-8"));
			osw.write("&java.version=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8"));
			osw.write("&java.vendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "UTF-8"));
			osw.write("&os.name=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
			osw.write("&os.arch=" + URLEncoder.encode(System.getProperty("os.arch"), "UTF-8"));
			osw.write("&os.version=" + URLEncoder.encode(System.getProperty("os.version"), "UTF-8"));
			
			// bundles
			StringBuffer sb = new StringBuffer();
			Element elBundles = XMLUtils.getChild(elResource, "bundles");
			if(elBundles != null) {
				Element elBundle = XMLUtils.getChild(elBundles, "bundle");
				boolean first = true;
				while(elBundle != null) {
					if(first) { first = false; } else { sb.append(","); }
					String symName = elBundle.getAttribute("symbolicName");
					if(symName != null) {
						sb.append(symName + ":" + elBundle.getAttribute("version"));
					} else {
						sb.append(elBundle.getAttribute("name"));
					}
					elBundle = XMLUtils.getNextSibling(elBundle);
				}
				osw.write("&bundles=" + URLEncoder.encode(sb.toString(), "UTF-8"));
			}
			
			osw.close();
			// execute method and handle any error responses.
			connection.getInputStream();
        } catch(Exception e) {
        	// ignore: registration was not successful
        }
	}
	
    private URLConnection openConnection(Element elConfig, String path) throws Exception {
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		URL baseURL = new URL(repoURL);
		URL url;
		if(path != null && repoURL.endsWith("zip")) {
			// open the zip
			url = baseURL;
		} else {
			url =new URL(baseURL,path);
		}
		
		String proxy = XMLUtils.getChildText(elConfig, "Proxy");
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		String proxyAuth = XMLUtils.getChildText(elConfig, "ProxyAuth");
		
		URLConnection cnx;
		if("manual".equals(proxy)) {
			Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
			cnx = url.openConnection(p);
			// proxy auth?
			if(proxyAuth != null && proxyAuth.length() > 0) {
                String base64 = Base64.encode(proxyAuth.getBytes());
                cnx.setRequestProperty(
                    "Proxy-Authorization", "Basic " + base64);
			}
		} else {
			// open the connection with default settings
			// set proxy autodetect
			try {
			    System.setProperty("java.net.useSystemProxies","true");
			} catch (SecurityException e) {
			    ; // failing to set this property isn't fatal
			}
			cnx = url.openConnection();	
		}
        if(cnx instanceof HttpsURLConnection) {
        	SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        	if(sslSocketFactory != null) {
        		HttpsURLConnection secureCon = (HttpsURLConnection) cnx;
                secureCon.setSSLSocketFactory(sslSocketFactory);
        	}
        }
        
		return cnx;
	}
	private InputStream openStream(String repoURL, URLConnection cnx, String path) throws IOException {
		if(repoURL.endsWith("zip")) {
			if(repoURL.startsWith("file:")) {
				// use a zip file, since this is way faster
				File f = new File(repoURL.substring("file:".length()));
				ZipFile zf = new ZipFile(f);
				ZipEntry ze = zf.getEntry(path);
				if(ze != null) {
					return zf.getInputStream(ze);
				} else {
					return null;
				}
			} else {
	            ZipInputStream zin = new ZipInputStream(cnx.getInputStream());
	            ZipEntry entry = zin.getNextEntry();
	            while (entry != null)
	            {
	                if (entry.getName().equalsIgnoreCase(path))
	                {
	                    return zin;
	                }
	                entry = zin.getNextEntry();
	            }
	            // nothing found
	            return null;
			}
		} else {
			return cnx.getInputStream();
		}
	}
    

	private Resource[] searchRepository(BundleContext context, RepositoryAdmin repo, String targetId, String targetVersion)
    {
        // Try to see if the targetId is a bundle ID.
        try
        {
            Bundle bundle = context.getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        }
        catch (NumberFormatException ex)
        {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null)
        {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return repo.discoverResources(sb.toString());
    }
    
	private void handleInstallPanelRequest(HttpServletRequest request,
			HttpServletResponse response, Element elConfig) throws Exception {

		Status status = (Status)request.getSession().getAttribute(INSTALL_STATUS);
		if(status != null && status.getStatus() != Status.ERROR && status.getStatus() != Status.DONE) {
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/installing.jsp");
			rd.forward(request, response);
			return;
		}
		
		Installable[] installables = getInstallables(request, response, elConfig);
		Installable resource = findResource(installables, request.getParameter("resource"));
		// lookup for the resource
		if(resource != null) {
			request.setAttribute(INSTALLABLE, resource);
			// show resource detail
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/installfeat.jsp");
			rd.forward(request, response);
		} else {
			// show list of resources
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/install.jsp");
			rd.forward(request, response);
		}		
		return;
	}
	
	
	private Installable findResource(Installable[] installables, String str) {
		Installable resource = null;
		if(installables != null && str != null) {
			// retrieve the resource
			for(int i=0; resource == null && i<installables.length; i++) {
				if(str.equals(installables[i].getID())) {
					resource = installables[i];
				}
			}
		}
		return resource;
	}

	private Installable[] getInstallables(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		// do we already have the list of installables and categories?
		HttpSession session = req.getSession();
		Installable[] installables;
		Object o = session.getAttribute(INSTALLABLES);
		if(!"true".equals(req.getParameter("refresh")) && o instanceof Installable[]) {
			// ok, we have the list of resources
			installables = (Installable[])o;
			req.setAttribute(INSTALLABLES, installables);
			req.setAttribute(CATEGORIES, session.getAttribute(CATEGORIES));
		} else {
			loadInstallables(req, resp, elConfig);
			installables = (Installable[])req.getAttribute(INSTALLABLES);
		}
		return installables;
	}
	
	private void loadInstallables(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		HttpSession session = req.getSession();
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		String authKey = XMLUtils.getChildText(elConfig, "AuthKey");
		URLConnection cnx = openConnection(elConfig, "repository.xml");
		if(authKey == null || authKey.length() == 0) {
			authKey = "none";
		}
		if(authKey != null && authKey.length() > 0 && cnx instanceof HttpURLConnection) {
	        cnx.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(
					cnx.getOutputStream());
			out.write("AuthKey=" + URLEncoder.encode(authKey, "UTF-8"));
			out.close();
		}
		
		InputStream is = openStream(repoURL, cnx, "repository.xml");
		if(is == null) {
			throw new Exception("Unable to find entry 'repository.xml'");
		}
        try {
			Document doc = XMLUtils.parse(is);
			Element elRepository = doc.getDocumentElement();
			if("error".equals(elRepository.getLocalName())) {
				throw new Exception(XMLUtils.getChildText(elRepository, "message"));
			}
			Installable[] installables;			
			String name = elRepository.getAttribute("name");
			if("Requea Repository".equals(name)) {
				// repo v1
				List lstInstall = new ArrayList();
				installables = parseInstallablesFromFeatures(elConfig);
				for(int i=0; i<installables.length; i++) {
					lstInstall.add(installables[i]);
				}
				// regular bundles
				installables = Installable.parse(elRepository);
				for(int i=0; i<installables.length; i++) {
					lstInstall.add(installables[i]);
				}
				installables = (Installable[])lstInstall.toArray(new Installable[lstInstall.size()]);
			} else {
				// parse the list of installable resources
				installables = Installable.parse(elRepository);
			}
			// grab all the images
			for(int j=0; j<installables.length; j++) {
				Installable f = installables[j];
				String strImage = f.getImage();
				if(strImage != null && !strImage.startsWith("http")) {
					// grab the image
					URLConnection cnxImg = openConnection(elConfig, strImage);
					if(cnxImg != null) {
						// copy the image on the local file
						InputStream isImg = openStream(repoURL, cnxImg, strImage);
						File file = new File(fConfigDir, "products/image/"+f.getName()+"-"+f.getVersion()+".jpg");
						file.getParentFile().mkdirs();

						OutputStream os = new FileOutputStream(file);
			            byte[] buffer = new byte[4096];
			            int count = 0;
			            for (int len = isImg.read(buffer); len > 0; len = isImg.read(buffer))
			            {
			                count += len;
			                os.write(buffer, 0, len);
			            }
			            os.close();
			            isImg.close();
			            
					}
				}
			}
			// get the category map
			Map categories = Installable.buildCategories(installables);
			
			// ok, we have the list of installables
			session.setAttribute(INSTALLABLES, installables);
			req.setAttribute(INSTALLABLES, installables);
			session.setAttribute(CATEGORIES, categories);
			req.setAttribute(CATEGORIES, categories);
		} catch(XMLException e) {
			req.setAttribute(ErrorTag.ERROR, "Unable to find repository information. Please try later, or check the repository URL");
		}
	}


	private Installable[] parseInstallablesFromFeatures(Element elConfig) throws Exception {
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		URLConnection cnx = openConnection(elConfig, "feature.xml");
		InputStream is = openStream(repoURL, cnx, "feature.xml");
		if(is == null) {
			throw new Exception("Unable to find entry 'repository.xml'");
		}
		Document doc = XMLUtils.parse(is);
		Element elFeatures = doc.getDocumentElement();
		if("error".equals(elFeatures.getLocalName())) {
			throw new Exception(XMLUtils.getChildText(elFeatures, "message"));
		}
		return Installable.parseAsFeature(elFeatures);
	}

	private static Element getServerConfig(File configDir) {

		File f = new File(configDir, "server.xml");
		if (!f.exists()) {
			return null;
		}

		// parse the file
		try {
			Document doc = XMLUtils.parse(new FileInputStream(f));
			return doc.getDocumentElement();
		} catch (FileNotFoundException e) {
			// cannot happen
			return null;
		} catch (XMLException e) {
			// should not happen: XML is corrupted. Better to ignore it
			return null;
		}
	}

	
	public static void updateServerRegistration(File configDir, String contactName, String contactEMail, String strPassword) throws Exception {

		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		
		Element elConfig = getServerConfig(configDir);
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		URL url = new URL(repoURL);
		// check if there is a certificate?
		File fileCert = new File(configDir, "dysoweb.p12"); 
		if(fileCert.exists()) {
			url = new URL(url, "update");
		} else {
			url = new URL(url, "../init");
		}
		
		Element elServer = null;
		String sysId = null;

		try {
			String proxy = XMLUtils.getChildText(elConfig, "Proxy");
			String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
			String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
			String proxyAuth = XMLUtils.getChildText(elConfig, "ProxyAuth");
			
			URLConnection cnx;
			if("manual".equals(proxy)) {
				Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
				cnx = url.openConnection(p);
				// proxy auth?
				if(proxyAuth != null && proxyAuth.length() > 0) {
                    String base64 = Base64.encode(proxyAuth.getBytes());
                    cnx.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
				}
			} else {
				// open the connection with default settings
				// set proxy autodetect
				try {
				    System.setProperty("java.net.useSystemProxies","true");
				} catch (SecurityException e) {
				    ; // failing to set this property isn't fatal
				}
				cnx = url.openConnection();	
			}
			
	        if(fileCert.exists() && cnx instanceof HttpsURLConnection) {
	        	SSLSocketFactory sslSocketFactory = initSocketFactory(configDir);
	        	if(sslSocketFactory != null) {
	        		HttpsURLConnection secureCon = (HttpsURLConnection) cnx;
	                secureCon.setSSLSocketFactory(sslSocketFactory);
	        	}
	        }
	        // set a default timeout
			cnx.setReadTimeout(DEFAULT_REPO_REGISTRATION_TIMEOUT);
	        cnx.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(
					cnx.getOutputStream());
			
			// send name and some patform info for support and OBR repository
			// selection
			String name = null;
	    	try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (Throwable e) {
				// ignore
				name = "unknown";
	        }
	
			out.write("Name=" + URLEncoder.encode(name, "UTF-8"));
			
			try {
				Document doc = XMLUtils.parse(new FileInputStream(new File(configDir, "server.xml")));
				elServer = doc.getDocumentElement();
			} catch(Exception e) {
				elServer = null;
			}
			
			// get the contact name and contact email if possible
			if(contactName != null && elServer != null) {
				contactName = XMLUtils.getChildText(elServer, "ContactName");
			}
			if(contactEMail != null && elServer != null) {
				contactEMail = XMLUtils.getChildText(elServer, "ContactEMail");
			}
			
			if(contactName != null && contactName.length() > 0) {
				out.write("&ContactName=" + URLEncoder.encode(contactName, "UTF-8"));
			}
			if(contactEMail != null && contactEMail.length() > 0) {
				out.write("&ContactEMail=" + URLEncoder.encode(contactEMail, "UTF-8"));
			}
			out.write("&LocalIP=" + URLEncoder.encode(InetAddress.getLocalHost().getHostAddress(), "UTF-8"));
			out.write("&java.version=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8"));
			out.write("&java.vendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "UTF-8"));
			out.write("&os.name=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
			out.write("&os.arch=" + URLEncoder.encode(System.getProperty("os.arch"), "UTF-8"));
			out.write("&os.version=" + URLEncoder.encode(System.getProperty("os.version"), "UTF-8"));
			out.close();
			
			// execute method and handle any error responses.
			InputStream in = cnx.getInputStream();
			// parse the XML document if any
			Document doc = XMLUtils.parse(in);
			Element el = doc.getDocumentElement();
			
			// retrieve the certificate if any
			String strCertificate = XMLUtils.getChildText(el, "certificate");
			if(strCertificate != null) {
				byte[] certificate = Base64.decode(strCertificate);
				FileOutputStream fos = new FileOutputStream(new File(configDir, "dysoweb.p12"));
				fos.write(certificate);
				fos.close();
			}
			// get the server id
			sysId = el.getAttribute("sysId");
		} catch(Exception e) {
			// unable to register the server on the remote repository
		}

		// update the server config
		elServer = XMLUtils.newElement("server");
		if (sysId != null)
			elServer.setAttribute("sysId", sysId);

		// store the password
		if(strPassword != null && strPassword.length() > 0) {
			XMLUtils.addElement(elServer, "Password", SecurityServlet.encrypt(strPassword));
		}
		if(contactName != null && contactName.length() > 0) {
			XMLUtils.addElement(elServer, "ContactName", contactName);
		}
		if(contactEMail != null && contactEMail.length() > 0) {
			XMLUtils.addElement(elServer, "ContactEMail", contactEMail);
		}

		// output the content as XML
		Source source = new DOMSource(elServer);
		StringWriter sw = new StringWriter();

		StreamResult result = new StreamResult(sw);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty("indent", "yes");
		xformer.transform(source, result);
		String xml = sw.toString();

		OutputStream os = new FileOutputStream(new File(configDir, "server.xml"));
		Writer w = new OutputStreamWriter(os, "UTF-8");
		w.write(xml);
		w.close();
		
	}

	public static SSLSocketFactory initSocketFactory(File configDir) {
		File fileCertificate = new File(configDir, "dysoweb.p12");
		if(!fileCertificate.exists()) {
			return null;
		}
		try {
			String strCertPassword = "dysoweb";
	        KeyStore keyStore = KeyStore.getInstance("PKCS12");
	        InputStream keyInput = new FileInputStream(fileCertificate);
	        keyStore.load(keyInput, strCertPassword.toCharArray());
	        keyInput.close();	        
	        
	        // does an HTTPS request to the repo passing the client certificates
	        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	        keyManagerFactory.init(keyStore, strCertPassword.toCharArray());
	        SSLContext context = SSLContext.getInstance("TLS");
	        context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
	        SSLSocketFactory socketFactory = context.getSocketFactory();
	        return socketFactory;
		} catch(Exception e) {
			return null;
		}
	}
}
