package com.requea.dysoweb.panel.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.requea.dysoweb.panel.InstallManager;
import com.requea.dysoweb.panel.InstallServlet;


public class VersionsTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
		// retrieve the list of versions
		String[] versions = (String[])request.getAttribute(InstallManager.VERSIONS);
		if(versions == null || versions.length == 0) {
			return SKIP_BODY;
		}

		String currentVersion = (String) request.getAttribute(InstallManager.CURRENTVERSION);

		// render the list of versions
        TagWriter tw = new TagWriter();
        tw.append("<span>versions: </span>");
        tw.append("<select name=\"ver\"");
        tw.append(" onchange=\"window.location='");
        tw.append(request.getContextPath());
        tw.append("/dysoweb/panel/secure/install?refresh=true&ver='+this.value\"");
        tw.append(">");
        // no version
        tw.append("<option value=\"base\"");
        if(currentVersion == null || currentVersion.equals("") || "base".equals(currentVersion)) {
            tw.append(" selected=\"selected\"");
        }
        tw.append(">Base version</option>");
        
        for(int i=0; i<versions.length; i++) {
            tw.append("<option value=\"");
            String ver = versions[i];
            tw.append(ver);
            tw.append("\"");
            if(ver.equals(currentVersion)) {
                tw.append(" selected=\"selected\"");
            }
            tw.append(">");
            try {
                tw.append(URLEncoder.encode(ver, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
            tw.append("</option>");
        }
        tw.append("</select>");
        tw.writeTo(pageContext);
		
		
		return EVAL_BODY_INCLUDE;
	}

	
}
