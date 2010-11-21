package com.requea.dysoweb.shell.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.util.xml.XMLUtils;

public class SecurityFilter implements Filter {

	public static final String AUTH = "com.requea.dysoweb.panel.auth";
	
	public void init(FilterConfig config) throws ServletException {

	}

	public void destroy() {

	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)resp;
		
		// check if we are authenticated
		HttpSession session = request.getSession();
		Object obj = session.getAttribute(AUTH);
		if(Boolean.TRUE.equals(obj)) {
			// ok: already authenticated
			chain.doFilter(request, response);
			return;
		}
		// does not send a response
		try {
			Document doc = XMLUtils.newDocument();
			Element el = doc.createElement("result");
			doc.appendChild(el);

			Element elErr = doc.createElement("div");
			el.appendChild(elErr);
			elErr.setAttribute("class", "err");
			
			ShellServlet.setText(elErr, "For security reasons you have been disconnected");
			
			// ajax response
	        String xml = XMLUtils.ElementToString(el);
	        response.setContentType("text/xml");
	        String encoding = null;
	        if(!"false".equals(System.getProperty("com.requea.dynpage.compressoutput"))) {
	        	encoding = request.getHeader("Accept-Encoding");
	        }
	        boolean supportsGzip = false;
	        if (encoding != null) {
		    	if (encoding.toLowerCase().indexOf("gzip") > -1) {
		    		supportsGzip = true;
	    		}
	    	}
	        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
	        OutputStream os = response.getOutputStream();
	        if(supportsGzip) {
	        	os = new GZIPOutputStream(os);
	        	response.setHeader("Content-Encoding", "gzip");
	        }
	        Writer w = new OutputStreamWriter(os, "UTF-8");
	        w.write(xml);
	        w.close();
		
		} catch (Exception e) {
			throw new ServletException(e);
		}

	}



}
