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

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.util.xml.XMLUtils;

/**
 * A filter definition from the web.xml
 * @author Pierre Dubois
 *
 */
public class FilterDefinition implements IFilterDefinition {

    private static Log fLog = LogFactory.getLog(FilterDefinition.class);
	
	private String fName;
	private String fClassName;
	private Filter fInstance;
	private Map    fParams;
	private boolean fInitialized;
	private ServletContext fServletContext;

	private long fBundleId;

	private ClassLoader fLoader;

	public FilterDefinition(long bundleId, String name, String cls) {
		fBundleId = bundleId;
		fName = name;
		fClassName = cls;
	}
	
	public long getBundleId() {
		return fBundleId;
	}
	
	public String getName() {
		return fName;
	}

	public Filter getInstance() {
		return fInstance;
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
	
	public synchronized void load() throws WebAppException {
		if(fInstance != null) {
			// already loaded
			return;
		}
		if(fLoader == null) {
			throw new WebAppException("Unable to retrieve class loader for filter definition");
		}
		
		fLog.info("Loading Filter " + fName);
		
		Thread th = Thread.currentThread();
		ClassLoader contextClassLoader = th.getContextClassLoader();
		
		try {
			// instantiate the servlet
			Class cls = fLoader.loadClass(fClassName);
			// set the class loader as the context class loader
			th.setContextClassLoader(cls.getClassLoader());
			Object obj = cls.newInstance();
			if(obj instanceof Filter) {
				fInstance = (Filter)obj;
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
	 * Initialize the filter based on the definition
	 */
	public synchronized void init(ServletContext context) throws ServletException {
		if(fInitialized) {
			return;
		}
		fServletContext = context;
		if(fInstance == null) {
			return;
		}
		
		fLog.info("Initializing Filter " + fName);
		
		fInstance.init(new Config());
		fInitialized = true;
	}

	class Config implements FilterConfig {

		public String getFilterName() {
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

	public boolean isInitialized() {
		return fInitialized;
	}

	public void unload() {
		if(fInstance != null) {
			fLog.info("Destroying Filter " + fName);
			fInstance.destroy();
		}
		fInstance = null;
		fInitialized = false;
	}

}
