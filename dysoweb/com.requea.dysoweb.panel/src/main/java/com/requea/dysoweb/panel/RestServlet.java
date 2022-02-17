package com.requea.dysoweb.panel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jdk.nashorn.api.scripting.JSObject;

import org.apache.felix.shell.ShellService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.requea.dysoweb.panel.InstallManager.Status;
import com.requea.dysoweb.panel.monitor.AjaxProgressMonitor;
import com.requea.dysoweb.panel.tags.ErrorTag;
import com.requea.dysoweb.util.xml.XMLException;
import com.requea.dysoweb.util.xml.XMLUtils;


public class RestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private File fConfigDir;
	private File fBinDir;
	private InstallManager fInstallManager;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fConfigDir = SecurityServlet.getConfigDir(config.getServletContext());
		fBinDir = SecurityServlet.getBinDir(config.getServletContext());

		fInstallManager = new InstallManager(fConfigDir, fBinDir);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
        process(request,response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

        process(request,response);
	}
	

	private void process(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		// check auth
        String auth = request.getHeader("Authorization");
        if(!checkBasicAuthentication(request, auth)) {
        	// denied
            response.setHeader("WWW-Authenticate", "Basic"); // "BASIC
            // realm=\"users\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
		
		// Read body
    	StringBuffer buffer = new StringBuffer();
    	String line = null;
    	try {
    		BufferedReader reader = request.getReader();
    		while ((line = reader.readLine()) != null)
    			buffer.append(line);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}

    	String op = request.getServletPath();
    	if(op == null) {
    		// cannot happen, but we never know
    		response.sendError(404);
    		return;
    	}
    	
    	if(op.startsWith("/dysoweb/rest/")) {
    		op = op.substring("/dysoweb/rest/".length());
    	} else {
    		// incorrect request
    		response.sendError(404);
    		return;
    	}
    	String param = "";
    	int idx = op.indexOf('/');
    	if(idx > 0) {
    		param = op.substring(idx+1);
    		op = op.substring(0, idx);
    	}
    	
    	if("ExecuteCommand".equals(op)) {
        	String data = buffer.toString();
            JSONObject jsResult = new JSONObject();

        	try {
        		JSONObject js = new JSONObject(data);
        		
        		// get the command
        		String cmd = js.getString("command");
        		
        		// get the shell and execute the command
				Activator act = Activator.getDefault();
				ShellService shell = act != null ? act.getShell() : null;
				if(shell != null) {
		            ByteArrayOutputStream baout = new ByteArrayOutputStream();
		            ByteArrayOutputStream baerr = new ByteArrayOutputStream();
		            // retieve the command to execute
		            try {
			            shell.executeCommand(cmd, new PrintStream(baout), new PrintStream(baerr));
			            String outMsg = baout.toString();
			            String errMsg = baerr.toString();
			            
			            // return value
			            jsResult.put("code", errMsg.length() == 0 ? 200 : 503);
			            jsResult.put("out", outMsg);
			            jsResult.put("err", errMsg);
		            } catch(Exception e) {
			            // return value
			            jsResult.put("code", 503);
			            jsResult.put("out", "");
			            jsResult.put("err", e.getMessage());
		            }
				} else {
		            // return value
		            jsResult.put("code", 503);
		            jsResult.put("out", "");
		            jsResult.put("err", "Shell service not available");
				}
        	} catch(Exception e) {
        		// incorrect json format
	            // return value
	            jsResult.put("code", 503);
	            jsResult.put("out", "");
	            jsResult.put("err", e.getMessage());
        	}
        	
            // output the json
	        response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
        	out.write(jsResult.toString());
	        out.close();
        	
        	// ok
        	return;
    	}
    	
    	if("GetInstalledBundles".equals(op)) {
    		BundleContext ctx = Activator.getDefault().getContext();
			Bundle[] bundles = ctx.getBundles();
    		
    		JSONArray jsArray = new JSONArray();
			
			for(int i=0; i<bundles.length; i++) {
				Bundle bundle = bundles[i];
				
				JSONObject jsBundle = new JSONObject();
				jsBundle.put("Name", bundle.getHeaders().get("Bundle-Name"));
				jsBundle.put("Status", getStateString(bundle.getState()));
				jsBundle.put("SymbolicName", bundle.getSymbolicName());
				jsBundle.put("Version", bundle.getHeaders().get("Bundle-Version"));
				
				jsArray.put(jsBundle);
			}
			
            // output the json
	        response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
        	out.write(jsArray.toString());
	        out.close();
	        return;
    	}
    	
    	if("GetInstallables".equals(op)) {
    		
    		// load installables
    		File f = new File(fConfigDir,"server.xml");
    		String pass = null;
    		if(f.exists()) {
    			try {
					Document doc = XMLUtils.parse(new FileInputStream(f));
					Element elConfig = doc.getDocumentElement();

					// check if server certificate is there
					File file = new File(fConfigDir, "dysoweb.p12");
					if(!file.exists()) {
						try {
							// reset the socket factory
							response.sendError(503, "Platform not secured");
							return;
						} catch(Exception e) {
							// ignore
						}
					}
					// init the http client
					try {
						fInstallManager.initHttpClient(elConfig);
					} catch(Exception e) {
						response.sendError(503, "Unable to register server: " + e.getMessage());
						return;
					}				
					
					Installable[] all = fInstallManager.getInstallables(request, response, elConfig);
					
		    		JSONArray jsArray = new JSONArray();
					for(int i=0; i<all.length; i++) {
						Installable installable = all[i];
						
						JSONObject jsBundle = new JSONObject();
						jsBundle.put("Name", installable.getName());
						jsBundle.put("SymbolicName", installable.getName());
						jsBundle.put("Type", installable.getType());
						jsBundle.put("Version", installable.getVersion());
						
						jsArray.put(jsBundle);
					}

		    		response.setContentType("application/json;charset=UTF-8");
		            PrintWriter out = response.getWriter();
		        	out.write(jsArray.toString());
			        out.close();
			        return;
    			} catch(IOException e) {
    				// output error
					response.sendError(503, "Unable to register server: " + e.getMessage());
					return;
    			} catch(XMLException e) {
					response.sendError(503, "Unable to register server: " + e.getMessage());
					return;
    			} catch (Exception e) {
    				// output error
					response.sendError(503, "Unable to register server: " + e.getMessage());
					return;
				}
    		}
    		
    	}
    	
    	if("Install".equals(op)) {
    		
			JSONObject jsResult = new JSONObject();
    		
    		// load installables
    		File f = new File(fConfigDir,"server.xml");
    		if(f.exists()) {
    			try {
					Document doc = XMLUtils.parse(new FileInputStream(f));
					Element elConfig = doc.getDocumentElement();
	    		
					// get the installable bundles and features
					Installable[] allInstallables = fInstallManager.getInstallables(request, response, elConfig);

					if (allInstallables == null) {
						throw new Exception((String) request.getAttribute(ErrorTag.ERROR));
					}

					List lst = new ArrayList();
					for(int i=0; i<allInstallables.length; i++) {
						Installable inst = allInstallables[i];
						// in the path?
						if((("product".equals(inst.getType()) || "feature".equals(inst.getType()))) ||
								("bundle".equals(inst.getType()) && inst.isRoot())) {
								lst.add(inst);
						}
					}
					Installable[] installables = (Installable[])lst.toArray(new Installable[lst.size()]);

					// get the version to install
		    		String ver = param;
		            if(ver == null || ver.equals("")) {
		                ver = (String) request.getSession().getAttribute(InstallManager.CURRENTVERSION);
		            }
		            if("".equals(ver) || "base".equals(ver)) {
		                ver = null;
		            }
		            if("{version}".equals(ver)) {
		            	ver = null;
		            }
					
					RepositoryAdmin repo = fInstallManager.initRepo(fConfigDir, elConfig, ver);
					
					BundleContext context = Activator.getDefault().getContext();

					// once we have the repo, we ask for deployment
					// create the progress monitor
					AjaxProgressMonitor monitor = new AjaxProgressMonitor();
					Status status = new Status();

					PackageAdmin pa = null;
					ServiceReference ref = context.getServiceReference(
							org.osgi.service.packageadmin.PackageAdmin.class.getName());
					if (ref != null) {
						pa = (PackageAdmin) context.getService(ref);
					}

					// launch the thread to install the bundles
					Thread th = new Thread(fInstallManager.newInstaller(elConfig, context, pa, repo, installables, monitor, status, true));
					th.start();
					
					// wait for completion: should use a future, but pb with old jdk versions
					int nb = 0;
					Document docMonitor = XMLUtils.newDocument();
					Element elRoot = docMonitor.createElement("root");
					while(nb++ < 5*60 && status.getStatus() != InstallManager.Status.DONE && status.getStatus() != InstallManager.Status.ERROR) {
						// wait a sec, 5 minutes max
						Thread.sleep(1000);
						monitor.renderProgress(elRoot);
					}

					// install done or resulting in an error
					if(status.getStatus() == InstallManager.Status.DONE) {
						jsResult.put("code", 200);
						jsResult.put("message", "ok");
					} else if(status.getException() != null) {
						jsResult.put("code", 503);
						jsResult.put("message", status.getException().getMessage() == null?status.getException().getClass():status.getException().getMessage());
					} else {
						jsResult.put("code", 503);
						jsResult.put("message", "Installed failed, but did not return an error.");
					}
					
    			} catch(Exception e) {
					jsResult.put("code", 503);
					jsResult.put("message", e.getMessage());
    			}
    		} else {
				jsResult.put("code", 503);
				jsResult.put("message", "Installed failed. Platform not secured.");
    		}

    		// output the result
    		response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
        	out.write(jsResult.toString());
	        out.close();
	        return;
    	}
    	
    	// incorrect uri
    	response.sendError(404);        
	}

    public String getStateString(int i)
    {
        if (i == Bundle.ACTIVE)
            return "Active";
        else if (i == Bundle.INSTALLED)
            return "Installed";
        else if (i == Bundle.RESOLVED)
            return "Resolved";
        else if (i == Bundle.STARTING)
            return "Starting";
        else if (i == Bundle.STOPPING)
            return "Stopping";
        return "Unknown";
    }
	
	
    protected boolean checkBasicAuthentication(HttpServletRequest request, String auth) throws IOException {
    	
		// check if we are authenticated
		HttpSession session = ((HttpServletRequest)request).getSession();
		Object obj = session.getAttribute(SecurityFilter.AUTH);
		if(Boolean.TRUE.equals(obj)) {
			return true;
		}

		// otherwise, check basic auth
        if (auth == null) {
            return false; // no auth
        }
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false; // we only do BASIC
        }
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
        String userpassDecoded = new String(Base64.decode(userpassEncoded));

        int idx = userpassDecoded.indexOf(":");
        String userLogin = userpassDecoded;
        if(idx > 0) {
            userLogin = userpassDecoded.substring(0,idx);
            userpassDecoded = userpassDecoded.substring(idx+1);
        } else {
            userpassDecoded = null;
        }
        
		// check if already authenticated
		File f = new File(fConfigDir,"server.xml");
		String pass = null;
		if(f.exists()) {
			try {
				Document doc = XMLUtils.parse(new FileInputStream(f));
				Element el = doc.getDocumentElement();
				pass = XMLUtils.getChildText(el, "Password");
			} catch(Exception e) {
				// assume platform is secured, but corrupted
				System.err.println("Platform server.xml is corrupted");
				e.printStackTrace();
				pass = null;
			}
		}
		if(pass != null) {
			// compare encrypted versions
			try {
				if(userpassDecoded != null) {
					userpassDecoded = SecurityServlet.encrypt(userpassDecoded);
				}
			} catch(Exception e) {
				userpassDecoded = null;
			}
			if (userpassDecoded == null
					|| !userpassDecoded.equals(pass)) {
				// password do not match
				return false;
			} else {
				// ok
				return true;
			}
		}
		
		// no password: platform not secured, deny access
        return false;
    }
    
}
