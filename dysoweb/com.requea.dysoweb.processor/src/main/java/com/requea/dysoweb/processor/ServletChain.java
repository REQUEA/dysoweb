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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.requea.dysoweb.processor.IFilterDefinition;
import com.requea.dysoweb.processor.IServletDefinition;

public class ServletChain implements FilterChain {

	private IServletDefinition fServletDefinition;
	private IFilterDefinition[] fFilterDefinitions;
	private int fPos;
	private ServletContext fServletContext;

	public ServletChain(ServletContext servletContext, IServletDefinition servletDefinition, IFilterDefinition[] filterDefinitions) {
		fServletContext = servletContext;
		fServletDefinition = servletDefinition;
		fFilterDefinitions = filterDefinitions;
		fPos = 0;
	}

	public void doFilter(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {

		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		
		if(fPos < fFilterDefinitions.length) {
			// invoke the filter
			try {
				IFilterDefinition def = fFilterDefinitions[fPos];
				Filter flt = def.getInstance();
				// we have progressed in the chain
				fPos ++;
				if(def.getLoader() != null)
					th.setContextClassLoader(def.getLoader());
				flt.doFilter(request, response, this);
			} finally {
				th.setContextClassLoader(cl);
			}
		} else if(fServletDefinition != null) {
			// we are done with the filters: invoke the servlet
			try {
				if(fServletDefinition.getLoader() != null)
					th.setContextClassLoader(fServletDefinition.getLoader());
				if(!fServletDefinition.isInitialized()) {
					fServletDefinition.init(fServletContext);
				}
				fServletDefinition.getInstance().service(request, response);
			} finally {
				th.setContextClassLoader(cl);
			}
		}
	}
}
