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
import java.net.InetAddress;
import java.net.MalformedURLException;
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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;
import org.osgi.service.packageadmin.PackageAdmin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.bundlerepository.ObrCommandImpl;
import com.requea.dysoweb.panel.bundlerepository.RepositoryAdminImpl;
import com.requea.dysoweb.panel.bundlerepository.ResolverImpl;
import com.requea.dysoweb.panel.monitor.AjaxProgressMonitor;
import com.requea.dysoweb.panel.monitor.IProgressMonitor;
import com.requea.dysoweb.panel.monitor.SubProgressMonitor;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.xml.ISO8601DateTimeFormat;
import com.requea.dysoweb.panel.utils.xml.XMLException;
import com.requea.dysoweb.panel.utils.xml.XMLUtils;


public class InstallServlet extends HttpServlet {

	public static final String CATEGORIES = "com.requea.dysoweb.categories";
	public static final String FEATURES = "com.requea.dysoweb.features";

	public static final String CATEGORY = "com.requea.dysoweb.category";
	public static final String FEATURE = "com.requea.dysoweb.feature";
	public static final String INSTALLEDFEATURE = "com.requea.dysoweb.installedfeature";

	private static final String INSTALL_MONITOR = "com.requea.dysoweb.panel.install.monitor";
	private static final String INSTALL_STATUS = "com.requea.dysoweb.panel.install.status";

	private static final long serialVersionUID = -680556291402571674L;
	
    protected static final DateFormat format = new ISO8601DateTimeFormat();
	
	private File fConfigDir;
	private SSLSocketFactory fSocketFactory;
	private RepositoryAdminImpl fRepo;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityFilter.getConfigDir(config.getServletContext());
		fConfigDir.mkdirs();
		// check if server certificate is there
		File file = new File(fConfigDir, "dysoweb.p12");
		if(!file.exists()) {
			try {
				SecurityServlet.updateServerRegistration(fConfigDir, null, null, null);
			} catch(Exception e) {
				// ignore
			}
		}
		
		// create the repo
		BundleContext context = Activator.getDefault().getContext();
		fRepo = new RepositoryAdminImpl(context);
		fRepo.setSSLSocketFactory(getSSLSocketFactory());

		addRepoURL();

		// register the repo service
		context.registerService(
                RepositoryAdmin.class.getName(),
                fRepo, null);
		
		// register the repo command
        // We dynamically import the impl service API, so it
        // might not actually be available, so be ready to catch
        // the exception when we try to register the command service.
        try
        {
            // Register "obr" impl command service as a
            // wrapper for the bundle repository service.
            context.registerService(
                org.apache.felix.shell.Command.class.getName(),
                new ObrCommandImpl(context, fRepo), null);
        }
        catch (Throwable th)
        {
            // Ignore.
        	th.printStackTrace();
        }
	}
	
	private void addRepoURL() {
		String url = System.getProperty("com.requea.dysoweb.repo");
		if(url == null) {
			url = SecurityServlet.DEFAULT_REPO;
		}
		if(!url.endsWith("/")) {
			url += "/";
		}
		url += "contents/repository.xml";
		
		try {
			fRepo.addRepository(new URL(url));
		} catch (MalformedURLException e) {
			// cannot happen
		} catch (Exception e) {
			// ignore
		}
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
		
		Element elConfig = getServerConfig();
		// first of all, check if the server is a registered server
		if (elConfig == null) {
			// include registration page
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure.jsp");
			rd.forward(request, response);
			return;
		}

		String op = request.getParameter("op");
		if("status".equals(op)) {
			handleStatusRequest(request, response);
			return;
		} else if("install".equals(op)) {
			handleInstallBundleRequest(request, response, elConfig);
		} else if("image".equals(op)) {
			// render the image
			String image = request.getParameter("image");
			if(image != null) {
				File f = new File(fConfigDir, "features/"+image);
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
				request.setAttribute(ErrorTag.ERROR, "Unable to retrieve installable application list. Please try later");
				RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/install.jsp");
				rd.forward(request, response);
			}
		}
	}

	private void handleStatusRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			// retrieve the monitor
			HttpSession session = request.getSession();
			AjaxProgressMonitor monitor = (AjaxProgressMonitor)session.getAttribute(INSTALL_MONITOR);
			Status status = (Status)session.getAttribute(INSTALL_STATUS);
			
			if(monitor == null || status == null) {
				return;
			}
			// retrieve the monitor result
			Element el = XMLUtils.newElement("div");

			// set the status
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
	    	Source source = new DOMSource(el);
			StringWriter out = new StringWriter();
			
			StreamResult result = new StreamResult(out);
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.setOutputProperty("indent", "yes");
			xformer.transform(source, result);
			
	        String xml = out.toString();

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
		} catch (TransformerConfigurationException e) {
			throw new ServletException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new ServletException(e);
		} catch (TransformerException e) {
			throw new ServletException(e);
		}
	}

	private void handleInstallBundleRequest(HttpServletRequest request,
			HttpServletResponse response, Element elConfig) throws IOException, ServletException {

		
		BundleContext context = Activator.getDefault().getContext();

		try {
			Feature[] allFeatures = getFeatures(request, response, elConfig);
			
			ArrayList lst = new ArrayList();

			Feature installedFeature = null;
			
			// get the list of features that need to be installed
			Map map = request.getParameterMap();
			Iterator iter = map.keySet().iterator();
			while(iter.hasNext()) {
				String strParam = (String)iter.next();
				if(strParam.startsWith("feature")) {
					Object value = map.get(strParam);
					if(value instanceof String[]) {
						String[] vals = (String[])value;
						if(vals.length > 0)
							value = vals[0];
					}
					if("install".equals(value)) {
						String strFeature = strParam.substring("feature".length());
						// find the requested feature
						Feature feature = findFeature(allFeatures, strFeature);
						if(feature == null) {
							throw new Exception("Unable to find feature '"+strFeature+"'. Cannot deploy");
						}
						installedFeature = feature;
						
						// find dependent features
						getDependentFeatures(lst, allFeatures, feature);
						if(!lst.contains(feature)) {
							// adds at the end
							lst.add(feature);
						}
					}
				}
			}
			
			// turn it as an array
			Feature[] features = (Feature[])lst.toArray(new Feature[lst.size()]);
			
			// once we have the repo, we ask for deployment
			
			// create the progress monitor
			AjaxProgressMonitor monitor = new AjaxProgressMonitor();
			Status status = new Status();
			request.getSession().setAttribute(INSTALL_MONITOR, monitor);
			request.getSession().setAttribute(INSTALL_STATUS, status);

			addRepoURL();
			// launch the trhead to install the bundles
			Thread th = new Thread(new Installer(context, fRepo, features, monitor, status));
			th.start();
			
			request.setAttribute(FEATURE, installedFeature);
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/installing.jsp");
			rd.forward(request, response);
		} catch (Exception e) {
			// show the error
			request.setAttribute(ErrorTag.ERROR, "Unable to install features: " + e.getMessage());
			RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/install.jsp");
			rd.forward(request, response);
		}
	}

	private void getDependentFeatures(List lst, Feature[] allFeatures,
			Feature feature) {

		List depends = feature.getDependsOn();
		for(int i=0; i<depends.size(); i++) {
			String sysId = (String)depends.get(i);
			
			// find the feature
			Feature f = null;
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
		
		
		private RepositoryAdminImpl fRepo;
		private BundleContext fContext;
		private Feature[] fFeatures;
		private IProgressMonitor fProgressMonitor;
		private Status fStatus;
		
		Installer(BundleContext context, RepositoryAdminImpl repo, Feature[] features, IProgressMonitor monitor, Status status) {
			fContext = context;
			fFeatures = features;
			fRepo = repo;
			fProgressMonitor = monitor;
			fStatus = status;
		}
		
		public void run() {
			
			// calc the total download size
			long lTotalSize = 0;
			try {
				lTotalSize = calcTotalDownloadSize(fContext, fRepo, fFeatures);
			} catch (Exception e) {
				// we will not get the total size
				lTotalSize = 0;
			}
			
			try {
				fProgressMonitor.beginTask("Installing feature ", lTotalSize == 0 ? IProgressMonitor.UNKNOWN : (int)lTotalSize);
				fStatus.setStatus(Status.STARTED);
				for(int i=0; i<fFeatures.length; i++) {
					try {
						installFeature(fContext, fRepo, fFeatures[i], fProgressMonitor);
					} catch(Exception e) {
						fStatus.setError(e);
		        		// abort
		        		return;
					}
				}
				// Get package admin service and request a refresh
		        ServiceReference ref = fContext.getServiceReference(
		            org.osgi.service.packageadmin.PackageAdmin.class.getName());
		        if (ref != null) {
			        PackageAdmin pa = (PackageAdmin) fContext.getService(ref);
			        if (pa != null) {
			        	pa.refreshPackages(null);
			        }
		        }

				fStatus.setStatus(Status.DONE);
			} catch(Exception e) {
				fStatus.setError(e);
			} finally {
				fProgressMonitor.done();
			}
		}
	}
	
	private long calcTotalDownloadSize(BundleContext context, RepositoryAdminImpl repo, Feature[] features) throws Exception {

		
		ResolverImpl resolver = (ResolverImpl)repo.resolver();
		// add the bundles for all the features
		for(int i=0; i<features.length; i++) {
			
			Feature feature = features[i];
			String bundles = feature.getBundleList();
			
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
		}
		
		// then resolve
		resolver.resolve();
		
		// get the list of resources added
		long lSize = 0;
		Map deployMap = new HashMap();
		Resource[] resources = resolver.getAddedResources();
		if(resources != null) {
			for(int i=0; i<resources.length; i++) {
	            deployMap.put(resources[i], resources[i]);
			}
		}
		resources = resolver.getRequiredResources();
		if(resources != null) {
			for(int i=0; i<resources.length; i++) {
	            deployMap.put(resources[i], resources[i]);
			}
		}
		resources = resolver.getOptionalResources();
		if(resources != null) {
			for(int i=0; i<resources.length; i++) {
	            deployMap.put(resources[i], resources[i]);
			}
		}
		Iterator iter = (Iterator)deployMap.values().iterator();
		while(iter.hasNext()) {
			Resource res = (Resource)iter.next();
			Long size = (Long)res.getProperties().get("size");
			if(size != null)
				lSize += size.longValue();
		}
		return lSize;
	}
	
	private void installFeature(BundleContext context, RepositoryAdminImpl repo, Feature feature, IProgressMonitor parentMonitor) throws Exception {

		// create an XML document for the feature
		Element elFeature = XMLUtils.newElement("feature");
		elFeature.setAttribute("id", feature.getID());
		String version = feature.getVersion();
		if(version != null) {
			elFeature.setAttribute("version", version);
		}
		// store the feature info
		XMLUtils.addElement(elFeature, "title", feature.getName());
		Element elDesc = XMLUtils.addElement(elFeature, "description");
		XMLUtils.setCDATA(elDesc, feature.getDescription());
		
		elDesc = XMLUtils.addElement(elFeature, "longDesc");
		XMLUtils.setCDATA(elDesc, feature.getLongDesc());
		synchronized (format) {
			elFeature.setAttribute("date", format.format(new Date()));
		}
		
		Element elImage = feature.getImage();
		if(elImage != null) {
			elImage = (Element)elFeature.getOwnerDocument().importNode(elImage, true);
			elFeature.appendChild(elImage);
		}
		Element elBundles = XMLUtils.addElement(elFeature, "bundles");
		
		String bundles = feature.getBundleList();
		
		// retrieve the bundles id
		StringTokenizer st = new StringTokenizer(bundles,",");
        ResolverImpl resolver = (ResolverImpl)repo.resolver();
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
        		resources = resolver.getOptionalResources();
        		if(resources != null && resources.length > 0) {
        			for(int i=0; i<resources.length; i++) {
        				deployMap.put(resources[i], resources[i]);
        			}
        		}
        		Iterator iter = deployMap.values().iterator();
        		while(iter.hasNext()) {
        			Resource resource = (Resource)iter.next();
    				Long size = (Long)resource.getProperties().get("size");
    				if(size != null) {
    					lSize += size.longValue();
    				}
                	Element elBundle = XMLUtils.addElement(elBundles, "bundle");
                	if(resource.getSymbolicName() != null) {
    	                elBundle.setAttribute("symbolicName", resource.getSymbolicName());
                	}
	                elBundle.setAttribute("name", resource.getPresentationName());
	                elBundle.setAttribute("version", resource.getVersion().toString());
        		}
        		IProgressMonitor monitor = new SubProgressMonitor(parentMonitor, (int)lSize);
        		try {
        			monitor.beginTask("Installing feature " + feature.getName(), (int)lSize);
	                try
	                {
	            		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, (int)lSize);
	                    resolver.deploy(true, subMonitor);
	                    // register the new application if something was installed
                    	registerInstalledApplication(feature, elFeature);
	                }
	                catch (IllegalStateException ex)
	                {
	                    throw ex;
	                }
        		} finally {
        			monitor.done();
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
	}
	
	private void registerInstalledApplication(Feature feature, Element elFeature) throws Exception {
		
		String fileName = feature.getID();
		String version = feature.getVersion();
		if(version != null) {
			fileName += "-"+version;
		}
		fileName += ".xml";
		
		File file = new File(fConfigDir, "features/"+fileName);
		file.getParentFile().mkdirs();
		
		// get the image
		Element elImage = XMLUtils.getChild(elFeature, "rqImage");
		if(elImage != null) {
			String sysId = elImage.getAttribute("sysId");
			String strURL = elImage.getAttribute("url");
			// copy the image on the local directory
			try {
				File fileImage = new File(fConfigDir, "features/"+sysId+".jpg");
				URL url = new URL(strURL);
	            OutputStream os = new FileOutputStream(fileImage);
	            URLConnection conn = url.openConnection();
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
	            elFeature.removeChild(elImage);
	            XMLUtils.addElement(elFeature, "image", sysId+".jpg");
			} catch(Exception e) {
				
			}
		}
		
		// store the content as XML
    	Source source = new DOMSource(elFeature);
		StringWriter out = new StringWriter();
		
		StreamResult result = new StreamResult(out);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty("indent", "yes");
		xformer.transform(source, result);
		
        String xml = out.toString();
        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        w.write(xml);
        w.close();
        
        // post install info the the repo server
        try {
    		// retrieve the list of applications that can be installed
    		String strUrl = System.getProperty("com.requea.dysoweb.repo");
    		if(strUrl == null) {
    			strUrl = SecurityServlet.DEFAULT_REPO;
    		}
    		if(!strUrl.endsWith("/")) {
    			strUrl += "/";
    		}
    		strUrl += "contents/registerinstall";
        	
			URL url = new URL(strUrl);
			URLConnection connection = url.openConnection();
	        if(connection instanceof HttpsURLConnection) {
	        	SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
	        	if(sslSocketFactory != null) {
	        		HttpsURLConnection secureCon = (HttpsURLConnection) connection;
	                secureCon.setSSLSocketFactory(sslSocketFactory);
	        	}
	        }
			connection.setDoOutput(true);
			OutputStreamWriter osw = new OutputStreamWriter(
	                connection.getOutputStream());
			
			// send name and some platform info for support and OBR repository
			osw.write("feature=" + feature.getSysId());
			osw.write("&LocalIP=" + URLEncoder.encode(InetAddress.getLocalHost().getHostAddress(), "UTF-8"));
			osw.write("&java.version=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8"));
			osw.write("&java.vendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "UTF-8"));
			osw.write("&os.name=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
			osw.write("&os.arch=" + URLEncoder.encode(System.getProperty("os.arch"), "UTF-8"));
			osw.write("&os.version=" + URLEncoder.encode(System.getProperty("os.version"), "UTF-8"));
			
			// bundles
			StringBuffer sb = new StringBuffer();
			Element elBundles = XMLUtils.getChild(elFeature, "bundles");
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

	
	private void handleInstallPanelRequest(HttpServletRequest req,
			HttpServletResponse resp, Element elConfig) throws Exception {

		Feature[] features = getFeatures(req, resp, elConfig);
		Feature feature = findFeature(features, req.getParameter("feature"));
		// lookup for the feature
		if(feature != null) {
			req.setAttribute(FEATURE, feature);
			// show feature detail
			RequestDispatcher rd = req.getRequestDispatcher("/dysoweb/panel/installfeat.jsp");
			rd.forward(req, resp);
		} else {
			// show list of features
			RequestDispatcher rd = req.getRequestDispatcher("/dysoweb/panel/install.jsp");
			rd.forward(req, resp);
		}		
		return;
	}
	
	
	private Feature findFeature(Feature[] features, String strFeature) {
		Feature feature = null;
		if(features != null && strFeature != null) {
			// retrieve the feature
			for(int i=0; feature == null && i<features.length; i++) {
				if(strFeature.equals(features[i].getID())) {
					feature = features[i];
				}
			}
		}
		return feature;
	}

	private Feature[] getFeatures(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		// do we already have the list of features and categories?
		HttpSession session = req.getSession();
		Feature[] features;
		Object o = session.getAttribute(FEATURES);
		if(!"true".equals(req.getParameter("refresh")) && o instanceof Feature[]) {
			// ok, we have the list of features
			features = (Feature[])o;
			req.setAttribute(FEATURES, features);
			req.setAttribute(CATEGORIES, session.getAttribute(CATEGORIES));
		} else {
			loadFeatures(req, resp, elConfig);
			features = (Feature[])req.getAttribute(FEATURES);
		}
		return features;
	}

	private void loadFeatures(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		HttpSession session = req.getSession();
		
		// retrieve the list of applications that can be installed
		String strUrl = System.getProperty("com.requea.dysoweb.repo");
		if(strUrl == null) {
			strUrl = SecurityServlet.DEFAULT_REPO;
		}
		if(!strUrl.endsWith("/")) {
			strUrl += "/";
		}
		strUrl += "contents/feature.xml";
		
        URL url = new URL(strUrl);
        URLConnection con = url.openConnection();
        if(con instanceof HttpsURLConnection) {
        	SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        	if(sslSocketFactory != null) {
        		HttpsURLConnection secureCon = (HttpsURLConnection) con;
                secureCon.setSSLSocketFactory(sslSocketFactory);
        	}
        }

        try {
			Document doc = XMLUtils.parse(con.getInputStream());
			// parse the list of installable features
			Feature[] features = Feature.parse(doc.getDocumentElement());
	
			// get the category map
			Map categories = Feature.buildCategories(features);
			
			// ok, we have the list of features
			session.setAttribute(FEATURES, features);
			req.setAttribute(FEATURES, features);
			session.setAttribute(CATEGORIES, categories);
			req.setAttribute(CATEGORIES, categories);
		} catch(XMLException e) {
			req.setAttribute(ErrorTag.ERROR, "Unable to find repository information. Please try later, or check the repository URL");
		}
	}

	private Element getServerConfig() {

		File f = new File(fConfigDir, "server.xml");
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
