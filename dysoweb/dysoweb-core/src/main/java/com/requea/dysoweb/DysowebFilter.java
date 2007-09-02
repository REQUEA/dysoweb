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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;


import com.requea.webenv.IWebProcessor;
import com.requea.webenv.WebContext;

public class DysowebFilter implements Filter {

	private Felix fPlatform;


	public void init(FilterConfig config) throws ServletException {
		
		ServletContext ctx = config.getServletContext();
		
		// initialize web environement 
		String str = ctx.getRealPath("/");
		WebContext.setBaseDir(str == null ? null : new File(str));
		WebContext.setScratchDir(getScratchDir(ctx));
		WebContext.setServletContext(ctx);
		String prefix = config.getInitParameter("RequestPrefix");
		WebContext.setRequestPrefix(prefix);
		// starts the osgi platform
		startFelix(ctx);
	}
	

	public void destroy() {
		// stop the felix platform
		if (fPlatform != null) {
//TODO			fPlatform.shutdown();
			fPlatform = null;
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		IWebProcessor processor = WebContext.getProcessor();
		if (processor != null) {
			// chain with the Request processor from the OSGI platform
			processor.process(request, response, chain);
		} else {
			// regular chain processing
			chain.doFilter(request, response);
		}
	}

	/*
	 * Starts the OSGI platform based on an embedded felix
	 */
	private synchronized void startFelix(ServletContext ctx)
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
		File fCache = new File(getScratchDir(ctx), "bundles/");
		fCache.mkdirs();
		// setup the local cache path
		configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, fCache.getAbsolutePath());
		configMap.put("osgi.contextClassLoaderParent", "ccl");
		
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(this.getClass().getClassLoader());
			// Now create an instance of the framework.
			Felix felix = new Felix(configMap, null);
			felix.start();
			fPlatform = felix;
		} catch (Exception ex) {
			throw new ServletException(ex);
		} finally {
			th.setContextClassLoader(cl);
		}
	}
	
	private static final String TMP_DIR = "javax.servlet.context.tempdir";

	/*
	 * Retrieve the web container scratch directory
	 */
	private File getScratchDir(ServletContext context) {
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

}
