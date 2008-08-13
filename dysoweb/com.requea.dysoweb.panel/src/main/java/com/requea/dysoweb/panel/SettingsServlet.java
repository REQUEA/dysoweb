package com.requea.dysoweb.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.panel.tags.InputTag;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;

public class SettingsServlet extends HttpServlet {

	
	private static final String COM_REQUEA_DYSOWEB_PANEL = "com.requea.dysoweb.panel.";
	private static final long serialVersionUID = 1L;
	private File fConfigDir;
	

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityFilter.getConfigDir(config.getServletContext());
		fConfigDir.mkdirs();

	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		process(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		process(request, response);
	}

	private void process(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		// init settings values if not already done
		initValues(request);
		
		String op = request.getParameter("op");
		// update values into session
		updateValues(request);
		
		// postback?
		if("postback".equals(op)) {
			// back
		} else if("test".equals(op)) {
			// try to open the connection with the repo
			test(request);
		} else if("save".equals(op)) {
			// save the content in the config file
			Element elConfig = getServerConfig();
			saveConfigValue(request, elConfig, "RepoURL");
			saveConfigValue(request, elConfig, "Proxy");
			String proxy = (String)request.getSession().getAttribute(COM_REQUEA_DYSOWEB_PANEL+"Proxy");
			if("manual".equals(proxy)) {
				saveConfigValue(request, elConfig, "ProxyHost");
				saveConfigValue(request, elConfig, "ProxyPort");
				saveConfigValue(request, elConfig, "ProxyAuth");
			} else {
				removeConfigValue(elConfig, "ProxyHost");
				removeConfigValue(elConfig, "ProxyPort");
				removeConfigValue(elConfig, "ProxyAuth");
			}
			// save the config
			try {
		        String xml = XMLUtils.DocumentToString(elConfig.getOwnerDocument(), true);
		        File file = new File(fConfigDir, "server.xml");
		        // then write the content as utf-8: zip it if the requests accept zip, since xml compresses VERY well
		        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		        w.write(xml);
		        w.close();
		        
				File fileCertificate = new File(fConfigDir, "dysoweb.p12");
				if(!fileCertificate.exists()) {
			        String repoURL = getValueFromSession(request.getSession(), "RepoURL");
			        if(repoURL.startsWith("https://repo.requea.com")) {
			        	// update server registration if not already done
			        	InstallServlet.updateServerRegistration(fConfigDir, null, null, null);
						// reinit the repo
			        	InstallServlet.initRepo(fConfigDir, elConfig);	
			        	request.getSession().removeAttribute(InstallServlet.FEATURES);
			        }
				}
			} catch(Exception e) {
				request.setAttribute(ErrorTag.ERROR, e);
			}
		}
		// re forward to registration for correction of errors
		RequestDispatcher rd = request
				.getRequestDispatcher("/dysoweb/panel/secure/settings.jsp");
		rd.forward(request, response);
	}

	private void removeConfigValue(Element elConfig, String name) {
		Element elVal = XMLUtils.getChild(elConfig, name);
		if(elVal != null) {
			elConfig.removeChild(elVal);
		}
	}

	private void saveConfigValue(HttpServletRequest request, Element elConfig,
			String name) {
		
		Element elVal = XMLUtils.getChild(elConfig, name);
		if(elVal == null) {
			elVal = XMLUtils.addElement(elConfig, name);
		}
		HttpSession session = request.getSession();
		XMLUtils.setText(elVal, (String)session.getAttribute(COM_REQUEA_DYSOWEB_PANEL+name));
	}

	private void test(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String repoURL = getValueFromSession(session, "RepoURL");
		String proxy = getValueFromSession(session, "Proxy");
		String proxyHost = getValueFromSession(session, "ProxyHost");
		String proxyPort = getValueFromSession(session, "ProxyPort");
		String proxyAuth = getValueFromSession(session, "ProxyAuth");
		// try to open the connection
		try {
			URL url = new URL(repoURL);
			URLConnection cnx = null;
			// proxy?
			if("manual".equals(proxy)) {
				// create a proxy
				Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
				cnx = url.openConnection(p);
				// proxy auth?
				if(proxyAuth != null && proxyAuth.length() > 0) {
                    String base64 = Base64.encode(proxyAuth.getBytes());
                    cnx.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
				}
			} else {
				// open the connection with default settings
				// set proxy autodetect
				try {
				    System.setProperty("java.net.useSystemProxies","true");
				} catch (SecurityException e) {
				    ; // failing to set this property isn't fatal
				}
				cnx = url.openConnection();	
			}
			// present the client certificate?
			if(cnx instanceof HttpsURLConnection) {
				SSLSocketFactory factory = InstallServlet.initSocketFactory(fConfigDir);
				((HttpsURLConnection)cnx).setSSLSocketFactory(factory);
			}

			// read the first line
			String type = cnx.getContentType();
			// must be of type "text/xml"
			if(type == null || !type.startsWith("text/xml")) {
				throw new Exception("Incorrect type for content, check the URL ("+type+")");
			}
			// read the content type to make sure that we have something
			InputStream is = cnx.getInputStream();
			is.close();
			
			// it was successful
			request.setAttribute(InputTag.INFO, "Successful repository connection to " + repoURL);
			
		} catch(MalformedURLException e) {
			request.setAttribute(ErrorTag.ERROR, "Incorrect reposiotry URL: " + e.getMessage());
		} catch(UnknownHostException e) {
			request.setAttribute(ErrorTag.ERROR, "Unknown host: " + e.getMessage());
		} catch(FileNotFoundException e) {
			request.setAttribute(ErrorTag.ERROR, "Unable to find " + e.getMessage());
		} catch(Exception e) {
			request.setAttribute(ErrorTag.ERROR, e);
		}
	}

	private void updateValues(HttpServletRequest request) {
		HttpSession session = request.getSession();
		
		updateValue(session, request, "RepoURL");
		updateValue(session, request, "Proxy");
		updateValue(session, request, "ProxyHost");
		updateValue(session, request, "ProxyPort");
		updateValue(session, request, "ProxyAuth");
	}

	private void updateValue(HttpSession session, HttpServletRequest request,
			String param) {
		String value = request.getParameter(param);
		if(value != null) {
			setValueInSession(session, param, value);
		}
	}

	private void initValues(HttpServletRequest request) {
		HttpSession session = request.getSession();

		String repoURL = getValueFromSession(session, "RepoURL");
		if(repoURL != null) {
			// already done
			return;
		}
		
		// load the config
		Element elConfig = getServerConfig();
		repoURL = XMLUtils.getChildText(elConfig, "RepoURL");
		if(repoURL == null) {
			repoURL = InstallServlet.DEFAULT_REPO;
		}
		setValueInSession(session, "RepoURL", repoURL);
		// proxy
		String proxy = XMLUtils.getChildText(elConfig, "Proxy");
		if(proxy == null) {
			proxy = "auto";
		}
		setValueInSession(session, "Proxy", proxy);
		
		// proxy host
		String proxyHost = XMLUtils.getChildText(elConfig, "ProxyHost");
		if(proxyHost == null) {
			proxyHost = "";
		}
		setValueInSession(session, "ProxyHost", proxyHost);
		
		// proxy port
		String proxyPort = XMLUtils.getChildText(elConfig, "ProxyPort");
		if(proxyPort == null) {
			proxyPort = "3128";
		}
		setValueInSession(session, "ProxyPort", proxyPort);
		
		// proxy auth
		String proxyAuth = XMLUtils.getChildText(elConfig, "ProxyAuth");
		if(proxyAuth == null) {
			proxyAuth = "";
		}
		setValueInSession(session, "ProxyAuth", proxyAuth);
		
		
	}

	private void setValueInSession(HttpSession session, String name, String value) {
		session.setAttribute(COM_REQUEA_DYSOWEB_PANEL+name, value);
		
	}

	private String getValueFromSession(HttpSession session, String name) {
		return (String)session.getAttribute(COM_REQUEA_DYSOWEB_PANEL+name);
	}

	private Element getServerConfig() {

		File f = new File(fConfigDir, "server.xml");
		if (!f.exists()) {
			return null;
		}

		// parse the file
		try {
			Document doc = XMLUtils.parse(new FileInputStream(f));
			return doc.getDocumentElement();
		} catch (FileNotFoundException e) {
			// cannot happen
			return null;
		} catch (XMLException e) {
			// should not happen: XML is corrupted. Better to ignore it
			return null;
		}
	}

}
