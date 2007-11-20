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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.requea.dysoweb.processor.IFilterDefinition;

public class FilterListChain implements FilterChain {

	private IFilterDefinition[] fFilters;
	private ServletChain fServletChain;
	private int fPos;

	public FilterListChain(IFilterDefinition[] filters, ServletChain ac) {
		fFilters = filters;
		fServletChain = ac;
		fPos = 0;
	}

	public void doFilter(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {

		if(fPos < fFilters.length) {
			Thread th = Thread.currentThread();
			ClassLoader cl = th.getContextClassLoader();
			try {
				IFilterDefinition fltDef = fFilters[fPos];
				// invoke the filter
				Filter flt = fltDef.getInstance();
				// we have progressed in the chain
				fPos ++;
				if(fltDef.getLoader() != null)
					th.setContextClassLoader(fltDef.getLoader());
				flt.doFilter(request, response, this);
			} finally {
				th.setContextClassLoader(cl);
			}
		} else if(fServletChain != null) {
			// we are done with the filters: invoke the servlet chain
			fServletChain.doFilter(request, response);
		}
	}

}
