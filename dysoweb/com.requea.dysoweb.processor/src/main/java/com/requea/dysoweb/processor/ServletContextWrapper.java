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
import java.util.EventListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import com.requea.dysoweb.processor.RequestProcessor.EntryInfo;

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

	@Override
	public Dynamic addFilter(String arg0, String arg1) {
		return fContext.addFilter(arg0, arg1);
	}

	@Override
	public Dynamic addFilter(String arg0, Filter arg1) {
		return fContext.addFilter(arg0, arg1);
	}

	@Override
	public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
		return fContext.addFilter(arg0, arg1);
	}

	@Override
	public void addListener(String arg0) {
		fContext.addListener(arg0);
	}

	@Override
	public <T extends EventListener> void addListener(T arg0) {
		fContext.addListener(arg0);
	}

	@Override
	public void addListener(Class<? extends EventListener> arg0) {
		fContext.addListener(arg0);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			String arg1) {
		return fContext.addServlet(arg0, arg1);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Servlet arg1) {
		return fContext.addServlet(arg0, arg1);
	}

	@Override
	public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0,
			Class<? extends Servlet> arg1) {
		return fContext.addServlet(arg0, arg1);
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> arg0)
			throws ServletException {
		return fContext.createFilter(arg0);
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> arg0)
			throws ServletException {
		return fContext.createListener(arg0);
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> arg0)
			throws ServletException {
		return fContext.createServlet(arg0);
	}

	@Override
	public void declareRoles(String... arg0) {
		fContext.declareRoles(arg0);
	}

	@Override
	public ClassLoader getClassLoader() {
		return fContext.getClassLoader();
	}

	@Override
	public String getContextPath() {
		return fContext.getContextPath();
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return fContext.getDefaultSessionTrackingModes();
	}

	@Override
	public int getEffectiveMajorVersion() {
		return fContext.getEffectiveMajorVersion();
	}

	@Override
	public int getEffectiveMinorVersion() {
		return fContext.getEffectiveMinorVersion();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return fContext.getEffectiveSessionTrackingModes();
	}

	@Override
	public FilterRegistration getFilterRegistration(String arg0) {
		return fContext.getFilterRegistration(arg0);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return fContext.getFilterRegistrations();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return fContext.getJspConfigDescriptor();
	}

	@Override
	public ServletRegistration getServletRegistration(String name) {
		ServletRegistration registration = fRequestProcessor.getServletRegistration(name);
		if(registration == null) {
			registration = fContext.getServletRegistration(name);
		}
		return registration;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return fContext.getServletRegistrations();
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return fContext.getSessionCookieConfig();
	}

	@Override
	public String getVirtualServerName() {
		return fContext.getVirtualServerName();
	}

	@Override
	public boolean setInitParameter(String arg0, String arg1) {
		return fContext.setInitParameter(arg0, arg1);
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
		fContext.setSessionTrackingModes(arg0);
	}
		
}
