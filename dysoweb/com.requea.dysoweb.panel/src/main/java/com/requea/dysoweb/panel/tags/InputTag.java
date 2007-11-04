package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class InputTag extends BodyTagSupport {

	private static final long serialVersionUID = 2639623088980168887L;

	private String fType;
	
	private String fName;
	
	private String fStyle;

	public void setType(String type) {
		fType = type;
	}

	public void setName(String name) {
		fName = name;
	}

	public void setStyle(String style) {
		fStyle = style;
	}

	public int doStartTag() throws JspException {
		
        // get the request
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		
        TagWriter tw = new TagWriter();
        
        tw.append("<input");
        if(fName != null) {
        	tw.append(" name=\"");
        	tw.append(fName);
        	tw.append("\"");
        }
        if(fStyle != null) {
        	tw.append(" class=\"");
        	tw.append(fStyle);
        	tw.append("\"");
        }
        if(fType != null) {
        	tw.append(" type=\"");
        	tw.append(fType);
        	tw.append("\"");
        }
        // retrieve the value 
        if(fType == null || "password".equals(fType)) {
	        String value = null;
	        if(fName != null) {
	        	value = request.getParameter(fName);
	        }
	        if(value == null) {
	        	value = "";
	        }
	    	tw.append(" value=\"");
	    	tw.append(value);
	    	tw.append("\"");
        } else if("checkbox".equals(fType)) {
        	if(fName != null) {
	        	String value = request.getParameter(fName);
	        	if("on".equals(value)) {
		    		tw.append(" checked=\"");
		    		tw.append("checked");
		    		tw.append("\"");
	        	}
        	}
    	}
    	if(fType == null) {
	    	tw.append(" size=\"30\"");
    	}
    	
    	tw.append(">");
        tw.writeTo(pageContext);
		return EVAL_BODY_INCLUDE;
	}

	public int doEndTag() throws JspException {
        TagWriter tw = new TagWriter();
    	tw.append("</input>");
        tw.writeTo(pageContext);
        
		return super.doEndTag();
	}
	
}
