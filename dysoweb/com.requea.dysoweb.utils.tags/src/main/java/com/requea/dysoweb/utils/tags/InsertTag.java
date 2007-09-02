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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.osgi.framework.Bundle;


public class InsertTag extends BodyTagSupport {

	private static final long serialVersionUID = 1L;
	private String fPath;
	
	private String fBundle;
	

	public void setPath(String path) {
		fPath = path;
	}
	
	public void setBundle(String bundle) {
		fBundle = bundle;
	}
	
	public int doStartTag() throws JspException {
		if(fBundle != null) {
			// check if the bundle is active
			Bundle bundle = BundleTag.getBundle(fBundle);
			if(bundle == null || bundle.getState() != Bundle.ACTIVE) {
				return SKIP_BODY;
			}
		}
		
        if(fPath != null) {
            try {
                pageContext.include(fPath);
            } catch (Exception e) {
                throw new JspException(e);
            }
        }
        return EVAL_BODY_INCLUDE;
	}


}
