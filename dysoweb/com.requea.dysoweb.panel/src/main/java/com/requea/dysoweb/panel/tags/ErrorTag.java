package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public class ErrorTag extends TagSupport {

	private static final long serialVersionUID = 5148255342586348685L;
	public static final String ERROR = "com.requea.error";

	public int doStartTag() throws JspException {

        // get the request
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

        Object error = request.getAttribute(ERROR);
		if(error instanceof Throwable) {
			TagWriter tw = new TagWriter();
			Throwable e = (Throwable)error;
			tw.append("<div class=\"rqerror\">");
			tw.append(e.getMessage());
			tw.append("</div>");
			tw.writeTo(pageContext);
		} else if(error != null) {
			TagWriter tw = new TagWriter();
			tw.append("<div class=\"rqerror\">");
			tw.append(error.toString());
			tw.append("</div>");
			tw.writeTo(pageContext);
		} else {
			// nothing to render
			return SKIP_BODY;
		}

		return super.doStartTag();
	}

}
