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

package com.requea.dysoweb.demo5;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SecurityFilter implements Filter {

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		// get the bundle
		String bn = request.getParameter("bndl");
		String op = request.getParameter("op");
		if(!"start".equals(op) && 
				(bn == null || (
				!bn.equals("com.requea.dysoweb.demo2") &&
				!bn.equals("com.requea.dysoweb.demo3") &&
				!bn.equals("com.requea.dysoweb.demo4")))) {
			// unallowed operation
			try {
				RequestDispatcher rd = request.getRequestDispatcher("/demo5/jsp/error.jsp");
				rd.forward(request, response);
			} catch(Throwable t) {
				t.printStackTrace();
			}
		} else {
			// regular processing
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig cfg) throws ServletException {
		
	}

}
