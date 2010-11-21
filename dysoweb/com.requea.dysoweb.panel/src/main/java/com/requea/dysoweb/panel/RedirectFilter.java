package com.requea.dysoweb.panel;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RedirectFilter implements Filter {

	
	public void init(FilterConfig config) throws ServletException {
	}

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		String uri = ((HttpServletRequest)request).getRequestURI();
		
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		
		if(uri.startsWith(httpRequest.getContextPath()+"/dysoweb/panel/panel.jsp")) {
			httpResponse.sendRedirect(httpRequest.getContextPath()+"/dysoweb/panel/secure/bundles.jsp");
			return;
		} else if(uri.equals(httpRequest.getContextPath()+"/dysoweb")) {
			httpResponse.sendRedirect(httpRequest.getContextPath()+"/dysoweb/panel/secure/bundles.jsp");
			return;
		}
		
		chain.doFilter(request, response);

	}


}
