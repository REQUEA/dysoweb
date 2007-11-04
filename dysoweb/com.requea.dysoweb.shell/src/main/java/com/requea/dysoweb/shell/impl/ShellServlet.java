// ========================================================================
// Copyright 2007 Requea.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.requea.dysoweb.shell.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.felix.shell.ShellService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class ShellServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static DocumentBuilderFactory fFactory;

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		try {
			DocumentBuilder builder = fFactory.newDocumentBuilder();

			Document doc = builder.newDocument();
			Element el = doc.createElement("result");
			doc.appendChild(el);

			Element elErr = doc.createElement("div");
			el.appendChild(elErr);
			elErr.setAttribute("class", "err");
			
			
			Element elMsg = doc.createElement("div");
			el.appendChild(elMsg);
			elMsg.setAttribute("class", "msg");
			
			// check if the shell operation is authorized
			Object shellAuth = request.getSession().getAttribute("com.requea.dysoweb.shell.auth");
			boolean authorized = false;
			if(Boolean.TRUE.equals(shellAuth)) {
				authorized = true;
			} else if(Boolean.FALSE.equals(shellAuth)) {
				// explicitely denied
				authorized = false;
			} else if(request.isUserInRole("osgimanager")) {
				// user has osgi role: may administer the platform
				authorized = true;
			} else {
				// authorize if request come from localhost
				if(request.getLocalName().equals("localhost")) {
					authorized = true;
				}
			}
			if(authorized) {
				// retrieve the shell
				Activator act = Activator.getInstance();
				ShellService shell = act != null ? act.getShell() : null;
				if(shell != null) {
					// retrieve the op
					String cmd = request.getParameter("command");
					if(cmd != null) {
						cmd = cmd.trim();
			            ByteArrayOutputStream out = new ByteArrayOutputStream();
			            ByteArrayOutputStream err = new ByteArrayOutputStream();
			            // retieve the command to execute
			            try {
				            shell.executeCommand(cmd, new PrintStream(out), new PrintStream(err));
				            String errMsg = err.toString();
				            String outMsg = out.toString();
				            appendResult(elErr, errMsg);
				            appendResult(elMsg, outMsg);
				            if(errMsg == null || errMsg.length() == 0) {
				            	if(outMsg != null && outMsg.length() > 0) {
					            	appendResult(elMsg, "\n");
				            	}
				            	appendResult(elMsg, "Command executed");
				            }
			            } catch(Exception e) {
			            	setText(el, "Error: " + e.getMessage());
			            }
					}
				} else {
					setText(elErr, "Error, no shell is available");
				}
			} else {
				setText(elErr, "Error, operation not authorized");
			}
			
			// ajax response
	    	Source source = new DOMSource(el);
			StringWriter out = new StringWriter();
			
			StreamResult result = new StreamResult(out);
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.setOutputProperty("indent", "yes");
			xformer.transform(source, result);
			
	        String xml = out.toString();
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
		
		
		} catch (ParserConfigurationException e) {
			throw new ServletException(e);
		} catch (TransformerConfigurationException e) {
			throw new ServletException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new ServletException(e);
		} catch (TransformerException e) {
			throw new ServletException(e);
		}
		
	}

    /**
     * Sets the text value for a given element.
     * @param el
     * @param value
     */
    public static void setText(Element el, String value) {
        // remove the children if already exist
        while (el.getFirstChild() != null) {
            el.removeChild(el.getFirstChild());
        }
        Text txt = el.getOwnerDocument().createTextNode(value);
        el.appendChild(txt);
    }

	
	private void appendResult(Element el, String str) {
        StringTokenizer st = new StringTokenizer(str, "\n");
        boolean first = true;
        while(st.hasMoreTokens()) {
        	String line = st.nextToken();
        	// add a break?
        	if(!first) {
        		Element elDiv = el.getOwnerDocument().createElement("div");
        		el.appendChild(elDiv);
        	} else {
        		first = false;
        	}
        	Node txt = el.getOwnerDocument().createTextNode(line);
        	el.appendChild(txt);
        }
	}
	
    static {
        fFactory =
            DocumentBuilderFactory.newInstance();
        fFactory.setNamespaceAware(true);
        fFactory.setIgnoringElementContentWhitespace(true);
        fFactory.setValidating(false);
    }

    
}
