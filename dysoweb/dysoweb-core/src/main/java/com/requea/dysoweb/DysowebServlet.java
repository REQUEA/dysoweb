package com.requea.dysoweb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.requea.webenv.DysowebSessionSerializer;
import com.requea.webenv.IWebProcessor;

public class DysowebServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Felix fPlatform;

	private static Bundle fActiveProcessorBundle;
	private static IWebProcessor fActiveProcessor;
	private static Version fActiveProcessorVersion;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		ServletContext ctx = config.getServletContext();
		// starts the osgi platform
		startFelix(ctx);
	}

	public static Felix getPlatform() {
		return fPlatform;
	}
	
	public void destroy() {
		
		stopFelix();
		super.destroy();
	}

	public static synchronized void stopFelix() {
		if(fPlatform != null) {
			try {
				preSerializeSessions();
				fPlatform.stop();
				// wait 5 seconds to have everything ok
				Thread.sleep(5000);
			} catch (BundleException e) {
				// ignore
			} catch (InterruptedException e) {
				// ignore
			}
			fPlatform = null;
		}
	}

	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {

		if (fActiveProcessor != null) {
			// chain with the Request processor from the OSGI platform
			fActiveProcessor.process(request, response, null);
		} else {
			super.service(request, response);
		}
	}

	private static final String TMP_DIR = "javax.servlet.context.tempdir";

	/*
	 * Retrieve the web container scratch directory
	 */
	public static File getScratchDir(ServletContext context) {
		// First try the Servlet 2.2 javax.servlet.context.tempdir property
		File scratchDir = (File) context.getAttribute(TMP_DIR);
		if (scratchDir == null) {
			// Not running in a Servlet 2.2 container.
			// Try to get the JDK 1.2 java.io.tmpdir property
			String dir = System.getProperty("java.io.tmpdir");
			if (dir != null)
				scratchDir = new File(dir);
		}
		return scratchDir;
	}

	/*
	 * Starts the OSGI platform based on an embedded felix
	 */
	public static synchronized void startFelix(ServletContext ctx)
			throws ServletException {

		// check if already started
		if(fPlatform != null) {
			return;
		}
		
		// set proxy autodetect
		try {
		    System.setProperty("java.net.useSystemProxies","true");
		} catch (SecurityException e) {
		    ; // failing to set this property isn't fatal
		}
		
		// the bundles are placed undef the WEB-INF directory
		Map configMap = new StringMap(false);

		// loads the dysoweb properties files
		try {
			Properties props = new Properties();
			InputStream is = ctx.getResourceAsStream("/WEB-INF/classes/dysoweb.properties");
			if(is != null) {
				props.load(is);
			}
			configMap.putAll(props);
		} catch(IOException e) {
			// cannot load the dysoweb properties: throw a servlet exception to stop intialisation 
			throw new ServletException(e);
		}

		// javax autoexport packages
		String str = (String)configMap.get("autoexport.packages");
		if(str != null) {
			ArrayList lst = new ArrayList();
			StringTokenizer st = new StringTokenizer(str, ",");
			while(st.hasMoreTokens()) {
				String token = st.nextToken();
				// get the package info
				Package pkg = Package.getPackage(token);
				if(pkg != null && !lst.contains(pkg)) {
					lst.add(pkg);
				}
			}
		}

		// add extra properties
		configMap.put(FelixConstants.SERVICE_URLHANDLERS_PROP, "true");
		configMap.put(FelixConstants.FRAMEWORK_BUNDLE_PARENT, "framework");
		
		// parse the autostart property, and add URL handlers
		Iterator iter = configMap.keySet().iterator();
		while(iter.hasNext()) {
			String key = (String)iter.next();
			if(key.startsWith(AutoActivator.AUTO_START_PROP)) {
				String autoStart = (String)configMap.get(key);
				StringBuffer sb = new StringBuffer();
				StringTokenizer st = new StringTokenizer(autoStart,",");
				while(st.hasMoreTokens()) {
					if(sb.length() > 0) {
						// this property is space delimited
						sb.append(" ");
					}
					String token = st.nextToken();
					// cleanup the token
					token = token.replaceAll("[ \t\r\n]", "");
					try {
						URL res = ctx.getResource("/WEB-INF/bundle/"+token);
						if(res != null) {
							sb.append(res.toString());
						} else {
							sb.append(token);
						}
					} catch(MalformedURLException e) {
						// ignore this one
					}
				}
				configMap.put(key, sb.toString());
			}
		}
		// creates the cache directory
		File fCache = null;
		try {
			InitialContext ic = new InitialContext();
			Context nc = (Context) ic.lookup("java:comp/env");
			String home = (String) nc.lookup("dysoweb.home");
			File basedir = null;
			if("dysoweb.home".equals(home)) {
				if(System.getProperty("jboss.home.dir") != null) {
					basedir = new File(new File(System.getProperty("jboss.home.dir")), "dysoweb.home");
				} else if(System.getProperty("catalina.home") != null) {
					basedir = new File(new File(System.getProperty("catalina.home")), "dysoweb.home");
				} else if(System.getProperty("jonas.base") != null) {
					basedir = new File(new File(System.getProperty("jonas.base")), "dysoweb.home");
				} else if(System.getProperty("weblogic.home") != null) {
					basedir = new File(new File(System.getProperty("weblogic.home")), "dysoweb.home");
				}
			}
			if(basedir == null) {
				// use default value
				basedir = new File(home);
			}
			basedir.mkdirs();
			fCache = basedir;
		} catch (NamingException nex) {
			// unable to lookup the requea configuration file
			fCache = getScratchDir(ctx);

			System.out.println("-----------------------------");
			System.out.println("INFO: The dysoweb bundle cache directory will be set to " + fCache.getAbsolutePath());
			System.out.println("We recommend that you setup the dysoweb.home Context Environment variable 'dysoweb.home' in your web container");
			System.out.println("    <Environment name=\"dysoweb.home\" type=\"java.lang.String\" value=\"[path to directory]\"/>");
			System.out.println("    see http://dysoweb.requea.com/dysopedia/index.php/Configuring_dysoweb_home");
			System.out.println("-----------------------------");
		}
		fCache.mkdirs();
		// setup the local cache path
		configMap.put(BundleCache.CACHE_ROOTDIR_PROP, fCache.getAbsolutePath());
		configMap.put("org.osgi.framework.storage", "bundles");
		
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(DysowebServlet.class.getClassLoader());
			
            List list = new ArrayList();
            list.add(new AutoActivator(configMap));
            
            configMap.put("felix.systembundle.activators", list);

            // Create a case-insensitive property map.
            Map cisMap = new StringMap(configMap, false);
			
			// Now create an instance of the framework.
			Felix felix = new Felix(cisMap);
			felix.start();
			fPlatform = felix;
			
			// get the Dysoweb processors
			registerProcessorListener(ctx, felix.getBundleContext());
			
		} catch (Exception ex) {
			throw new ServletException(ex);
		} finally {
			th.setContextClassLoader(cl);
		}
	}

	private static void registerProcessorListener(final ServletContext servletContext, final BundleContext context) {
		
		retrieveActiveProcessor(servletContext, context);
		
		// and register a listener
		ServiceListener sl = new ServiceListener() {
			
			public void serviceChanged(ServiceEvent ev) {
				ServiceReference sr = ev.getServiceReference();
				switch (ev.getType()) {
					case ServiceEvent.REGISTERED: {
						// check if the version is better
						try {
							Version v = Version.parseVersion((String)sr.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
							if(fActiveProcessorVersion == null || v.compareTo(fActiveProcessorVersion) > 0) {
								setActiveProcessor(servletContext, context, sr);
							}
						} catch(Exception e) {
							// ignore this bundle
						}
					}
					break;
					case ServiceEvent.UNREGISTERING: {
						// are we talking about the active processor?
						if(sr.getBundle() == fActiveProcessorBundle) {
							// yes!
							retrieveActiveProcessor(servletContext, context);
						}
					}
					break;
				}
			}
		};
		String filter = "(objectclass=" + IWebProcessor.class.getName() + ")";
		try {
			context.addServiceListener(sl, filter);
		} catch (InvalidSyntaxException e) {
			// ignore this one
		}
	}

	private static void retrieveActiveProcessor(ServletContext servletContext, BundleContext context) {
		// get the service ref of all dysoweb apps
		try {
			ServiceReference[] srs = context.getServiceReferences(IWebProcessor.class.getName(), 
					null);
			if(srs != null && srs.length > 0) {
				// get the one with the best version
				ServiceReference ref = getBestVersion(srs);
				if(srs.length > 1) {
					System.out.println("Active Dysoweb Processor is bundle " + ref.getBundle().getBundleId());
				}
				setActiveProcessor(servletContext, context, ref);
			} else {
				// deactivate the old one if there is one
				if(fActiveProcessor != null) {
					fActiveProcessor.deactivate();
				}
				// no active processor
				fActiveProcessor = null;
				fActiveProcessorBundle = null;
				fActiveProcessorVersion = null;
			}
		} catch (InvalidSyntaxException e) {
			// ignore this one
		}
	}

	private static void setActiveProcessor(ServletContext servletContext, BundleContext context, ServiceReference ref) {
		if(fPlatform == null) {
			return;
		}
		Bundle bundle = ref.getBundle();
		String strVer = (String) ref.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
		Version v = null;
		if(strVer != null) {
			v = Version.parseVersion(strVer);
		}
		IWebProcessor processor = (IWebProcessor)context.getService(ref);
		
		synchronized (fPlatform) {
			// deactivate the old one
			if(fActiveProcessor != null) {
				fActiveProcessor.deactivate();
			}
			// activarte the new one
			try {
				processor.activate(servletContext, null);
				// activation was successful
				fActiveProcessor = processor;
				fActiveProcessorBundle = bundle;
				fActiveProcessorVersion = v;
				// register the processor in the servlet context
				servletContext.setAttribute("com.requea.dysoweb.processor", processor);
			} catch (Throwable e) {
				System.err.println("ERROR: cannot activate Dysoweb processor:" + e.getMessage());
				// bad: the new one could not be activated!!!
				// try to reactivate the old one if there is one
				if(fActiveProcessor != null) {
					try {
						fActiveProcessor.activate(servletContext, null);
					} catch (Throwable e1) {
						// at this point, there is nothing that we can do
						fActiveProcessor = null;
						fActiveProcessorBundle = null;
						fActiveProcessorVersion = null;
						System.err.println("ERROR: cannot activate Dysowe processor:" + e1.getMessage());
					}
				}
			}
			
		}
	}

	private static ServiceReference getBestVersion(ServiceReference[] srs) {
		ServiceReference found = null;
		Version foundVer = null;
		for(int i=0; i<srs.length; i++) {
			ServiceReference sr = srs[i];
			String strVer = (String) sr.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
			if(strVer != null) {
				Version v = Version.parseVersion(strVer);
				if(foundVer == null) {
					found = sr;
					foundVer = v;
				} else if(v.compareTo(foundVer) >0) {
					// better version
					found = sr;
					foundVer = v;
				}
			}
		}
		// return whatever was found
		return found;
	}

	/**
	 * Retrieve the currently active processor
	 * @return
	 */
	public static IWebProcessor getActiveProcessor() {
		if(fPlatform == null) {
			return null;
		}
		synchronized (fPlatform) {
			return fActiveProcessor;
		}
	}

	private static Map fSessions = new ConcurrentHashMap();

	public static void registerSession(HttpSession session) {
		fSessions.put(session.getId(), session);
	}
	
	public static void unregisterSession(HttpSession session) {
		fSessions.remove(session.getId());
	}
	
	private static void preSerializeSessions() {
		Iterator iter = fSessions.values().iterator();
		while(iter.hasNext()) {
			HttpSession session = (HttpSession) iter.next();
			
			// grab the attributes
			Enumeration e  = session.getAttributeNames();
			while(e.hasMoreElements()) {
				String name = (String) e.nextElement();
				Object obj = session.getAttribute(name);
				if(obj instanceof DysowebSessionSerializer) {
					try {
						((DysowebSessionSerializer)obj).preSerialize();
					} catch (SecurityException ex) {
						// ignore
					} catch (IOException ex) {
						// ignore
					}
				}
			}
		}
	}
}
