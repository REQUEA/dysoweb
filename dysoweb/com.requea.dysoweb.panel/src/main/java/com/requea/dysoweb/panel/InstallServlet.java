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
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.osgi.service.packageadmin.PackageAdmin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.InstallServlet;
import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.SecurityServlet.RegistrationException;
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

	private static final String COM_REQUEA_DYSOWEB_PANEL = "com.requea.dysoweb.panel.";
	
	private static final String INSTALL_MONITOR = "com.requea.dysoweb.panel.install.monitor";
	private static final String INSTALL_STATUS = "com.requea.dysoweb.panel.install.status";
	
	public static final String DEFAULT_REPO = "https://repo.requea.com/dysoweb/repo/contents/repository.xml";
	

	private static final long serialVersionUID = -680556291402571674L;
	
    protected static final DateFormat format = new ISO8601DateTimeFormat();
	private static final int DEFAULT_REPO_REGISTRATION_TIMEOUT = 60*1000;
	public static final String REGISTERED = "com.requea.dysoweb.registered";
	
	private File fConfigDir;

	private HttpClient m_httpClient;
	private HttpHost m_targetHost;
	

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = getConfigDir(config.getServletContext());
		fConfigDir.mkdirs();
	}
	
	public RepositoryAdmin initRepo(File configDir, Element elConfig) throws Exception {
		
		// get the repo
		RepositoryAdmin repo = Activator.getDefault().getRepo();
		if(repo == null) {
			// not initialized
			throw new Exception("No repository service available at this time");
		}

		
		// init proxy settings
		String settings = XMLUtils.getChildText(elConfig, "Settings");
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		String proxyUsername = XMLUtils.getChildText(elConfig, "ProxyUsername");
		String proxyPassword = XMLUtils.getChildText(elConfig, "ProxyPassword");
		
		// ssl client certificate and proxy settings
		if(repo instanceof ClientAuthRepositoryAdmin) {
			ClientAuthRepositoryAdmin clr = (ClientAuthRepositoryAdmin)repo;
			clr.setHttp(m_httpClient, m_targetHost);
		} else {
			if("manual".equals(settings) && proxyHost != null && proxyHost.length() > 0) {
				// force system wide proxy settings
				try {
				    System.setProperty("http.proxyHost",proxyHost);
				    System.setProperty("http.proxyPort",proxyPort);
				    if(proxyUsername != null)
				    System.setProperty("http.proxyAuth",proxyUsername+":"+proxyPassword);
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
		
		// local cache URL if any
		String localCacheURL = XMLUtils.getChildText(elConfig, "LocalCacheURL");
		if(localCacheURL != null && localCacheURL.length() > 0) {
			repo.addRepository(new URL(localCacheURL));
		}
		
		// update the repo URL
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
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

	private void process(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		String op = request.getParameter("op");
		Element elConfig = getServerConfig(fConfigDir);
		// first of all, check if the server is a registered server
		if (elConfig == null && !"register".equals(op)) {
			// include registration page
			String ru = request.getRequestURI();
			request.setAttribute("com.requea.dysoweb.panel.ru", ru);
			
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure.jsp");
			rd.forward(request, response);
			return;
		}
		
		File fileCertificate = new File(fConfigDir, "dysoweb.p12");
		boolean registered = fileCertificate.exists();
		request.setAttribute(InstallServlet.REGISTERED, new Boolean(registered));
		
		if("status".equals(op)) {
			handleStatusRequest(request, response);
			return;
		} else if ("register".equals(op)) {

			String ru = request.getParameter("ru");
			request.setAttribute("com.requea.dysoweb.panel.ru", ru == null ? "" : ru);
			
			if (!checkParameters(request, response)) {
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}

			// register the server and goes back to callers page
			try {
				
				String authKey = request.getParameter("AuthKey");
				String strPassword = request.getParameter("Password");
				saveConfig(fConfigDir, authKey, strPassword);
				// from this point on, we are authenticated
				HttpSession session = request.getSession();
				session.setAttribute(SecurityFilter.SECURED, Boolean.TRUE);
				session.setAttribute(SecurityFilter.AUTH, Boolean.TRUE);

				// save the settings and switch to the install page
				updateSettingsValues(request);
				
				saveSettings(request);
				

				if(!registered) {
			        String repoURL = getValueFromSession(request.getSession(), "RepoURL");
			        if(repoURL == null) {
			        	repoURL = DEFAULT_REPO;
			        }
			        if(repoURL.startsWith("https")) {
			        	// update server registration if not already done
			        	updateServerRegistration(authKey, strPassword);
			        }
				}
				
				// reset the http client
				m_httpClient = null;
				m_targetHost = null;
				session.removeAttribute(InstallServlet.INSTALLABLES);
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

			// registration was ok, redirect to the install page
			response.sendRedirect(response.encodeURL(request.getContextPath()
					+ "/dysoweb/panel/secure/install"));
			
		} else if("install".equals(op)) {
			// check if server certificate is there
			File file = new File(fConfigDir, "dysoweb.p12");
			if(!file.exists()) {
				try {
					// reset the socket factory
					response.sendRedirect(response.encodeURL(request.getContextPath()
							+ "/dysoweb/panel/bundle.jsp"));
					return;
				} catch(Exception e) {
					// ignore
				}
			}
			// init the http client
			try {
				initHttpClient(elConfig);
			} catch(Exception e) {
				request.setAttribute(ErrorTag.ERROR,
						"Unable to register server: " + e.getMessage());
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure/installsettings.jsp");
				rd.forward(request, response);
				return;
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
		} else if("save".equals(op)) {
			// save the settings and switch to the install page
			updateSettingsValues(request);
	        HttpSession session = request.getSession();
	        String newAuthKey = (String)session.getAttribute(COM_REQUEA_DYSOWEB_PANEL+"AuthKey");
	        String oldAuthKey = XMLUtils.getChildText(elConfig, "AuthKey");
	        
			saveSettings(request);
			
			if(!registered || (oldAuthKey != null && !oldAuthKey.equals(newAuthKey))) {
		        String repoURL = getValueFromSession(request.getSession(), "RepoURL");
		        if(repoURL.startsWith("https")) {
		        	// update server registration if not already done
		        	try {
						updateServerRegistration(newAuthKey, null);
					} catch (Exception e) {
						request.setAttribute(ErrorTag.ERROR,
								"Unable to register server: " + e.getMessage());
						// re forward to registration for correction of errors
						RequestDispatcher rd = request
								.getRequestDispatcher("/dysoweb/panel/secure/installsettings.jsp");
						rd.forward(request, response);
						return;
					}
		        }
			}
			
			// reset the http client
			m_httpClient = null;
			m_targetHost = null;

			session.removeAttribute(InstallServlet.INSTALLABLES);
			// from this point on, we are authenticated
			session.setAttribute(SecurityFilter.AUTH, Boolean.TRUE);
			session.setAttribute(SecurityFilter.SECURED, Boolean.TRUE);
			session.setAttribute("com.requea.dysoweb.shell.auth", Boolean.TRUE);
			// redirect to install
			response.sendRedirect(request.getContextPath()+"/dysoweb/panel/secure/install");
			return;
		} else if("true".equals(request.getParameter("settings"))) {
			// redirect to settings
			initSettingsValues(request);
			updateSettingsValues(request);
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/installsettings.jsp");
			rd.forward(request, response);
			return;
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
		String authkey = request.getParameter("AuthKey");
		if (authkey == null) {
			request.setAttribute(ErrorTag.ERROR, "Authorization Key is required");
			return false;
		}
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
			request.getSession().setAttribute(INSTALL_MONITOR, null);
			request.getSession().setAttribute(INSTALL_STATUS, null);
		} else if(status.getStatus() == Status.DONE) {
			el.setAttribute("status", "done");
			Element elMsg = XMLUtils.addElement(el, "div");
			elMsg.setAttribute("class", "rqtask");
			XMLUtils.setText(elMsg, "Application installed successfully");
			request.getSession().setAttribute(INSTALL_MONITOR, null);
			request.getSession().setAttribute(INSTALL_STATUS, null);
			
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
			
			RepositoryAdmin repo = initRepo(fConfigDir, elConfig);
			
			PackageAdmin pa = null;
	        ServiceReference ref = context.getServiceReference(
	                org.osgi.service.packageadmin.PackageAdmin.class.getName());
            if (ref != null) {
	            pa = (PackageAdmin) context.getService(ref);
            }			
			// once we have the repo, we ask for deployment
			// create the progress monitor
			AjaxProgressMonitor monitor = new AjaxProgressMonitor();
			Status status = new Status();
			request.getSession().setAttribute(INSTALL_MONITOR, monitor);
			request.getSession().setAttribute(INSTALL_STATUS, status);

			// launch the trhead to install the bundles
			Thread th = new Thread(new Installer(context, pa, repo, installables, monitor, status));
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
		private PackageAdmin fPackageAdmin;
		
		Installer(BundleContext context, PackageAdmin packageAdmin, RepositoryAdmin repo, Installable[] resources, IProgressMonitor monitor, Status status) {
			fContext = context;
			fResources = resources;
			fRepo = repo;
			fPackageAdmin = packageAdmin;
			fProgressMonitor = monitor;
			fStatus = status;
		}
		
		public void run() {
			
			try {
				fStatus.setStatus(Status.STARTED);
				try {
					installResources(fContext, fRepo, fResources);
					fPackageAdmin.refreshPackages(null);
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
			        Resource[] res = searchRepository(context, repo, installable.getID(), installable.getVersion());
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
	        		// note: optional resources are not deployed
	        		
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
	            OutputStream os = new FileOutputStream(fileImage);

				HttpGet httpget = new HttpGet(strURL);
				HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
				HttpEntity entity = response.getEntity();
				if (entity != null) {
	            
		            InputStream is = entity.getContent();
		            byte[] buffer = new byte[4096];
		            int count = 0;
		            for (int len = is.read(buffer); len > 0; len = is.read(buffer))
		            {
		                count += len;
		                os.write(buffer, 0, len);
		            }
		            os.close();
		            is.close();
				}	            
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

		HttpPost httppost = new HttpPost("/dysoweb/repo/contents/registerinstall");

		List formparams = new ArrayList();
        
        // post install info the the repo server
		// retrieve the list of applications that can be installed
		formparams.add(new BasicNameValuePair("feature", installable.getSysId()));
		formparams.add(new BasicNameValuePair("LocalIP", InetAddress.getLocalHost().getHostAddress()));
		formparams.add(new BasicNameValuePair("java.version", System.getProperty("java.version")));
		formparams.add(new BasicNameValuePair("java.vendor", System.getProperty("java.vendor")));
		formparams.add(new BasicNameValuePair("os.name", System.getProperty("os.name")));
		formparams.add(new BasicNameValuePair("os.arch", System.getProperty("os.arch")));
		formparams.add(new BasicNameValuePair("os.version", System.getProperty("os.version")));
		// send name and some platform info for support and OBR repository
		
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
    		formparams.add(new BasicNameValuePair("bundles", sb.toString()));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");		
		httppost.setEntity(entity);
		
		// execute method and handle any error responses.
		m_httpClient.execute(m_targetHost, httppost);
		
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
        StringBuffer sb = new StringBuffer("(|(symbolicname=");
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
		
		// is there an install config?
		File file = new File(fConfigDir, "dysoweb.p12");
		if(!file.exists()) {
			// redirect to settings
			initSettingsValues(request);
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure/installsettings.jsp");
			rd.forward(request, response);
			return;
		}

		// init the socket factory
		initHttpClient(elConfig);
		
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
	
	private void updateSettingsValues(HttpServletRequest request) {
		HttpSession session = request.getSession();
		
		updateValue(session, request, "RepoURL");
		updateValue(session, request, "AuthKey");
		updateValue(session, request, "Version");
		updateValue(session, request, "Settings");
		updateValue(session, request, "Proxy");
		updateValue(session, request, "ProxyHost");
		updateValue(session, request, "ProxyPort");
		updateValue(session, request, "ProxyAuth");
		updateValue(session, request, "LocalCacheURL");
	}

	private void updateValue(HttpSession session, HttpServletRequest request,
			String param) {
		String value = request.getParameter(param);
		if(value != null) {
			setValueInSession(session, param, value);
		}
	}
	
	private void initSettingsValues(HttpServletRequest request) {
		HttpSession session = request.getSession();

		String repoURL = getValueFromSession(session, "RepoURL");
		if(repoURL != null) {
			// already done
			return;
		}
		
		// load the config
		Element elConfig = getServerConfig(fConfigDir);
		repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = InstallServlet.DEFAULT_REPO;
		}
		setValueInSession(session, "RepoURL", repoURL);
		// auth key
		String authKey = XMLUtils.getChildText(elConfig, "AuthKey");
		if(authKey == null) {
			authKey = "";
		}
		setValueInSession(session, "AuthKey", authKey);
		
		// version
		String version = XMLUtils.getChildText(elConfig, "Version");
		if(version == null) {
			version = "demo";
		}
		setValueInSession(session, "Version", version);
		
		// settings
		String settings = XMLUtils.getChildText(elConfig, "Settings");
		if(settings == null) {
			settings = "auto";
		}
		setValueInSession(session, "Settings", settings);
		
		// proxy
		String proxy = XMLUtils.getChildText(elConfig, "Proxy");
		if(proxy == null) {
			proxy = "auto";
		}
		setValueInSession(session, "Proxy", proxy);
		
		// proxy host
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		if(proxyHost == null) {
			proxyHost = "";
		}
		setValueInSession(session, "ProxyHost", proxyHost);
		
		// proxy port
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		if(proxyPort == null) {
			proxyPort = "3128";
		}
		setValueInSession(session, "ProxyPort", proxyPort);
		
		// proxy auth
		String proxyAuth = XMLUtils.getChildText(elConfig, "ProxyAuth");
		if(proxyAuth == null) {
			proxyAuth = "";
		}
		setValueInSession(session, "ProxyAuth", proxyAuth);
		
		// localcache
		String local = XMLUtils.getChildText(elConfig, "LocalCacheURL");
		if(local == null) {
			local = "";
		}
		setValueInSession(session, "LocalCacheURL", local);
	}	
	
	private String getValueFromSession(HttpSession session, String name) {
		return (String)session.getAttribute(COM_REQUEA_DYSOWEB_PANEL+name);
	}
	private void setValueInSession(HttpSession session, String name, String value) {
		session.setAttribute(COM_REQUEA_DYSOWEB_PANEL+name, value);
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
		
		HttpGet httpget = new HttpGet("/dysoweb/repo/contents/repository.xml");
		HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
        
            InputStream is = entity.getContent();
		
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
						HttpGet httpgetImg = new HttpGet(strImage);
						HttpResponse responseImg = m_httpClient.execute(m_targetHost, httpgetImg);
						HttpEntity entityImg = responseImg.getEntity();
						if (entityImg != null) {
				        
				            InputStream isImg = entity.getContent();
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
	}


	private Installable[] parseInstallablesFromFeatures(Element elConfig) throws Exception {
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = DEFAULT_REPO;
		}
		
		HttpGet httpget = new HttpGet("/dysoweb/repo/contents/feature.xml");
		HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
        
            InputStream is = entity.getContent();
		
			Document doc = XMLUtils.parse(is);
			Element elFeatures = doc.getDocumentElement();
			if("error".equals(elFeatures.getLocalName())) {
				throw new Exception(XMLUtils.getChildText(elFeatures, "message"));
			}
			is.close();
			return Installable.parseAsFeature(elFeatures);
		} else {
			// no feature
			return new Installable[0];
		}
	}

	public static Element getServerConfig(File configDir) {

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

	
	public static void saveConfig(File configDir, String authKey, String strPassword) throws Exception {

		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		
		Element elConfig = getServerConfig(configDir);
		if(elConfig == null) {
			elConfig = XMLUtils.newElement("server");
		}
		// store the password
		if(strPassword != null && strPassword.length() > 0) {
			setElementValue(elConfig, "Password", SecurityServlet.encrypt(strPassword));
		}
		if(authKey != null && authKey.length() > 0) {
			setElementValue(elConfig, "AuthKey", authKey);
		}

		// output the content as XML
		Source source = new DOMSource(elConfig);
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

	private static void setElementValue(Element elServer, String tag, String value)
			throws RegistrationException {
		
		Element elPassword = XMLUtils.getChild(elServer, tag);
		if(elPassword == null) 
			elPassword = XMLUtils.addElement(elServer, tag);
		
		XMLUtils.setText(elPassword, value);
	}

	private DefaultHttpClient createHttpClient(
			SchemeRegistry schemeRegistry) {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);
		HttpConnectionParams.setSoTimeout(params, 30 * 1000);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);	
		ClientConnectionManager cm = new ThreadSafeClientConnManager(schemeRegistry);		
		DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
		return httpClient;
	}
	
	private SchemeRegistry createSchemeRegistry()
				throws KeyStoreException, FileNotFoundException, IOException,
				NoSuchAlgorithmException, CertificateException,
				UnrecoverableKeyException, KeyManagementException {

		SchemeRegistry schemeRegistry = new SchemeRegistry();		
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

		
		File fileCertificate = new File(fConfigDir, "dysoweb.p12");
		if(fileCertificate.exists()) {
			String strCertPassword = "dysoweb";
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			InputStream keyInput = new FileInputStream(fileCertificate);
			keyStore.load(keyInput, strCertPassword.toCharArray());
			keyInput.close();           
	
			// does an HTTPS request to the repo passing the client certificates
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, strCertPassword.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
	
			SSLSocketFactory sf = new SSLSocketFactory(
					sslContext,
					SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			Scheme https = new Scheme("https", 443, sf);
	
			schemeRegistry.register(https);
		} else {
			schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
		}
		return schemeRegistry;
	}
	
	public void initHttpClient(Element elConfig) throws Exception {

		SchemeRegistry schemeRegistry = createSchemeRegistry();
		
        DefaultHttpClient httpClient = createHttpClient(schemeRegistry);
        
		// init proxy settings
		String settings = XMLUtils.getChildText(elConfig, "Settings");
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		String proxyUserName = XMLUtils.getChildText(elConfig, "ProxyUsername");
		String proxyUserPassword = XMLUtils.getChildText(elConfig, "ProxyPassword");
		
		// ssl client certificate and proxy settings
		if("manual".equals(settings) && proxyHost != null && proxyHost.length() > 0) {
			// force proxy settings
			try {
				HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
		        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		        // set proxy credentials
				httpClient.getCredentialsProvider().setCredentials(
		                new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
		                new UsernamePasswordCredentials(proxyUserName, proxyUserPassword));
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
		
		m_httpClient = httpClient;
        m_targetHost = new HttpHost("repo.requea.com", 443, "https");
		
	}
	
	

	private void removeConfigValue(Element elConfig, String name) {
		Element elVal = XMLUtils.getChild(elConfig, name);
		if(elVal != null) {
			elConfig.removeChild(elVal);
		}
	}

	private void saveConfigValue(HttpServletRequest request, Element elConfig,
			String name) {
		
		HttpSession session = request.getSession();
		String value = (String)session.getAttribute(COM_REQUEA_DYSOWEB_PANEL+name);
		if(value == null) {
			value = "";
		}
		Element elVal = XMLUtils.getChild(elConfig, name);
		if(elVal == null) {
			elVal = XMLUtils.addElement(elConfig, name);
		}
		XMLUtils.setText(elVal, value);
	}
	
	private void saveSettings(HttpServletRequest request) {
		// save the content in the config file
		Element elConfig = getServerConfig(fConfigDir);
		saveConfigValue(request, elConfig, "Settings");
		String settings = (String)request.getSession().getAttribute(COM_REQUEA_DYSOWEB_PANEL+"Settings");
		if("manual".equals(settings)) {
			saveConfigValue(request, elConfig, "RepoURL");
			saveConfigValue(request, elConfig, "ProxyHost");
			saveConfigValue(request, elConfig, "ProxyPort");
			saveConfigValue(request, elConfig, "LocalCacheURL");
		} else {
			removeConfigValue(elConfig, "ProxyHost");
			removeConfigValue(elConfig, "ProxyPort");
			removeConfigValue(elConfig, "ProxyAuth");
			removeConfigValue(elConfig, "LocalCacheURL");
		}

		// save the config
		try {
	        String xml = XMLUtils.DocumentToString(elConfig.getOwnerDocument(), true);
	        File file = new File(fConfigDir, "server.xml");
	        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
	        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
	        w.write(xml);
	        w.close();
		} catch(Exception e) {
			request.setAttribute(ErrorTag.ERROR, e);
		}
	}
	


	private void updateServerRegistration(String authKey, String password) throws Exception {

		HttpPost httppost;
		// check if there is a certificate?
		File fileCert = new File(fConfigDir, "dysoweb.p12"); 
		if(fileCert.exists()) {
			httppost = new HttpPost("/dysoweb/repo/contents/update");
		} else {
			httppost = new HttpPost("/dysoweb/repo/init");
		}

		List formparams = new ArrayList();
		
		
		Element elConfig = getServerConfig(fConfigDir);
		if(m_httpClient == null) 
			initHttpClient(elConfig);
		
		Element elServer = null;
		String sysId = null;


		// send name and some platform info for support and OBR repository
		// selection
		String name = null;
		try {
			name = InetAddress.getLocalHost().getHostName();
		} catch (Throwable e) {
			// ignore
			name = "unknown";
		}

		formparams.add(new BasicNameValuePair("Name", name));

		try {
			Document doc = XMLUtils.parse(new FileInputStream(new File(fConfigDir, "server.xml")));
			elServer = doc.getDocumentElement();
		} catch(Exception e) {
			elServer = null;
		}

		formparams.add(new BasicNameValuePair("LocalIP", InetAddress.getLocalHost().getHostAddress()));
		formparams.add(new BasicNameValuePair("java.version", System.getProperty("java.version")));
		formparams.add(new BasicNameValuePair("java.version", System.getProperty("java.version")));
		formparams.add(new BasicNameValuePair("java.vendor", System.getProperty("java.vendor")));
		formparams.add(new BasicNameValuePair("os.name", System.getProperty("os.name")));
		formparams.add(new BasicNameValuePair("os.arch", System.getProperty("os.arch")));
		formparams.add(new BasicNameValuePair("os.version", System.getProperty("os.version")));
		
		if(authKey != null && authKey.length() > 0) {
			formparams.add(new BasicNameValuePair("AuthKey", authKey.trim()));
		}
		
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");		
		httppost.setEntity(entity);
		
		// execute method and handle any error responses.
		HttpResponse resp = m_httpClient.execute(m_targetHost, httppost);
		HttpEntity respEntity = resp.getEntity();
		if(respEntity != null) {
			
			InputStream in = respEntity.getContent();
			// parse the XML document if any
			Document doc = XMLUtils.parse(in);
			in.close();
			Element el = doc.getDocumentElement();
			if("error".equals(el.getTagName())) {
				String strMsg = XMLUtils.getChildText(el, "message");
				throw new Exception(strMsg);
			}
			// retrieve the certificate if any
			String strCertificate = XMLUtils.getChildText(el, "certificate");
			if(strCertificate != null) {
				byte[] certificate = Base64.decode(strCertificate);
				// p12 or cert?
				if("application/x-x509-ca-cert".equals(el.getAttribute("type"))) {
					fileCert = new File(fConfigDir, "dysoweb.crt");			
				} else {
					fileCert = new File(fConfigDir, "dysoweb.p12");				
				}
				FileOutputStream fos = new FileOutputStream(fileCert);
				fos.write(certificate);
				fos.close();
			}
			// get the server id
			sysId = el.getAttribute("sysId");
	
			// update the server config
			elServer = getServerConfig(fConfigDir);
			if (sysId != null)
				elServer.setAttribute("sysId", sysId);
			
			// set the authkey (if any)
			if(authKey != null) {
	            Element elVal = XMLUtils.getChild(elConfig, name);
	            if(elVal == null) {
	                elVal = XMLUtils.addElement(elConfig, name);
	            }
	            XMLUtils.setText(elVal, authKey.trim());
			}
	
			// output the content as XML
			Source source = new DOMSource(elServer);
			StringWriter sw = new StringWriter();
	
			StreamResult result = new StreamResult(sw);
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.setOutputProperty("indent", "yes");
			xformer.transform(source, result);
			String xml = sw.toString();
	
			OutputStream os = new FileOutputStream(new File(fConfigDir, "server.xml"));
			Writer w = new OutputStreamWriter(os, "UTF-8");
			w.write(xml);
			w.close();
		}
	}

	public File getConfigDir(ServletContext servletContext) {
		File dir = null;
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(this.getClass().getClassLoader());
			InitialContext ic = new InitialContext();
			Context nc = (Context) ic.lookup("java:comp/env");
			dir = new File((String) (nc.lookup("dysoweb.home")),"config");
		} catch (NamingException nex) {
			// unable to lookup the requea configuration file
			dir = new File(SecurityFilter.getScratchDir(servletContext),"config");
		} finally {
			th.setContextClassLoader(cl);
		}
		return dir;
	}
	
	
}

