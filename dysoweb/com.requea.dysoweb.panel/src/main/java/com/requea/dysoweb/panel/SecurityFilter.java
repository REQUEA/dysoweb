package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.util.xml.XMLUtils;

public class SecurityFilter implements Filter {

	public static final String AUTH = "com.requea.dysoweb.panel.auth";
	private File fConfigDir;
	
	public void init(FilterConfig config) throws ServletException {
		fConfigDir = getConfigDir(config.getServletContext());
	}

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		// check if we are authenticated
		HttpSession session = ((HttpServletRequest)request).getSession();
		String op = request.getParameter("op");
		Object obj = session.getAttribute(AUTH);
		if("register".equals(op) || Boolean.TRUE.equals(obj)) {
			// ok
			chain.doFilter(request, response);
			return;
		} else {
			HttpServletRequest req = (HttpServletRequest)request;
			String ru = req.getRequestURI();
			request.setAttribute("com.requea.dysoweb.panel.ru", ru);
			
			// not authenticated yet
			File f = new File(fConfigDir,"server.xml");
			boolean bSecured = false;
			if(f.exists()) {
				try {
					Document doc = XMLUtils.parse(new FileInputStream(f));
					Element el = doc.getDocumentElement();
					String pass = XMLUtils.getChildText(el, "Password");
					if(pass != null && pass.length() > 0) {
						bSecured = true;
					}
				} catch(Exception e) {
					bSecured = false;
				}
				
			}
			if(bSecured) {
				// already registered: just need auth
				RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/auth.jsp");
				rd.forward(request, response);
				return;
			} else {
				// forward to registration
				RequestDispatcher rd = request.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}
		}
	}

	public static File getConfigDir(ServletContext servletContext) {
		File dir = null;
		try {
			InitialContext ic = new InitialContext();
			Context nc = (Context) ic.lookup("java:comp/env");
			dir = new File((String) (nc.lookup("dysoweb.home")),"config");
		} catch (NamingException nex) {
			// unable to lookup the requea configuration file
			dir = new File(getScratchDir(servletContext),"config");
		}
		return dir;
	}
	
	private static final String TMP_DIR = "javax.servlet.context.tempdir";
	private static String getScratchDir(ServletContext context) {
		// First try the Servlet 2.2 javax.servlet.context.tempdir property
		File scratchDir = (File) context.getAttribute(TMP_DIR);
		if (scratchDir == null) {
			// Not running in a Servlet 2.2 container.
			// Try to get the JDK 1.2 java.io.tmpdir property
			String dir = System.getProperty("java.io.tmpdir");
			return dir;
		} else {
			return scratchDir.getAbsolutePath();
		}
	}

	

}
