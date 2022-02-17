package com.requea.dysoweb.panel;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Element;

import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.Base64;
import com.requea.dysoweb.util.xml.XMLUtils;
import com.requea.webenv.IWebProcessor;

public class SecurityServlet extends HttpServlet {

	private static final long serialVersionUID = 3754555117889929726L;
	private File fConfigDir;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = getConfigDir(getServletContext());
		fConfigDir.mkdirs();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		process(req, resp);
	}

	private void process(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// get the op
		String op = request.getParameter("op");

		if ("auth".equals(op)) {

			String ru = request.getParameter("ru");
			// check host name
			request.setAttribute("com.requea.dysoweb.panel.ru", ru == null ? ""
					: ru);

			// get the config element
			Element elConfig = null;
			try {
				elConfig = InstallManager.getServerConfig(fConfigDir);
			} catch (Exception e) {
				// show the error
				request.setAttribute(ErrorTag.ERROR,
						"Unable to retrieve server config: " + e.getMessage());
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/auth.jsp");
				rd.forward(request, response);
				return;
			}
			if (elConfig == null) {
				// not registered!
				request.setAttribute(ErrorTag.ERROR, "Server not registered");
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/auth.jsp");
				rd.forward(request, response);
				return;
			}

			// retrieve the password and compare
			String pass = request.getParameter("Password");
			// compare encrypted versions
			try {
				if(pass != null) {
					pass = encrypt(pass);
				}
			} catch(Exception e) {
				pass = null;
			}
			if (pass == null
					|| !pass.equals(XMLUtils.getChildText(elConfig, "Password"))) {
				// not registered!
				request.setAttribute(ErrorTag.ERROR, "Incorrect password");
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/auth.jsp");
				rd.forward(request, response);
				return;
			}
			
			// ok, authenticated
			HttpSession session = request.getSession();
			session.setAttribute(SecurityFilter.AUTH, Boolean.TRUE);
			session.setAttribute(SecurityFilter.SECURED, Boolean.TRUE);
			session.setAttribute("com.requea.dysoweb.shell.auth", Boolean.TRUE);
			
			// auth was ok, redirect
			if (ru != null && ru.length() > 0 && !"null".equals(ru)) {
				response.sendRedirect(response.encodeURL(ru));
			} else {
				response.sendRedirect(response.encodeURL(request.getContextPath()
						+ "/dysoweb/panel/bundle.jsp"));
			}
		}
	}
	

	public static class RegistrationException extends Exception {
		private static final long serialVersionUID = -3340567087940905530L;
		public RegistrationException(String msg) {
			super(msg);
		}
		public RegistrationException(Exception e) {
			super(e);
		}
	}
	
	static String encrypt(String plaintext)
			throws RegistrationException {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA"); // step 2
		} catch (NoSuchAlgorithmException e) {
			throw new RegistrationException(e.getMessage());
		}
		try {
			md.update(plaintext.getBytes("UTF-8")); // step 3
		} catch (UnsupportedEncodingException e) {
			throw new RegistrationException(e.getMessage());
		}

		byte raw[] = md.digest(); // step 4
		String hash = Base64.encodeBytes(raw); // step 5
		return hash; // step 6
	}

	public static File getConfigDir(ServletContext servletContext) {
		File dir = null;
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(IWebProcessor.class.getClassLoader());
			InitialContext ic = new InitialContext();
			Context nc = (Context) ic.lookup("java:comp/env");
			String home = (String) nc.lookup("dysoweb.home");
			File basedir = null;
			if("dysoweb.home".equals(home)) {
				if(System.getProperty("jboss.home.dir") != null) {
					basedir = new File(new File(System.getProperty("jboss.home.dir")), "dysoweb.home");
				} else if(System.getProperty("catalina.base") != null) {
					basedir = new File(new File(System.getProperty("catalina.base")), "dysoweb.home");
				} else if(System.getProperty("catalina.home") != null) {
					basedir = new File(new File(System.getProperty("catalina.home")), "dysoweb.home");
				} else if(System.getProperty("jonas.base") != null) {
					basedir = new File(new File(System.getProperty("jonas.base")), "dysoweb.home");
				} else if(System.getProperty("weblogic.home") != null && System.getenv("DOMAIN_HOME") != null) {
					basedir = new File(new File(System.getenv("DOMAIN_HOME")), "dysoweb.home");
				}
			}
			if(basedir == null) {
				// use default value
				basedir = new File(home);
			}
			basedir.mkdirs();
			dir = new File(basedir,"config");
		} catch (NamingException nex) {
			// unable to lookup the requea configuration file
			dir = new File(SecurityFilter.getScratchDir(servletContext),"config");
		} finally {
			th.setContextClassLoader(cl);
		}
		return dir;
	}

	public static File getBinDir(ServletContext servletContext) {
		File dir = null;
		Thread th = Thread.currentThread();
		ClassLoader cl = th.getContextClassLoader();
		try {
			th.setContextClassLoader(IWebProcessor.class.getClassLoader());
			File basedir = null;
			if(System.getProperty("jboss.home.dir") != null) {
				basedir = new File(System.getProperty("jboss.home.dir"), "bin");
			} else if(System.getProperty("catalina.base") != null) {
				basedir = new File(System.getProperty("catalina.base"), "bin");
			} else if(System.getProperty("catalina.home") != null) {
				basedir = new File(System.getProperty("catalina.home"), "bin");
			} else if(System.getProperty("jonas.base") != null) {
				basedir = new File(System.getProperty("jonas.base"), "bin");
			} else if(System.getProperty("weblogic.home") != null && System.getenv("DOMAIN_HOME") != null) {
				basedir = new File(System.getenv("DOMAIN_HOME"), "bin");
			}
			return basedir;
		} catch (Exception nex) {
		} finally {
			th.setContextClassLoader(cl);
		}
		return null;
	}


}
