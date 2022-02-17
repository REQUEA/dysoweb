package com.requea.dysoweb.panel.tags;

import com.requea.dysoweb.panel.InstallManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;


public class RestartTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of versions
		Boolean restart = (Boolean)request.getAttribute(InstallManager.RESTART_OPTION);
		if(!Boolean.TRUE.equals(restart)) {
			return SKIP_BODY;
		}

		// render the checkbox
        TagWriter tw = new TagWriter();
        tw.append("<div class=\"restart\">");
        tw.append("<input name=\"restart_option\" id=\"restart_option\" checked=\"checked\" type=\"checkbox\" value=\"true\">");
        tw.append("<label for=\"restart_option\">Restart application server after installation</label>");
        tw.append("</div>");
        tw.writeTo(pageContext);
		
		
		return EVAL_BODY_INCLUDE;
	}

	
}
