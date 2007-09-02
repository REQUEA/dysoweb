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

package com.requea.dysoweb.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.requea.dysoweb.processor.RequestProcessor.EntryInfo;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class ServletContextWrapper implements ServletContext {

	private static final Object ATTRIBUTE_SET = Boolean.TRUE;
	private ServletContext fContext;
	private RequestProcessor fRequestProcessor;
	private Map fParams;
	private Map fAttributesMap;

	public ServletContextWrapper(RequestProcessor requestProcessor, ServletContext servletContext, Map contextParams) {
		fRequestProcessor = requestProcessor;
		fContext = servletContext;
		fParams = contextParams;
		fAttributesMap = new ConcurrentHashMap();
	}

	
	
	private class ParamEnumeration implements Enumeration {

		
		private Enumeration fParentEnum;
		private Iterator fParamsIterator;

		public ParamEnumeration(Iterator params, Enumeration attributeNames) {
			fParamsIterator = params;
			fParentEnum = attributeNames;
		}

		public boolean hasMoreElements() {
			if(fParamsIterator.hasNext()) {
				return true;
			} else {
				return fParentEnum.hasMoreElements();
			}
		}

		public Object nextElement() {
			if(fParamsIterator.hasNext()) {
				return fParamsIterator.next();
			} else if(fParentEnum.hasMoreElements()) {
				return fParentEnum.nextElement();
			} else {
				return null;
			}
		}
		
	}
	
	public Enumeration getAttributeNames() {
		return fContext.getAttributeNames();
	}

	public ServletContext getContext(String arg0) {
		return fContext.getContext(arg0);
	}

	public String getInitParameter(String name) {
		String value = (String)fParams.get(name);
		return value != null ? value : fContext.getInitParameter(name);
	}

	public Enumeration getInitParameterNames() {
		return new ParamEnumeration(fParams.keySet().iterator(), fContext.getInitParameterNames());
	}

	public int getMajorVersion() {
		return fContext.getMajorVersion();
	}

	public String getMimeType(String arg0) {
		return fContext.getMimeType(arg0);
	}

	public int getMinorVersion() {
		return fContext.getMinorVersion();
	}

	public RequestDispatcher getNamedDispatcher(String arg0) {
		return fContext.getNamedDispatcher(arg0);
	}

	public String getRealPath(String path) {
		if(path.startsWith("$scratch")) {
			path = fRequestProcessor.getScratchDir() + path.substring("$scratch".length());
			return  path;
		} else {
			return fContext.getRealPath(path);
		}
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		return fContext.getRequestDispatcher(arg0);
	}

	public URL getResource(String path) throws MalformedURLException {
		if(path.startsWith("$scratch")) {
			path = fRequestProcessor.getScratchDir() + path.substring("$scratch".length());
			return new File(path).toURL();
		} else {
			// retrieve the entry
			EntryInfo info;
			try {
				info = fRequestProcessor.getEntryInfo(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(info != null) {
				return info.getLocalURL();
			} else {
				return null;
			}
		}
	}

	public InputStream getResourceAsStream(String path) {
		if(path.startsWith("$scratch")) {
			path = fRequestProcessor.getScratchDir() + path.substring("$scratch".length());
			try {
				return new FileInputStream(new File(path));
			} catch (FileNotFoundException e) {
				return null;
			}
		} else {
			// retrieve the entry
			EntryInfo info;
			try {
				info = fRequestProcessor.getEntryInfo(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(info != null) {
				URL url = info.getLocalURL();
				try {
					return url.openStream();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				// go up to the parent servlet context
				return fContext.getResourceAsStream(path);
			}
		}
	}

	public Set getResourcePaths(String arg0) {
		return fContext.getResourcePaths(arg0);
	}

	public String getServerInfo() {
		return fContext.getServerInfo();
	}

	public Servlet getServlet(String arg0) throws ServletException {
		// deprecated
		return null;
	}

	public String getServletContextName() {
		return fContext.getServletContextName();
	}

	public Enumeration getServletNames() {
		// deprecated
		return null;
	}

	public Enumeration getServlets() {
		// deprecated
		return null;
	}

	public void log(Exception arg0, String arg1) {
		// deprecated
	}

	public void log(String arg0, Throwable arg1) {
		fContext.log(arg0, arg1);
	}

	public void log(String arg0) {
		fContext.log(arg0);
	}

	public Object getAttribute(String name) {
		return fContext.getAttribute(name);
	}
	
	public void removeAttribute(String name) {
		fContext.removeAttribute(name);
		fAttributesMap.remove(name);
	}

	public void setAttribute(String name, Object value) {
		fContext.setAttribute(name, value);
		fAttributesMap.put(name, ATTRIBUTE_SET);
	}


	/**
	 * Cleanup all the attributes that were registered in the ServletContext 
	 * by a bundle
	 */
	public void cleanupAttributes() {
		Iterator iter = fAttributesMap.keySet().iterator();
		while(iter.hasNext()) {
			String name = (String)iter.next();
			fContext.removeAttribute(name);
		}
		fAttributesMap.clear();
	}
		
}
