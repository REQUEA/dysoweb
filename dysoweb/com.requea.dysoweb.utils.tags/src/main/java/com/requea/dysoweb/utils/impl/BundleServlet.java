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

package com.requea.dysoweb.utils.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// retrieve the op
		String op = request.getParameter("op");
		String bn = request.getParameter("bndl");
		
		Bundle bundle = null;
		Activator act = Activator.getInstance();
		if(act != null) {
			// retrieve the bundle
			Bundle[] bundles = act.getContext().getBundles();
			for(int i=0; bundle == null && i<bundles.length; i++) {
				String sym = bundles[i].getSymbolicName();
				if(sym != null && sym.equals(bn)) {
					// we have found the bundle
					bundle = bundles[i];
				}
			}
		}
		
		
		try {
			// launch the op
			if(bundle != null) {
				if("start".equals(op)) {
					bundle.start();
				} else if("stop".equals(op)) {
					bundle.stop();
				}
			}
		} catch(BundleException e) {
			// TODO
		}
		
		// redirect to the refererer (if any)
		String ru = request.getParameter("ru");
		if(ru == null)
			ru = request.getHeader("Referer");
		if(ru == null)
			ru = request.getContextPath() + "/index.jsp";
		
		// redirect to the ru
    	response.sendRedirect(ru);
	}

	
	
}
