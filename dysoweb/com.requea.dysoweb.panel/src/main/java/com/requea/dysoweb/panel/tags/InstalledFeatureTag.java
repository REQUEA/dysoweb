package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.w3c.dom.Element;

import com.requea.dysoweb.panel.InstallServlet;
import com.requea.dysoweb.panel.utils.xml.XMLUtils;

public class InstalledFeatureTag extends TagSupport {

	private static final long serialVersionUID = -41581024145550944L;

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

        Element el = (Element)request.getAttribute(InstallServlet.INSTALLEDFEATURE);
        if(el == null) {
        	return SKIP_BODY;
        }

        if("date".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
			String strDate = el.getAttribute("date");
			tw.append(strDate);
        	tw.writeTo(pageContext);
        } else if("description".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = XMLUtils.getChildText(el, "description");
        	if(str != null) {
		        str = str.replaceAll("\\r\\n", "<br/>");
		        str = str.replaceAll("\\n", "<br/>");
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("title".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = XMLUtils.getChildText(el, "title");
        	if(str != null) {
		        str = str.replaceAll("\\r\\n", "<br/>");
		        str = str.replaceAll("\\n", "<br/>");
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("id".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = el.getAttribute("id");
        	if(str != null) {
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("version".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = el.getAttribute("version");
        	if(str != null) {
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("longDesc".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = XMLUtils.getChildText(el, "longDesc");
        	if(str != null) {
		        str = str.replaceAll("\\r\\n", "<br/>");
		        str = str.replaceAll("\\n", "<br/>");
	        	tw.append(str);
        	}
        	tw.writeTo(pageContext);
        } else if("image".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String strImage = XMLUtils.getChildText(el, "image");
        	if(strImage != null) {
            	tw.append("<img src=\"");
            	tw.append(request.getContextPath());
            	tw.append("/dysoweb/panel/install?op=image&image=");
            	tw.append(strImage);
            	tw.append("\"");
            	if(fStyle != null) {
            		tw.append(" class=\"");
            		tw.append(fStyle);
            		tw.append("\"");
            	}
            	tw.append("></img>");
            	tw.writeTo(pageContext);
        		
        	} else {
        		return SKIP_BODY;
        	}
        }
        
    	return SKIP_BODY;
	}
	
}
