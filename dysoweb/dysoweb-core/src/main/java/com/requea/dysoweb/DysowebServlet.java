package com.requea.dysoweb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;

import com.requea.webenv.IWebProcessor;
import com.requea.webenv.WebContext;

public class DysowebServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Felix fPlatform;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		ServletContext ctx = config.getServletContext();
		
		// initialize web environement 
		String str = ctx.getRealPath("/");
		WebContext.setBaseDir(str == null ? null : new File(str));
		WebContext.setScratchDir(getScratchDir(ctx));
		WebContext.setServletContext(ctx);
		String prefix = config.getInitParameter("RequestPrefix");
		WebContext.setRequestPrefix(prefix);
		// starts the osgi platform
		fPlatform = startFelix(ctx);
	}

	public void destroy() {
		super.destroy();
		if(fPlatform != null) {
			fPlatform = null;
		}
	}


	public void service(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {

		IWebProcessor processor = WebContext.getProcessor();
		if (processor != null) {
			// chain with the Request processor from the OSGI platform
			processor.process(request, response, null);
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
	public static synchronized Felix startFelix(ServletContext ctx)
			throws ServletException {

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
			// list all packages
			for(int i=0; i<lst.size(); i++) {
				System.out.println(lst.get(i).toString());
			}
		}

		// add extra properties
		configMap.put(FelixConstants.SERVICE_URLHANDLERS_PROP, "false");
		// parse the autostart property, and add URL handlers
		Iterator iter = configMap.keySet().iterator();
		while(iter.hasNext()) {
			String key = (String)iter.next();
			if(key.startsWith(FelixConstants.AUTO_START_PROP)) {
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
		File fCache = new File(DysowebServlet.getScratchDir(ctx), "bundles/");
		fCache.mkdirs();
		// setup the local cache path
		configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, fCache.getAbsolutePath());
		configMap.put("osgi.contextClassLoaderParent", "ccl");
		
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(DysowebServlet.class.getClassLoader());
			// Now create an instance of the framework.
			Felix felix = new Felix(configMap, null);
			felix.start();
			return felix;
		} catch (Exception ex) {
			throw new ServletException(ex);
		} finally {
			th.setContextClassLoader(cl);
		}
	}
	
	
}
