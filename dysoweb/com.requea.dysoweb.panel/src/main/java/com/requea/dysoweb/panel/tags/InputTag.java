package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class InputTag extends BodyTagSupport {

	private static final long serialVersionUID = 2639623088980168887L;

	public static final String INFO = "com.requea.dysoweb.info";

	private String fType;
	
	private String fName;
	
	private String fStyle;

	private String fSize;

	public void setType(String type) {
		fType = type;
	}

	public void setName(String name) {
		fName = name;
	}

	public void setStyle(String style) {
		fStyle = style;
	}

	public void setSize(String size) {
		fSize = size;
	}
	
	public int doStartTag() throws JspException {
		
        // get the request
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        String value = null;
        
        if(fName != null) {
        	value = request.getParameter(fName);
        }
        // if value null, get value from session
        if(value == null) {
    		value = getValueFromSession(request.getSession(), fName);
        }
        if(value == null) {
        	value = "";
        }
		
        
        TagWriter tw = new TagWriter();
        if("info".equals(fName)) {
        	String info = (String)request.getAttribute(INFO);
        	if(info != null) {
        		tw.append("<div");
        		if(fStyle != null) {
        			tw.append(" class=\"");
        			tw.append(fStyle);
        			tw.append("\"");
        		}
        		tw.append(">");
        		tw.append(info);
        		tw.append("</div>");
        	}
        } else if("ProxySettings".equals(fName)) {
	        try {
				if(!"manual".equals(request.getParameter("Proxy"))) {
					return SKIP_BODY;
				} else {
					return EVAL_BODY_INCLUDE;
				}
			} catch (Exception e) {
				return SKIP_BODY;
			}
        } else if("Proxy".equals(fName)) {
	        tw.append("<input name=\"");
        	tw.append(fName);
        	tw.append("\" onclick=\"pb(this)\"");
	        if(fStyle != null) {
	        	tw.append(" class=\"");
	        	tw.append(fStyle);
	        	tw.append("\"");
	        }
	        tw.append(" type=\"radio\" value=\"auto\"");
	        if("auto".equals(value)) {
	        	tw.append(" checked=\"checked\"");
	        }
	        tw.append(">Automatic</input>");

	        tw.append("<input name=\"");
        	tw.append(fName);
        	tw.append("\" onclick=\"pb(this)\"");
	        if(fStyle != null) {
	        	tw.append(" class=\"");
	        	tw.append(fStyle);
	        	tw.append("\"");
	        }
	        tw.append(" type=\"radio\" value=\"manual\"");
	        if("manual".equals(value)) {
	        	tw.append(" checked=\"checked\"");
	        }
	        tw.append(">Manual</input>");
	        
        } else {
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
	        if(fSize != null) {
	        	tw.append(" size=\"");
	        	tw.append(fSize);
	        	tw.append("\"");
	        }
	        
	        // retrieve the value from the request param
	        if(fType == null || "password".equals(fType)) {
		    	tw.append(" value=\"");
		    	tw.append(value);
		    	tw.append("\"");
	        } else if("checkbox".equals(fType)) {
	        	if(fName != null) {
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
        }
        
        tw.writeTo(pageContext);
		return EVAL_BODY_INCLUDE;
	}

	private String getValueFromSession(HttpSession session, String name) {
		return (String)session.getAttribute("com.requea.dysoweb.panel."+name);
	}

	public int doEndTag() throws JspException {
        TagWriter tw = new TagWriter();
    	tw.append("</input>");
        tw.writeTo(pageContext);
        
		return super.doEndTag();
	}
	
}
