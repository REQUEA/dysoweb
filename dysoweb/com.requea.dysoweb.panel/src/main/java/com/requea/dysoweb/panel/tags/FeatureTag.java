package com.requea.dysoweb.panel.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.w3c.dom.Element;

import com.requea.dysoweb.panel.Feature;
import com.requea.dysoweb.panel.InstallServlet;


public class FeatureTag extends TagSupport {

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

        Feature feature = (Feature)request.getAttribute(InstallServlet.FEATURE);
        if(feature == null) {
        	return SKIP_BODY;
        }
        
        if("title".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append(feature.getName());
        	tw.writeTo(pageContext);
        } else if("description".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = feature.getDescription();
	        str = str.replaceAll("\\r\\n", "<br/>");
	        str = str.replaceAll("\\n", "<br/>");
        	tw.append(str);
        	tw.writeTo(pageContext);
        } else if("longDesc".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	String str = feature.getLongDesc();
	        str = str.replaceAll("\\r\\n", "<br/>");
	        str = str.replaceAll("\\n", "<br/>");
        	tw.append(str);
        	tw.writeTo(pageContext);
	    } else if("infoURL".equals(fProperty)) {
	    	String url = feature.getInfoURL();
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
	    	String url = feature.getBaseURL();
        	TagWriter tw = new TagWriter();
	    	if(url == null || url.length() == 0) {
	    		tw.append("Application ");
	    		tw.append(feature.getName());
	    		tw.append(" installed");
	    	} else {
	        	tw.append("<a href=\"");
	        	tw.append(request.getContextPath());
	        	tw.append(url);
	        	tw.append("\"");
	        	if(fStyle != null) {
	        		tw.append(" class=\"");
	        		tw.append(fStyle);
	        		tw.append("\"");
	        	}
	        	tw.append(" target=\"_blank\"");
	        	tw.append(">");
	        	tw.append("Launch "); // TODO: translate
	        	tw.append(feature.getName());
	        	tw.append("</a>");
	    	}
        	tw.writeTo(pageContext);
        } else if("install".equals(fProperty)) {
        	String bundles = feature.getBundleList();
        	if(bundles == null || bundles.length() == 0) {
        		return SKIP_BODY;
        	}
        	TagWriter tw = new TagWriter();
        	tw.append("<a href=\"");
        	tw.append(request.getContextPath());
        	tw.append("/dysoweb/panel/install?bundles=");
        	tw.append(bundles);
        	tw.append("&feature=");
        	tw.append(feature.getID());
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
        } else if("image".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	Element elImage = feature.getImage();
        	if(elImage == null) 
        		return SKIP_BODY;
        	tw.append("<img src=\"");
        	tw.append(elImage.getAttribute("url"));
        	tw.append("\"");
        	String width = elImage.getAttribute("width");
        	if(width != null && width.length() > 0) {
        		tw.append(" width=\"");
        		tw.append(width);
        		tw.append("\"");
        	}
        	String height = elImage.getAttribute("height");
        	if(height != null && height.length() > 0) {
        		tw.append(" height=\"");
        		tw.append(height);
        		tw.append("\"");
        	}
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append("></img>");
        	tw.writeTo(pageContext);
        } else if("link".equals(fProperty)) {
        	TagWriter tw = new TagWriter();
        	tw.append("<a href=\"");
        	tw.append(request.getContextPath());
        	tw.append("/dysoweb/panel/install?feature=");
        	tw.append(feature.getID());
        	tw.append("\"");
        	if(fStyle != null) {
        		tw.append(" class=\"");
        		tw.append(fStyle);
        		tw.append("\"");
        	}
        	tw.append(">");
        	tw.append("<div");
        	tw.append(">");
        	Element elImage = feature.getImage();
        	if(elImage == null) {
        		tw.append(feature.getName());
        	} else {
	        	tw.append("<img src=\"");
	        	tw.append(elImage.getAttribute("url"));
	        	tw.append("\"");
	        	String width = elImage.getAttribute("width");
	        	if(width != null && width.length() > 0) {
	        		tw.append(" width=\"");
	        		tw.append(width);
	        		tw.append("\"");
	        	}
	        	String height = elImage.getAttribute("height");
	        	if(height != null && height.length() > 0) {
	        		tw.append(" height=\"");
	        		tw.append(height);
	        		tw.append("\"");
	        	}
	        	tw.append(" alt=\"");
	        	tw.append(feature.getName());
	        	tw.append("\"");
	        	tw.append("></img>");
        	}
        	tw.append("</div></a>");
        	tw.writeTo(pageContext);
        }

		return super.doStartTag();
	}
	
	
}
