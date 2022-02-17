package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.requea.dysoweb.panel.InstallManager;
import com.requea.dysoweb.panel.Installable;
import com.requea.dysoweb.panel.InstallServlet;
import com.requea.dysoweb.panel.utils.Util;


public class InstallableTag extends TagSupport {

	private static final long serialVersionUID = -6946596706261994434L;

	private String fProperty;

	private String fStyle;

	public void setProperty(String property) {
		fProperty = property;
	}
	public void setStyle(String style) {
		fStyle = style;
	}

	public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

        Installable installable = (Installable)request.getAttribute(InstallServlet.INSTALLABLE);
        if(installable == null) {
        	return SKIP_BODY;
        }
        
        if("title".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append(installable.getName());
        	tw.writeTo(pageContext);
        } else if("description".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = installable.getDescription();
        	if(str != null) {
        		str = Util.escapeHTML(str);
		        str = str.replaceAll("\\r\\n", "<br/>");
		        str = str.replaceAll("\\n", "<br/>");
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("documentation".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = installable.getLongDesc();
        	if(str != null) {
        		str = Util.escapeHTML(str);
		        str = str.replaceAll("\\r\\n", "<br/>");
		        str = str.replaceAll("\\n", "<br/>");
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("version".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = installable.getVersion();
        	if(str != null) {
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
	    } else if("infoURL".equals(fProperty)) {
	    	String url = installable.getInfoURL();
	    	if(url == null || url.length() == 0)
	    		return SKIP_BODY;
    	
        
        	TagWriter tw = new TagWriter();
        	tw.append("<a href=\"");
        	tw.append(url);
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append(" target=\"_blank\"");
        	tw.append(">");
        	tw.append("more infos"); // TODO: translate
        	tw.append("</a>");
        	tw.writeTo(pageContext);
	    } else if("launch".equals(fProperty)) {
       	TagWriter tw = new TagWriter();

        	tw.append("<div>");
        	tw.append("<a href=\"");
        	tw.append(request.getContextPath());
			tw.append("/page");
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append(" target=\"_blank\"");
        	tw.append(">");
        	tw.append("Launch "); // TODO: translate
			String config = (String)request.getAttribute(InstallManager.CONFIG);
			if (config != null)
				tw.append(config);
			else
        		tw.append("application");
        	tw.append("</a>");
        	tw.append("<div>");

        	tw.writeTo(pageContext);
        } else if("install".equals(fProperty)) {
        	String bundles = installable.getBundleList();
        	if(bundles == null || bundles.length() == 0) {
        		return SKIP_BODY;
        	}
        	TagWriter tw = new TagWriter();
        	tw.append("<a href=\"");
        	tw.append(request.getContextPath());
        	tw.append("/dysoweb/panel/secure/install?bundles=");
        	tw.append(bundles);
        	tw.append("&feature=");
        	tw.append(installable.getID());
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append("><span>");
        	tw.append("install");
        	tw.append("</span></a>");
        	tw.writeTo(pageContext);
        } else if("check".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append("<input name=\"inst_");
        	tw.append(installable.getID());
        	tw.append("\"");
        	if(installable.isRoot()) {
        		tw.append(" checked=\"checked\"");
        	}
        	tw.append(" type=\"checkbox\" value=\"");
        	tw.append("install");
        	tw.append("\"/>");
        	tw.writeTo(pageContext);
        } else if("image".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String strImage = installable.getImage();
        	if(strImage == null) 
        		return SKIP_BODY;
        	tw.append("<img src=\"");
        	if(strImage.startsWith("http")) {
        		tw.append(strImage);
        	} else {
	        	tw.append(request.getContextPath());
	        	tw.append("/dysoweb/panel/secure/install?op=image&image=");
	        	tw.append(installable.getName()+"-"+installable.getVersion());
        	}
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append("></img>");
        	tw.writeTo(pageContext);
        }
		return super.doStartTag();
	}
	
	
}
