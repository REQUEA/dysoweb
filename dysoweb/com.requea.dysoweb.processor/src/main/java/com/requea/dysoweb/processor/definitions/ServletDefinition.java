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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.processor.IServletDefinition;
import com.requea.dysoweb.util.xml.XMLUtils;

/**
 * A servlet definition from the web.xml
 * @author Pierre Dubois
 *
 */
public class ServletDefinition implements IServletDefinition {

    private static Log fLog = LogFactory.getLog(ServletDefinition.class);
	
	
	private String fName;
	private String fClassName;
	private Servlet fInstance;
	private Map<String,String>    fParams;
	private boolean fInitialized;
	private int fLoadOnStartup;
	private ServletContext fServletContext;
	private List<String> fMappings = new ArrayList<String>();

	private long fBundleId;


	private ClassLoader fLoader;
	
	

	public ServletDefinition(long bundleId, String name, String cls, int loadOnStartup) {
		fBundleId = bundleId;
		fName = name;
		fClassName = cls;
		fLoadOnStartup = loadOnStartup;
	}

	public ClassLoader getLoader() {
		return fLoader;
	}

	public synchronized void loadClass(Bundle bundle) throws WebAppException {
		if(fClassName == null) {
			fLoader = null;
		} else {
			Class cls;
			try {
				cls = bundle.loadClass(fClassName);
			} catch (ClassNotFoundException e) {
				throw new WebAppException(e);
			}
			fLoader = new LoaderWrapper(cls.getClassLoader());
		}
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
		if(fLoader == null) {
			throw new WebAppException("Unable to retrieve class loader for servlet definition");
		}
		
		fLog.info("Loading Servlet " + fName);
		
		try {
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
		
		fInstance.init(new Config());
		fInitialized = true;
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
		if(fInstance != null && fInitialized) {
			fLog.info("Destroying Servlet " + fName);
			fInstance.destroy();
		}
		fInstance = null;
		fInitialized = false;
	}

	public String getClassName() {
		return fClassName;
	}

	public String getInitParameter(String name) {
		return fParams.get(name);
	}

	public Map<String, String> getInitParameters() {
		return fParams;
	}

	public boolean setInitParameter(String arg0, String arg1) {
		fParams.put(arg0,  arg1);
		return false;
	}

	public Set<String> setInitParameters(Map<String, String> arg0) {
		Set<String> params = new HashSet<String>();
		// TODO
		return params;
	}

	public String getRunAsRole() {
		// TODO Auto-generated method stub
		return null;
	}

}
