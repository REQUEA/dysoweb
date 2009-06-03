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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.requea.dysoweb.utils.impl.Activator;



public class BundleTag extends TagSupport {

	private static final long serialVersionUID = 1L;
	private String fProperty;
	private String fClass;
	private String fBundle;
	private String fCommand;
	
	public void setProperty(String value) {
		fProperty = value;
	}
	public void setClass(String value) {
		fClass = value;
	}
	public void setBundle(String bundleId) {
		fBundle = bundleId;
	}
	public void setCommand(String command) {
		fCommand = command;
	}


	public int doEndTag() throws JspException {
		return super.doEndTag();
	}

	public int doStartTag() throws JspException {
		// retrieve the bundle
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		Bundle bundle = null;
		if(fBundle != null) {
			bundle = getBundle(fBundle);
		} else {
			bundle = (Bundle)request.getAttribute(TagConstants.BUNDLE);
		}
		if(bundle == null) {
			// no bundle, nothing to render
			return SKIP_BODY;
		}
		
		// render the bundle property
		TagWriter tw = new TagWriter();
		if("command".equals(fProperty)) {
			int state = bundle.getState();
			String op;
			switch(state) {
			case Bundle.ACTIVE:
				op = "stop";
				break;
			case Bundle.INSTALLED:
			case Bundle.RESOLVED:
				op = "start";
				break;
			default:
				return SKIP_BODY;
			}
			tw.append("<a href=\"");
			StringBuffer sb = new StringBuffer();
			sb.append(request.getContextPath());
			sb.append("/");
			if(fCommand == null) {
				sb.append("dwbndl");
			} else {
				sb.append(fCommand);
			}
			sb.append("?bndl=");
			sb.append(Long.toString(bundle.getBundleId()));
			sb.append("&op=");
			sb.append(op);
			HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
			tw.append(response.encodeURL(sb.toString()));
			tw.append("\"");
			if(fClass != null) {
				tw.append(" class=\"");
				tw.append(fClass);
				tw.append("\"");
			}
			tw.append(">");
			tw.append(op);
			tw.append("</a>");
		} else if("stateimg".equals(fProperty)) {
			int state = bundle.getState();
			String str = getStateString(state);
			switch(state) {
			case Bundle.ACTIVE:
				tw.append("<img src=\"");
				tw.append(request.getContextPath());
				tw.append("/dysoweb/img/bndl/green.gif\"");
				tw.append(" alt=\"");
				tw.append(str);
				tw.append("\" />");
				break;
			case Bundle.INSTALLED:
			case Bundle.RESOLVED:
				tw.append("<img src=\"");
				tw.append(request.getContextPath());
				tw.append("/dysoweb/img/bndl/red.gif\"");
				tw.append(" alt=\"");
				tw.append(str);
				tw.append("\" />");
				break;
			case Bundle.STARTING:
			case Bundle.STOPPING:
				tw.append("<img src=\"");
				tw.append(request.getContextPath());
				tw.append("/dysoweb/img/bndl/orange.gif\"");
				tw.append(" alt=\"");
				tw.append(str);
				tw.append("\" />");
				break;
			default:
				return SKIP_BODY;
			}
		} else if("state".equals(fProperty)) {
			String str = getStateString(bundle.getState());
			tw.append(str);
		} else if(fProperty != null) {
			// try a deader property
			Object val = bundle.getHeaders().get(fProperty);
			if(val instanceof String)
				tw.append((String)val);
		}
		
		tw.writeTo(pageContext);
		return EVAL_BODY_INCLUDE;
	}

    public String getStateString(int i)
    {
        if (i == Bundle.ACTIVE)
            return "Active";
        else if (i == Bundle.INSTALLED)
            return "Installed";
        else if (i == Bundle.RESOLVED)
            return "Resolved";
        else if (i == Bundle.STARTING)
            return "Starting";
        else if (i == Bundle.STOPPING)
            return "Stopping";
        return "Unknown";
    }

    public static Bundle getBundle(String bundleId) {
    	if(bundleId == null)
    		return null;
    	Activator act = Activator.getInstance();
    	if(act == null) {
    		return null;
    	}
		BundleContext ctx = act.getContext();
		if(ctx == null) {
			return null;
		}
		
		Bundle[] bundles = ctx.getBundles();
		for(int i=0; i<bundles.length; i++) {
			if(bundleId.equals(bundles[i].getSymbolicName())) {
				return bundles[i];
			}
		}
		// nothing found
		return null;
    }

}
