package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class CategoryTag extends TagSupport {

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

        String id = (String)request.getAttribute("com.requea.category.id");
        String label = (String)request.getAttribute("com.requea.category.label");
        if(id == null || label == null) {
        	return SKIP_BODY;
        }
        
        if("label".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append(label);
        	tw.writeTo(pageContext);
        } else if("id".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append(id);
        	tw.writeTo(pageContext);
	    } else if("link".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append("<a href=\"");
        	tw.append(request.getContextPath());
        	tw.append("/dysoweb/panel/secure/install?category=");
        	tw.append(id);
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append(">");
        	tw.append(label);
        	tw.append("</a>");
        	tw.writeTo(pageContext);
        }

		return super.doStartTag();
	}
	
	
}
