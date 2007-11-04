package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.SecurityFilter;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.utils.xml.Base64;
import com.requea.dysoweb.panel.utils.xml.XMLUtils;

public class SecurityServlet extends HttpServlet {

	private static final long serialVersionUID = 3754555117889929726L;
	public static final String DEFAULT_REPO = "https://repo.requea.com/dysoweb/repo";
	private File fConfigDir;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityFilter.getConfigDir(getServletContext());
		fConfigDir.mkdirs();

		// check if server certificate is there
		File file = new File(fConfigDir, "dysoweb.p12");
		if(!file.exists()) {
			try {
				updateServerRegistration(fConfigDir, null, null, null);
			} catch(Exception e) {
				// ignore
			}
		}
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

		if ("register".equals(op)) {

			String ru = request.getParameter("ru");
			request.setAttribute("com.requea.dysoweb.panel.ru", ru == null ? ""
					: ru);

			if (!checkParameters(request, response)) {
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}

			// register the server and goes back to callers page
			try {
				String contactName = request.getParameter("ContactName");
				String contactEMail = request.getParameter("ContactEMail");
				String strPassword = request.getParameter("Password");
				updateServerRegistration(fConfigDir, contactName, contactEMail, strPassword);
				// from this point on, we are authenticated
				request.getSession().setAttribute(SecurityFilter.AUTH,
						Boolean.TRUE);
				request.getSession().setAttribute("com.requea.dysoweb.shell.auth", Boolean.TRUE);
			} catch (Exception e) {
				request.setAttribute(ErrorTag.ERROR,
						"Unable to register server: " + e.getMessage());
				// re forward to registration for correction of errors
				RequestDispatcher rd = request
						.getRequestDispatcher("/dysoweb/panel/secure.jsp");
				rd.forward(request, response);
				return;
			}

			// registration was ok, redirect
			if (ru != null && ru.length() > 0 && !"null".equals(ru)) {
				response.sendRedirect(ru);
				return;
			} else {
				response.sendRedirect(request.getContextPath()
						+ "/dysoweb/panel/panel.jsp");
			}
		} else if ("auth".equals(op)) {

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
			
			// check if the server was registered?
			String sysId = elConfig.getAttribute("sysId");
			File fileCert = new File(fConfigDir, "dysoweb.p12");
			if(sysId == null || sysId.length() == 0 || !fileCert.exists()) {
				try {
					updateServerRegistration(fConfigDir,
							XMLUtils.getChildText(elConfig, "ContactName"), 
							XMLUtils.getChildText(elConfig, "ContactEMail"),
							pass);
				} catch(Exception e) {
					// never mind, will try later
					
				}
			}
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
	
	public static void updateServerRegistration(File configDir, String contactName, String contactEMail, String strPassword) throws Exception {

		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		
		String strURL = System.getProperty("com.requea.dysoweb.repo");
		if (strURL == null) {
			strURL = DEFAULT_REPO;
		}
		if (!strURL.endsWith("/")) {
			strURL += "/";
		}
		
		// check if there is a certificate?
		File fileCert = new File(configDir, "dysoweb.p12"); 
		if(fileCert.exists()) {
			strURL += "contents/update";
		} else {
			strURL += "init";
		}
		
		Element elServer = null;
		String sysId = null;

		try {
			URL url = new URL(strURL);
			URLConnection connection = url.openConnection();
	        if(fileCert.exists() && connection instanceof HttpsURLConnection) {
	        	SSLSocketFactory sslSocketFactory = InstallServlet.initSocketFactory(configDir);
	        	if(sslSocketFactory != null) {
	        		HttpsURLConnection secureCon = (HttpsURLConnection) connection;
	                secureCon.setSSLSocketFactory(sslSocketFactory);
	        	}
	        }
			
			connection.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(
	                connection.getOutputStream());
			
			// send name and some patform info for support and OBR repository
			// selection
			String name = null;
	    	try {
				name = InetAddress.getLocalHost().getHostName();
			} catch (Throwable e) {
				// ignore
				name = "unknown";
	        }
	
			out.write("Name=" + URLEncoder.encode(name, "UTF-8"));
			
			try {
				Document doc = XMLUtils.parse(new FileInputStream(new File(configDir, "server.xml")));
				elServer = doc.getDocumentElement();
			} catch(Exception e) {
				elServer = null;
			}
			
			// get the contact name and contact email if possible
			if(contactName != null && elServer != null) {
				contactName = XMLUtils.getChildText(elServer, "ContactName");
			}
			if(contactEMail != null && elServer != null) {
				contactEMail = XMLUtils.getChildText(elServer, "ContactEMail");
			}
			
			if(contactName != null && contactName.length() > 0) {
				out.write("&ContactName=" + URLEncoder.encode(contactName, "UTF-8"));
			}
			if(contactEMail != null && contactEMail.length() > 0) {
				out.write("&ContactEMail=" + URLEncoder.encode(contactEMail, "UTF-8"));
			}
			out.write("&LocalIP=" + URLEncoder.encode(InetAddress.getLocalHost().getHostAddress(), "UTF-8"));
			out.write("&java.version=" + URLEncoder.encode(System.getProperty("java.version"), "UTF-8"));
			out.write("&java.vendor=" + URLEncoder.encode(System.getProperty("java.vendor"), "UTF-8"));
			out.write("&os.name=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
			out.write("&os.arch=" + URLEncoder.encode(System.getProperty("os.arch"), "UTF-8"));
			out.write("&os.version=" + URLEncoder.encode(System.getProperty("os.version"), "UTF-8"));
			out.close();
			
			// execute method and handle any error responses.
			InputStream in = connection.getInputStream();
			// parse the XML document if any
			Document doc = XMLUtils.parse(in);
			Element el = doc.getDocumentElement();
			
			// retrieve the certificate if any
			String strCertificate = XMLUtils.getChildText(el, "certificate");
			if(strCertificate != null) {
				byte[] certificate = Base64.decode(strCertificate);
				FileOutputStream fos = new FileOutputStream(new File(configDir, "dysoweb.p12"));
				fos.write(certificate);
				fos.close();
			}
			// get the server id
			sysId = el.getAttribute("sysId");
		} catch(Exception e) {
			// unable to register the server on the remote repository
		}

		// update the server config
		elServer = XMLUtils.newElement("server");
		if (sysId != null)
			elServer.setAttribute("sysId", sysId);

		// store the password
		XMLUtils.addElement(elServer, "Password", encrypt(strPassword));
		if(contactName != null && contactName.length() > 0) {
			XMLUtils.addElement(elServer, "ContactName", contactName);
		}
		if(contactEMail != null && contactEMail.length() > 0) {
			XMLUtils.addElement(elServer, "ContactEMail", contactEMail);
		}

		// output the content as XML
		Source source = new DOMSource(elServer);
		StringWriter sw = new StringWriter();

		StreamResult result = new StreamResult(sw);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty("indent", "yes");
		xformer.transform(source, result);
		String xml = sw.toString();

		OutputStream os = new FileOutputStream(new File(configDir, "server.xml"));
		Writer w = new OutputStreamWriter(os, "UTF-8");
		w.write(xml);
		w.close();
		
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
	
	private static String encrypt(String plaintext)
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

	private boolean checkParameters(HttpServletRequest request,
			HttpServletResponse response) {
		// check the parameters
		String pass1 = request.getParameter("Password");
		if (pass1 == null) {
			request.setAttribute(ErrorTag.ERROR, "Password is required");
			return false;
		}
		String pass2 = request.getParameter("Password2");
		if (!pass1.equals(pass2)) {
			request.setAttribute(ErrorTag.ERROR, "Passwords do not match");
			return false;
		}
		return true;
	}

}
