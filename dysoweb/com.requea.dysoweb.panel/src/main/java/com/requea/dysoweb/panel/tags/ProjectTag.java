package com.requea.dysoweb.panel.tags;

import com.requea.dysoweb.panel.InstallManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyTagSupport;


public class ProjectTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of versions
        String project = (String)request.getAttribute(InstallManager.PROJECT);
        String config = (String)request.getAttribute(InstallManager.CONFIG);
		if((project == null || project.length() == 0) && (config == null || config.length() == 0) ) {
			return SKIP_BODY;
		}

		// render the checkbox
        TagWriter tw = new TagWriter();
        tw.append("<div class=\"projectd\">");
        if (project != null && project.length() > 0) {
            tw.append("<span class=\"project\">Project: ");
            tw.append(project);
            tw.append("</span>");
        }
        if (config != null && config.length() > 0) {
            tw.append("<span class=\"config\">Configuration:");
            tw.append(config);
            tw.append("</span>");
        }
        tw.append("</div>");
        tw.writeTo(pageContext);
		
		
		return EVAL_BODY_INCLUDE;
	}

	
}
