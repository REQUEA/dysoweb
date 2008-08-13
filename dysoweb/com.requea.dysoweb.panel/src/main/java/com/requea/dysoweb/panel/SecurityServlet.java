package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.Base64;
import com.requea.dysoweb.util.xml.XMLUtils;

public class SecurityServlet extends HttpServlet {

	private static final long serialVersionUID = 3754555117889929726L;
	private File fConfigDir;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityFilter.getConfigDir(getServletContext());
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
			request.setAttribute("com.requea.dysoweb.panel.ru", ru == null ? ""
					: ru);

			// get the config element
			Element elConfig = null;
			try {
				elConfig = getServerConfig();
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
			request.getSession()
					.setAttribute(SecurityFilter.AUTH, Boolean.TRUE);
			request.getSession().setAttribute("com.requea.dysoweb.shell.auth", Boolean.TRUE);
			
			// auth was ok, redirect
			if (ru != null && ru.length() > 0 && !"null".equals(ru)) {
				response.sendRedirect(ru);
			} else {
				response.sendRedirect(request.getContextPath()
						+ "/dysoweb/panel/panel.jsp");
			}
		}
	}
	

	private Element getServerConfig() throws Exception {

		File f = new File(fConfigDir, "server.xml");
		if (!f.exists()) {
			return null;
		}

		// parse the file
		Document doc = XMLUtils.parse(new FileInputStream(f));
		return doc.getDocumentElement();
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
		String hash = Base64.encode(raw); // step 5
		return hash; // step 6
	}


}
