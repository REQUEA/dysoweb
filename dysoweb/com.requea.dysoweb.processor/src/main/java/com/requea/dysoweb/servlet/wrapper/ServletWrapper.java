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

package com.requea.dysoweb.servlet.wrapper;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.Bundle;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.processor.IServletDefinition;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.processor.ServletChain;


public class ServletWrapper {
	
	private long fBundleId;
	private ServletContext fServletContext;
	private IServletDefinition fServletDefinition;
	private IFilterDefinition[] fFilterDefinitions;

	public ServletWrapper() {
		
	}
	public ServletWrapper(long bundleId, ServletContext context, IServletDefinition def) {
		fBundleId = bundleId;
		fServletDefinition = def;
		fServletContext = context;
		fFilterDefinitions = new IFilterDefinition[0];
	}
	
	public ServletWrapper(long bundleId, ServletContext context, HttpServlet servlet, ClassLoader loader) {
		fBundleId = bundleId;
		fServletContext = context;
		fServletDefinition = new ServletDefinition(servlet, loader);
		fFilterDefinitions = new IFilterDefinition[0];
	}


	public long getBundleId() {
		return fBundleId;
	}
	
	public synchronized void addFilter(IFilterDefinition flt) {
		IFilterDefinition[] defs = new IFilterDefinition[fFilterDefinitions.length+1];
		for(int i=0; i<defs.length-1; i++) {
			defs[i] = fFilterDefinitions[i];
		}
		defs[defs.length-1] = flt;
		// set the new filter list
		fFilterDefinitions = defs;
	}
	
	public IServletDefinition getServletDefinition() {
		return fServletDefinition;
	}
	
	/*
	 * Delegate the processing of the request to the underlying servlet
	 */
	public synchronized ServletChain getChain() throws ServletException, IOException {
		// create a filter chain for the context
		return new ServletChain(fServletContext, fServletDefinition, fFilterDefinitions);
	}

	private class ServletDefinition implements IServletDefinition {

		private HttpServlet fServlet;
		private ClassLoader fLoader;

		public ServletDefinition(HttpServlet servlet, ClassLoader loader) {
			fServlet = servlet;
			fLoader = loader;
		}

		public long getBundleId() {
			return -1;
		}

		public Servlet getInstance() {
			return fServlet;
		}

		public int getLoadOnStartup() {
			return 0;
		}

		public String getName() {
			return null;
		}

		public void init(ServletContext ctx) throws ServletException {
			
		}

		public boolean isInitialized() {
			return true;
		}

		public void load() throws WebAppException {
			
		}

		public void unload() {
			
		}

		public ClassLoader getLoader() {
			return fLoader;
		}

		public void loadClass(Bundle bundle) throws WebAppException {
			// nothing to do: already loadded
		}
		

	}

}
