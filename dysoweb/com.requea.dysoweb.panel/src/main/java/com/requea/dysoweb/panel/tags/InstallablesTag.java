package com.requea.dysoweb.panel.tags;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.requea.dysoweb.panel.InstallManager;
import com.requea.dysoweb.panel.Installable;
import com.requea.dysoweb.panel.InstallServlet;


public class InstallablesTag extends BodyTagSupport {

	private static final long serialVersionUID = -6421694776111708347L;
	private Installable[] fFeatures;
	private int fIndex;
	private String fType;
	

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of features
		Installable[] installables = (Installable[])request.getAttribute(InstallManager.INSTALLABLES);
		if(installables == null) {
			return SKIP_BODY;
		}

		String cat = request.getParameter("category");

		List lst = new ArrayList();
		for(int i=0; i<installables.length; i++) {
			Installable f = installables[i];
			// in the path?
			if(fType == null || 
					("products".equals(fType) && ("product".equals(f.getType()) || "feature".equals(f.getType()))) || 
					("rootbundles".equals(fType) && "bundle".equals(f.getType()) && f.isRoot()) ||
					("otherbundles".equals(fType) && "bundle".equals(f.getType()) && !f.isRoot()) ||
					("bundles".equals(fType) && "bundle".equals(f.getType()))) {
				if(cat == null || f.isInCategory(cat)) {
					lst.add(f);
				}
			}
		}
		fFeatures = (Installable[])lst.toArray(new Installable[lst.size()]);
		fIndex = 0;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.INSTALLABLE, fFeatures[fIndex]);
			return EVAL_BODY_INCLUDE;
		} else {			
			return SKIP_BODY;
		}
		
	}

	public void setType(String type) {
		fType = type;
	}

	public int doAfterBody() throws JspException {

        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		fIndex ++;
		if(fIndex < fFeatures.length) {
			request.setAttribute(InstallServlet.INSTALLABLE, fFeatures[fIndex]);
			return EVAL_BODY_AGAIN;
		} else {			
			return SKIP_BODY;
		}
		
	}

	
}
