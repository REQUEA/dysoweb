package com.requea.dysoweb.panel.tags;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.requea.dysoweb.panel.InstallManager;
import com.requea.dysoweb.panel.Installable;
import com.requea.dysoweb.panel.InstallServlet;


public class CategoriesTag extends BodyTagSupport {

	private static final long serialVersionUID = -6421694776111708347L;
	private int fIndex;
	private List fIds;
	private List fLabels;

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of features
		Map categories = (Map)request.getAttribute(InstallManager.CATEGORIES);
		if(categories == null) {
			return SKIP_BODY;
		}
		List lstIds = new ArrayList();
		List lstLabels = new ArrayList();
		
		Iterator iter = categories.values().iterator();
		while(iter.hasNext()) {
			Installable.Category c = (Installable.Category)iter.next();
			if(!lstIds.contains(c.fId)) {
				lstIds.add(c.fId);
				lstLabels.add(c.fLabel);
			}
		}
 		fIds = lstIds;
 		fLabels = lstLabels;
		fIndex = 0;
		
		if(fIndex < fIds.size()) {
			request.setAttribute("com.requea.category.id", fIds.get(fIndex));
			request.setAttribute("com.requea.category.label", fLabels.get(fIndex));
			return EVAL_BODY_INCLUDE;
		} else {			
			return SKIP_BODY;
		}
		
	}

	public int doAfterBody() throws JspException {

        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		fIndex ++;

		if(fIndex < fIds.size()) {
			request.setAttribute("com.requea.category.id", fIds.get(fIndex));
			request.setAttribute("com.requea.category.label", fLabels.get(fIndex));
			return EVAL_BODY_AGAIN;
		} else {			
			return SKIP_BODY;
		}
	}

	
}
