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

package com.requea.dysoweb.utils.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class MenuTag extends TagSupport {

	private static final long serialVersionUID = 1L;
	private String fPath;
	private String fLabel;

	public void setPath(String path) {
		fPath = path;
	}

	public void setLabel(String lbl) {
		fLabel = lbl;
	}

	public int doStartTag() throws JspException {

		TagWriter tw = new TagWriter();
		if(fPath == null || fLabel == null) {
			return SKIP_BODY;
		}
		
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		String uri = request.getRequestURI();
		String path = request.getContextPath()+fPath;
		if(uri.startsWith(path)) {
			// current menu
			tw.append("<span class=\"current\">");
			tw.append(fLabel);
			tw.append("</span>");
		} else {
			// not the current one
			tw.append("<a href=\"");
			tw.append(path);
			tw.append("\">");
			tw.append(fLabel);
			tw.append("</a>");
		}
		tw.writeTo(pageContext);
		return super.doStartTag();
	}
}
