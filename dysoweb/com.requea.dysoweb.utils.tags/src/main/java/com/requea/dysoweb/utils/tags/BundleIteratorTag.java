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

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.requea.dysoweb.utils.impl.Activator;

public class BundleIteratorTag extends BodyTagSupport {

	private static final long serialVersionUID = 1L;
	private Bundle[] fBundles;
	private int fIndex;
	private String fProperty;
	
	public void setProperty(String property) {
		fProperty = property;
	}

	public int doEndTag() throws JspException {
		// reset local variables to have them GCed
		fBundles = null;
		return super.doEndTag();
	}

	public int doStartTag() throws JspException {
		
		if("oddeven".equals(fProperty)) {
			// get the parent iterator
			BundleIteratorTag parent = (BundleIteratorTag)TagUtils.getParentTag(this, BundleIteratorTag.class);
			if(parent != null) {
				TagWriter tw = new TagWriter();
				tw.append(parent.getIndex() % 2 == 0 ? "odd" : "even");
				tw.writeTo(pageContext);
			}
			return SKIP_BODY;
		} else if(fProperty == null || "iterator".equals(fProperty)) {
			// regular iterator
			// retrieve the list of bundles
	    	Activator act = Activator.getInstance();
	    	if(act == null) {
	    		return SKIP_BODY;
	    	}
			BundleContext ctx = act.getContext();
			if(ctx == null) {
				return SKIP_BODY;
			}
			
			Bundle[] bundles = ctx.getBundles();
			fBundles = bundles;
			fIndex = 0;
			
			if(fIndex < fBundles.length) {
				ServletRequest request = pageContext.getRequest();
				request.setAttribute(TagConstants.BUNDLE, fBundles[fIndex]);
				return EVAL_BODY_INCLUDE;
			} else {
				return SKIP_BODY;
			}
		} else {
			return SKIP_BODY;
		}
	}

	public int doAfterBody() throws JspException {
		fIndex ++;
		if(fIndex < fBundles.length) {
			ServletRequest request = pageContext.getRequest();
			request.setAttribute(TagConstants.BUNDLE, fBundles[fIndex]);
			return EVAL_BODY_AGAIN;
		} else {
			return SKIP_BODY;
		}
	}
	
	public int getIndex() {
		return fIndex;
	}

}
