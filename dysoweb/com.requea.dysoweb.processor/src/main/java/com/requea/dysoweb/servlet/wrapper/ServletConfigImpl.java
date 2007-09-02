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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class ServletConfigImpl implements ServletConfig {

	private ServletContext fContext;
	private String fName;
	private Map fParams;

	public ServletConfigImpl(ServletContext servletContext, String name, Map params) {
		fContext = servletContext;
		fName = name;
		fParams = params;
	}

	public String getInitParameter(String key) {
		return fParams == null ? null : (String)fParams.get(key);
	}

	public Enumeration getInitParameterNames() {
		if(fParams != null) {
			ArrayList lst = new ArrayList();
			lst.addAll(fParams.keySet());
			return Collections.enumeration(lst);
		} else {
			return Collections.enumeration(Collections.EMPTY_LIST);
		}
	}

	public ServletContext getServletContext() {
		return fContext;
	}

	public String getServletName() {
		return fName;
	}

}
