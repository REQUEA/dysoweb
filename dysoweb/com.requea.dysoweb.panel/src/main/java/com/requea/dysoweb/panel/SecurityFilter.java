package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
	public static final String SECURED = "com.requea.dysoweb.panel.secured";
	public static final String AUTHENTICATING = "com.requea.dysoweb.panel,authenticating";
	
	private File fConfigDir;
	
	public void init(FilterConfig config) throws ServletException {
		fConfigDir = SecurityServlet.getConfigDir(config.getServletContext());
	}

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		// check if we are authenticated
		HttpSession session = ((HttpServletRequest)request).getSession();
		String op = request.getParameter("op");
		Object obj = session.getAttribute(AUTH);
		if(Boolean.TRUE.equals(obj) || Boolean.TRUE.equals(request.getAttribute(AUTHENTICATING))) {
			// ok: already authenticated
			chain.doFilter(request, response);
			return;
		}

		request.setAttribute(AUTHENTICATING, Boolean.TRUE);
		if("auth".equals(op)) {
			// ok: already authenticated
			chain.doFilter(request, response);
			return;
		}
		

		// check if already authenticated
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
				// assume platform is secured, but corrupted
				System.err.println("Platform server.xml is corrupted");
				e.printStackTrace();
				bSecured = true;
			}
		}
		if(bSecured) {
			session.setAttribute(SECURED, Boolean.TRUE);
		}
		
		HttpServletRequest req = (HttpServletRequest)request;
		String ru = req.getRequestURI();
		if(!bSecured && "register".equals(op)) {
			// ok to process: platform not secured
			chain.doFilter(request, response);
			return;
		} else if(!"status".equals(op)) {
			request.setAttribute("com.requea.dysoweb.panel.ru", ru);
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

	
	private static final String TMP_DIR = "javax.servlet.context.tempdir";
	public static String getScratchDir(ServletContext context) {
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
