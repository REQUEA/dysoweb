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
import java.io.Writer;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;
import org.osgi.service.packageadmin.PackageAdmin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.selector.fixed.FixedProxySelector;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.UrlPacScriptSource;
import com.requea.dysoweb.panel.InstallManager.Status;
import com.requea.dysoweb.panel.monitor.AjaxProgressMonitor;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.Base64;
import com.requea.dysoweb.panel.utils.ISO8601DateTimeFormat;
import com.requea.dysoweb.service.obr.ClientAuthRepositoryAdmin;
import com.requea.dysoweb.service.obr.HttpClientExecutor;
import com.requea.dysoweb.service.obr.IProgressMonitor;
import com.requea.dysoweb.service.obr.MonitoredResolver;
import com.requea.dysoweb.service.obr.SubProgressMonitor;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;

public class InstallManager {

	private static final int TIMEOUT = 60;
	public static final String DEFAULT_REPO = "https://repo.requea.com/dysoweb/repo/contents/repository.xml";
    public static final String VERSIONS = "com.requea.dysoweb.versions";
    public static final String CURRENTVERSION = "com.requea.dysoweb.currentversion";
	public static final String CATEGORIES = "com.requea.dysoweb.categories";
    public static final String INSTALLABLES = "com.requea.dysoweb.installables";
	
	private File fConfigDir;

	private CloseableHttpClient m_httpClient;

	private HttpHost m_targetHost;

    protected static final DateFormat format = new ISO8601DateTimeFormat();
	

	public InstallManager(File configDir) {
		fConfigDir = configDir;
	}

	public RepositoryAdmin initRepo(File configDir, Element elConfig, String ver) throws Exception {
		
		// get the repo
		RepositoryAdmin repo = Activator.getDefault().getRepo();
		if(repo == null) {
			// not initialized
			throw new Exception("No repository service available at this time");
		}

		// ssl client certificate and proxy settings
		if(repo instanceof ClientAuthRepositoryAdmin) {
			ClientAuthRepositoryAdmin clr = (ClientAuthRepositoryAdmin)repo;
			clr.setHttp(new HttpClientExecutor() {
				
				public InputStream executeGet(String path) throws IOException {
					if(path.startsWith("http")) {
						HttpGet httpget = new HttpGet(path);
						HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
						HttpEntity entity = response.getEntity();
						if (entity != null) {
				            InputStream is = entity.getContent();
				            return is;
						} else {
							return null;
						}
					} else {
						URL url = new URL(path);
						URLConnection cnx = url.openConnection();
						if(cnx != null && cnx.getContentLength() > 0) {
							InputStream is = cnx.getInputStream();
							return is;
						} else {
							return null;
						}
					}
				}
			});
		}
		
		// local cache URL if any
		String localCacheURL = XMLUtils.getChildText(elConfig, "LocalCacheURL");
		if(localCacheURL != null && localCacheURL.length() > 0) {
			repo.addRepository(new URL(localCacheURL));
		}
		
		// update the repo URL
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null || repoURL.equals("")) {
			repoURL = InstallManager.DEFAULT_REPO;
		}
		// add repo version
        if(ver != null && !"".equals(ver) && !"base".equals(ver)) {
            if(repoURL.indexOf('?') < 0)
                repoURL += "?ver="+ver;
            else
                repoURL += "&ver="+ver;
        }
		
		repo.addRepository(new URL(repoURL));
		
		return repo;
	}
	
	public void initHttpClient(Element elConfig) throws Exception {

        SSLContext sslcontext = createSSLContext();
        // Allow client cert
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

        // setup timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT * 1000)
                .setConnectTimeout(TIMEOUT * 1000)
                .build();        

        // build http client connexion
        HttpClientBuilder builder = HttpClients.custom();
        
		// ssl client certificate
		builder.setDefaultRequestConfig(requestConfig)
			.setSSLSocketFactory(sslsf);
        
        
		// init proxy settings
		String settings = XMLUtils.getChildText(elConfig, "Settings");
		String proxyPAC = XMLUtils.getChildText(elConfig, "ProxyPAC");
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		final String proxyUserName = XMLUtils.getChildText(elConfig, "ProxyUsername");
		String proxyPassword = XMLUtils.getChildText(elConfig, "ProxyPassword");
		if(proxyPassword != null && proxyPassword.startsWith("3DES:")) {
			CryptUtils cu = CryptUtils.getInstance("3DES");
			proxyPassword = cu.decrypt(proxyPassword.substring("3DES:".length()));
		}
		String proxyNTDomain = XMLUtils.getChildText(elConfig, "ProxyNTDomain");
		
		// force proxy settings with manual settings
		if("manual".equals(settings)) {
			
			ProxySelector selector = null; 
			if(proxyPAC != null && !"".equals(proxyPAC)) {
				UrlPacScriptSource pacSource = new UrlPacScriptSource(proxyPAC);
				selector = new PacProxySelector(pacSource);
			} else if (proxyHost != null && proxyHost.length() > 0) {
				// uses a proxy host
				if(proxyPort == null || proxyPort.equals("")) {
					proxyPort = "80";
				}
				selector = new FixedProxySelector(proxyHost, Integer.parseInt(proxyPort));
			}
			
			// auth
			if(proxyUserName != null && !"".equals(proxyUserName)) {
    			CredentialsProvider credsProvider = new BasicCredentialsProvider();
    			Credentials creds;
    			if(proxyNTDomain != null && !"".equals(proxyNTDomain)) {
        			creds = new NTCredentials(proxyUserName, proxyPassword, proxyNTDomain, null); 
    			} else {
    				creds = new UsernamePasswordCredentials(proxyUserName, proxyPassword);
    			}
    		    credsProvider.setCredentials(
    		    		AuthScope.ANY,
    		    		creds);
    			builder.setDefaultCredentialsProvider(credsProvider);	    
			}
			if(selector != null) {
	            // build the route with the proxy selector
	            SystemDefaultRoutePlanner routePlanner = new SystemDefaultRoutePlanner(selector);
	            // set the route planner with the proxy configuration
	    		builder.setRoutePlanner(routePlanner);
			}
		} else {
            // use proxy-vole for proxy settings auto search
            ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
            proxySearch.setPacCacheSettings(32, 1000*60*5); // Cache 32 urls for up to 5 min.
            ProxySelector selector = proxySearch.getProxySelector();

            // is there a proxy selector?
            if(selector != null) {
                SystemDefaultRoutePlanner routePlanner = new SystemDefaultRoutePlanner(selector);
	            // set the route planner with the proxy configuration
	    		builder.setRoutePlanner(routePlanner);
            }
		}
        
		// build the http client with the config
        m_httpClient = builder.build();
		
        // target host is hard coded
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null || repoURL.equals("")) {
			repoURL = DEFAULT_REPO;
		}
		URL targetURL = new URL(repoURL);
		if(repoURL.startsWith("http")) {
			// init the host
			m_targetHost = new HttpHost(targetURL.getHost(), targetURL.getPort(), targetURL.getProtocol());
		} else {
			m_targetHost = null;
		}
	}
	
	
    private SSLContext createSSLContext() throws IOException {
        try {
    		File certFile = new File(fConfigDir, "dysoweb.p12");
    		if(certFile.exists()) {
        	
	            String strCertPassword = "dysoweb";
	            KeyStore keyStore = KeyStore.getInstance("PKCS12");
	            InputStream keyInput = new FileInputStream(certFile);
	            keyStore.load(keyInput, strCertPassword.toCharArray());
	            keyInput.close();           
	            
	            // does an HTTPS request to the repo passing the client certificates
	            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	            keyManagerFactory.init(keyStore, strCertPassword.toCharArray());
	            SSLContext context = SSLContext.getInstance("TLS");
	            context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
	            return context;
    		} else {
    			// default SSL context
	            SSLContext context = SSLContext.getInstance("TLS");
	            context.init(null, null, null);	            
	            return context;
    		}
        } catch(Exception e) {
            throw new IOException(e);
        }
    }

	public void reset() {
		m_httpClient = null;
		m_targetHost = null;
	}
	
	

	private void registerInstalledApplication(Element elConfig, Installable installable, Element elResource) throws Exception {
		
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL != null && repoURL.length() > 0) {
			URL url = new URL(repoURL);
			if(!url.getProtocol().startsWith("http") || !"repo.requea.com".equals(url.getHost())) {
				// not registrable
				return;
			}
		} else {
			repoURL = InstallManager.DEFAULT_REPO;
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
	            copyImage(repoURL, strURL, fileImage);	            
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

        try {
	        URL url = new URL(repoURL);
			HttpPost httppost = new HttpPost(new URL(url, "registerinstall").toString());
	
			List formparams = new ArrayList();
	        
	        // post install info the the repo server
			// retrieve the list of applications that can be installed
			if(installable.getSysId() != null) {
				formparams.add(new BasicNameValuePair("feature", installable.getSysId()));
			}
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
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");		
				httppost.setEntity(entity);
				
				// execute method and handle any error responses.
				m_httpClient.execute(m_targetHost, httppost);
			}
        } catch(Exception e) {
        	// ignore
        }
	}
	
	
	private void copyImage(String repoURL, String strURL, File fileImage)
			throws FileNotFoundException, IOException, ClientProtocolException {
		OutputStream os = new FileOutputStream(fileImage);

		if(repoURL == null || repoURL.startsWith("http")) {
			HttpGet httpget = new HttpGet(strURL);
			HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
			
			    InputStream is = entity.getContent();
			    byte[] buffer = new byte[4096];
			    for (int len = is.read(buffer); len > 0; len = is.read(buffer))
			    {
			        os.write(buffer, 0, len);
			    }
			    os.close();
			    is.close();
			}
		} else {
			URL baseURL = new URL(repoURL);
			URL imgURL = new URL(baseURL, strURL);
			URLConnection cnx = imgURL.openConnection();
        	if(cnx != null && cnx.getContentLength() > 0) {
			    InputStream is = cnx.getInputStream();
			    byte[] buffer = new byte[4096];
			    for (int len = is.read(buffer); len > 0; len = is.read(buffer))
			    {
			        os.write(buffer, 0, len);
			    }
			    os.close();
			    is.close();
			}
		}
	}
	
	
	public static class Status implements Serializable {

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
	
	public class Installer implements Runnable {
		
		
		private RepositoryAdmin fRepo;
		private BundleContext fContext;
		private Installable[] fResources;
		private IProgressMonitor fProgressMonitor;
		private Status fStatus;
		private PackageAdmin fPackageAdmin;
		private Element fConfig;
		
		Installer(Element elConfig, BundleContext context, PackageAdmin packageAdmin, RepositoryAdmin repo, Installable[] resources, IProgressMonitor monitor, Status status) {
			fContext = context;
			fResources = resources;
			fRepo = repo;
			fPackageAdmin = packageAdmin;
			fProgressMonitor = monitor;
			fStatus = status;
			fConfig = elConfig;
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
				elResource.setAttribute("date", format.format(new Date()));
	            // register the new application if something was installed
	        	registerInstalledApplication(fConfig, installable, elResource);
	        }
		}
				
	}
	


	private static Resource[] searchRepository(BundleContext context, RepositoryAdmin repo, String targetId, String targetVersion)
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
    	


    public static Resource selectNewestVersion(Resource[] resources)
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

	public Installer newInstaller(Element elConfig, BundleContext context,
			PackageAdmin pa, RepositoryAdmin repo, Installable[] installables,
			AjaxProgressMonitor monitor, Status status) {
		return new Installer(elConfig, context, pa, repo, installables, monitor, status);
	}
	
	private void loadInstallables(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		HttpSession session = req.getSession();
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null || repoURL.length() == 0) {
			repoURL = InstallManager.DEFAULT_REPO;
		}
		
		
		InputStream is = null;
		if(repoURL.startsWith("http")) {
			String ver = (String) session.getAttribute(CURRENTVERSION);
			if(ver != null && !"".equals(ver) && !"base".equals(ver)) {
			    if(repoURL.indexOf('?') < 0)
			        repoURL += "?ver="+ver;
			    else
	                repoURL += "&ver="+ver;
			}
			HttpGet httpget = new HttpGet(repoURL);
			HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
	            is = entity.getContent();
			}
		} else {
			URL url = new URL(repoURL);
			URLConnection cnx = url.openConnection();
			is = cnx.getInputStream();
		}
				
		if(is != null) {
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
					String installableName = f.getName();
					String version = f.getVersion();
					if(strImage != null && !strImage.startsWith("http")) {
						downloadImage(repoURL, strImage, installableName, version);
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
		} else {
			req.setAttribute(ErrorTag.ERROR, "Unable to find repository information. Please try later, or check the repository URL");
		}
	}

	private void downloadImage(String repoURL, String strImage, String installableName,
			String version) throws IOException, ClientProtocolException,
			FileNotFoundException {

		if(repoURL == null || repoURL.startsWith("http")) {
			// use http client
			HttpGet httpgetImg = new HttpGet(strImage);
			HttpResponse responseImg = m_httpClient.execute(m_targetHost, httpgetImg);
			HttpEntity entityImg = responseImg.getEntity();
			if (entityImg != null) {
			    InputStream isImg = entityImg.getContent();
				File file = new File(fConfigDir, "products/image/"+installableName+"-"+version+".jpg");
				file.getParentFile().mkdirs();
				OutputStream os = new FileOutputStream(file);
			    byte[] buffer = new byte[4096];
			    for (int len = isImg.read(buffer); len > 0; len = isImg.read(buffer))
			    {
			        os.write(buffer, 0, len);
			    }
			    os.close();
			    isImg.close();
			}
		} else {
			URL url = new URL(repoURL);
			URL imgURL = new URL(url, strImage);
			URLConnection cnx = imgURL.openConnection();
        	if(cnx != null && cnx.getContentLength() > 0) {
			    InputStream isImg = cnx.getInputStream();
				File file = new File(fConfigDir, "products/image/"+installableName+"-"+version+".jpg");
				file.getParentFile().mkdirs();
				OutputStream os = new FileOutputStream(file);
			    byte[] buffer = new byte[4096];
			    for (int len = isImg.read(buffer); len > 0; len = isImg.read(buffer))
			    {
			        os.write(buffer, 0, len);
			    }
			    os.close();
			    isImg.close();
			}
		}
	}

    private void loadVersions(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
        
        HttpSession session = req.getSession();
        String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
        if(repoURL == null || repoURL.length() == 0) {
            repoURL = InstallManager.DEFAULT_REPO;
        }

        InputStream is = null;
        
        if(repoURL == null || repoURL.startsWith("http")) {
            HttpGet httpget = new HttpGet("/dysoweb/repo/contents/config");
            HttpResponse response = m_httpClient.execute(m_targetHost, httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                is = entity.getContent();
            }
        } else {
        	URL baseURL = new URL(repoURL);
        	URL url = new URL(baseURL, "/dysoweb/repo/contents/config");
        	URLConnection cnx = url.openConnection();
        	if(cnx != null && cnx.getContentLength() > 0) {
        		is = cnx.getInputStream();
        	}
        }
        
        if(is != null) {

            try {
                Document doc = XMLUtils.parse(is);
                Element elResult = doc.getDocumentElement();
                if("error".equals(elResult.getLocalName())) {
                    throw new Exception(XMLUtils.getChildText(elResult, "message"));
                }
                List lstVersions = new ArrayList();
                
                Element elVersions = XMLUtils.getChild(elResult, "versions");
                if(elVersions != null) {
                    // list the versions
                    Element elVersion = XMLUtils.getChild(elVersions, "version");
                    while(elVersion != null) {
                        lstVersions.add(XMLUtils.getTextValue(elVersion));
                        
                        elVersion = XMLUtils.getNextSibling(elVersion);
                    }
                }
                
                // versions loaded
                String[] versions = (String[])lstVersions.toArray(new String[lstVersions.size()]);
                
                session.setAttribute(VERSIONS, versions);
                req.setAttribute(VERSIONS, versions);
                
                String ver = req.getParameter("ver");
                if(ver == null || ver.equals("")) {
                    ver = (String) session.getAttribute(CURRENTVERSION);
                }
                if(ver != null ) {
                    session.setAttribute(CURRENTVERSION, ver);
                    req.setAttribute(CURRENTVERSION, ver);
                }
                
            } catch(XMLException e) {
                req.setAttribute(ErrorTag.ERROR, "Unable to find repository information. Please try later, or check the repository URL");
            }
        }
    }


	private Installable[] parseInstallablesFromFeatures(Element elConfig) throws Exception {
		String repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null || repoURL.length() == 0) {
			repoURL = InstallManager.DEFAULT_REPO;
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
	
	public Installable[] getInstallables(HttpServletRequest req, HttpServletResponse resp, Element elConfig) throws Exception {
		
		// do we already have the list of installables and categories?
		HttpSession session = req.getSession();
		Installable[] installables;
		Object o = session.getAttribute(INSTALLABLES);
		if(!"true".equals(req.getParameter("refresh")) && o instanceof Installable[]) {
			// ok, we have the list of resources
			installables = (Installable[])o;
			req.setAttribute(INSTALLABLES, installables);
			req.setAttribute(CATEGORIES, session.getAttribute(CATEGORIES));
            req.setAttribute(VERSIONS, session.getAttribute(VERSIONS));
            String currentVersion = (String)session.getAttribute(CURRENTVERSION);
            if(currentVersion != null) {
                req.setAttribute(CURRENTVERSION, currentVersion);
            }
		} else {
		    loadVersions(req,resp,elConfig);
			loadInstallables(req, resp, elConfig);
			installables = (Installable[])req.getAttribute(INSTALLABLES);
		}
		return installables;
	}


	public void updateServerRegistration(String repoURL, String authKey, String password) throws Exception {

		if(repoURL != null && !repoURL.startsWith("http")) {
			// do nothing if not http
			return;
		}
		// check if there is a certificate?
		File fileCert = new File(fConfigDir, "dysoweb.p12"); 

		URL url = new URL(repoURL);
		// check if there is a certificate?
		if(fileCert.exists()) {
			url = new URL(url, "update");
		} else {
			url = new URL(url, "../init");
		}
		
		HttpPost httppost = new HttpPost(url.toString());

		List formparams = new ArrayList();
		
		
		Element elConfig = getServerConfig(fConfigDir);
		// reinit http client, since proxy settings may have changed
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
			String xml = XMLUtils.ElementToString(elServer, true);
	
			OutputStream os = new FileOutputStream(new File(fConfigDir, "server.xml"));
			Writer w = new OutputStreamWriter(os, "UTF-8");
			w.write(xml);
			w.close();
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
	
}
