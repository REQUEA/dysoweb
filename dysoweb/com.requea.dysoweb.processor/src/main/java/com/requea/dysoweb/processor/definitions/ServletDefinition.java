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

package com.requea.dysoweb.processor.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.requea.dysoweb.WebAppService;
import com.requea.dysoweb.processor.IServletDefinition;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.util.xml.XMLUtils;

/**
 * A servlet definition from the web.xml
 * @author Pierre Dubois
 *
 */
public class ServletDefinition implements IServletDefinition {

    private static Log fLog = LogFactory.getLog(ServletDefinition.class);
	
	
	private long fBundleId;
	private String fName;
	private String fClassName;
	private Servlet fInstance;
	private Map    fParams;
	private boolean fInitialized;
	private int fLoadOnStartup;
	private ServletContext fServletContext;
	private ClassLoader fLoader;
	
	

	public ServletDefinition(WebAppService service, long bundleId, String name, String cls, int loadOnStartup) {
		fBundleId = bundleId;
		fName = name;
		fClassName = cls;
		fLoadOnStartup = loadOnStartup;
		fLoader = new LoaderWrapper(service.getClass().getClassLoader());
	}
	
	public Servlet getInstance() {
		return fInstance;
	}
	

	public long getBundleId() {
		return fBundleId;
	}
	
	public String getName() {
		return fName;
	}

	public boolean isInitialized() {
		return fInitialized;
	}

	public int getLoadOnStartup() {
		return fLoadOnStartup;
	}

	public synchronized void load() throws WebAppException {
		if(fInstance != null) {
			// already loaded
			return;
		}
		
		fLog.info("Loading Servlet " + fName);
		
		Thread th = Thread.currentThread();
		ClassLoader contextClassLoader = th.getContextClassLoader();
		
		try {
			// instantiate the servlet
			th.setContextClassLoader(fLoader);
			Class cls = fLoader.loadClass(fClassName);
			Object obj = cls.newInstance();
			if(obj instanceof Servlet) {
				fInstance = (Servlet)obj;
			} else {
				throw new WebAppException("The class "+fClassName+" is not a Servlet. It should extend javax.servlet.Servlet");
			}
		} catch(ClassNotFoundException e) {
			throw new WebAppException(e);
		} catch (InstantiationException e) {
			throw new WebAppException(e);
		} catch (IllegalAccessException e) {
			throw new WebAppException(e);
		} finally {
			th.setContextClassLoader(contextClassLoader);
		}
	}

	
	/*
	 * init the servlet if not already done
	 */
	public synchronized void init(ServletContext context) throws ServletException {
		if(fInitialized) {
			// already initialized
			return;
		}
		fServletContext = context;
		
		if(fInstance == null) {
			return;
		}
		
		fLog.info("Initializing Servlet " + fName);
		
		Thread th = Thread.currentThread();
		ClassLoader contextClassLoader = th.getContextClassLoader();
		try {
			th.setContextClassLoader(fLoader);
			fInstance.init(new Config());
			fInitialized = true;
		} finally {
			th.setContextClassLoader(contextClassLoader);
		}
	}
	
	class Config implements ServletConfig {

		public String getServletName() {
			return fName;
		}
		
		public String getInitParameter(String param) {
			if(param == null || fParams == null)
				return null;
			
			return (String)fParams.get(param);
		}

		public Enumeration getInitParameterNames() {
			if(fParams == null) {
				return Collections.enumeration(Collections.EMPTY_LIST);
			} else {
				ArrayList lst = new ArrayList();
				lst.addAll(fParams.keySet());
				return Collections.enumeration(lst);
			}
		}

		public ServletContext getServletContext() {
			return fServletContext;
		}

	}
	
	/*
	 * Loads the parameters definition
	 */
	public void loadParams(Element el) {
		
		Map params = new HashMap();
		Element child = XMLUtils.getFirstChild(el);
		while(child != null) {
			if("init-param".equals(child.getTagName())) {
				String name = XMLUtils.getChildText(child, "param-name");
				String value = XMLUtils.getChildText(child, "param-value");
				params.put(name, value);
			}
			// get the next one
			child = XMLUtils.getNext(child);
		}
		// initialize the params
		fParams = params;
	}
	
	public void setInitialized(boolean b) {
		fInitialized = b;
	}

	public void setLoadOnStartup(int loadOnStartup) {
		fLoadOnStartup = loadOnStartup;
	}

	public void unload() {
		if(fInstance != null) {
			fLog.info("Destroying Servlet " + fName);
			fInstance.destroy();
		}
		fInstance = null;
		fInitialized = false;
	}

	public ClassLoader getLoader() { 
		return fLoader;
	}

}
