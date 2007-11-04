package com.requea.dysoweb.panel.tags;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.requea.dysoweb.panel.Feature;
import com.requea.dysoweb.panel.InstallServlet;


public class FeaturesTag extends BodyTagSupport {

	private static final long serialVersionUID = -6421694776111708347L;
	private Feature[] fFeatures;
	private int fIndex;

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of features
		Feature[] features = (Feature[])request.getAttribute(InstallServlet.FEATURES);
		if(features == null) {
			return SKIP_BODY;
		}
		
		String cat = request.getParameter("category");
		if(cat != null && cat.length() > 0) {
			List lst = new ArrayList();
			for(int i=0; i<features.length; i++) {
				Feature f = features[i];
				if(f.isInCategory(cat)) {
					lst.add(f);
				}
			}
			fFeatures = (Feature[])lst.toArray(new Feature[lst.size()]);
		} else {
			fFeatures = features;
		}
		fIndex = 0;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.FEATURE, fFeatures[fIndex]);
			return EVAL_BODY_INCLUDE;
		} else {			
			return SKIP_BODY;
		}
		
	}

	public int doAfterBody() throws JspException {

        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		fIndex ++;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.FEATURE, fFeatures[fIndex]);
			return EVAL_BODY_AGAIN;
		} else {			
			return SKIP_BODY;
		}
		
	}

	
}
