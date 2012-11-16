package com.requea.dysoweb.processor;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.requea.webenv.DysowebSessionSerializer;



public class RequestWrapper extends HttpServletRequestWrapper {


	private ServletContext fServletContext;


	public RequestWrapper(ServletContext servletContext, HttpServletRequest request) {
		super(request);
		fServletContext = servletContext;
	}
	
	public String getContextPath() {
		return super.getContextPath();
	}

	public HttpSession getSession() {
		HttpSession session = super.getSession();
		if(session instanceof DysowebSession) {
			return session;
		} else if(session != null) {
			return new DysowebSession(session);
		} else {
			return null;
		}
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = super.getSession(create);
		if(session != null) {
			return new DysowebSession(session);
		} else {
			return null;
		}
	}

	
    public String getPathInfo() {
	    // retrieve the key
	    String key = (String) this.getAttribute(RequestProcessor.PATHINFO);
	    if(key == null || key.startsWith("*")) {
	        // use default value
	        return super.getPathInfo();
	    } else {
            // get the uri
            String uri = this.getRequestURI();
            String contextPath = this.getContextPath();
            if(uri.startsWith(contextPath)) {
                uri = uri.substring(contextPath.length());
            } else {
             // use default value
                return super.getPathInfo();
            }
            // remove the key
	        int idx = key.indexOf('*');
	        if(idx > 0) {
	            // remove star
	            key = key.substring(0, idx);
	        }
	        if(key.endsWith("/")) {
	            key = key.substring(0, key.length()-1);
	        }
	        if(uri.startsWith(key)) {
	            uri = uri.substring(key.length());
	        } else {
	            // use default value
                return super.getPathInfo();
	        }
	        idx = uri.indexOf('?');
	        if(idx >= 0) {
	            uri = uri.substring(0,idx);
	        }
	        return uri;
	    }
    }


    private class DysowebSession implements HttpSession {

		HttpSession fSession;
		
		public DysowebSession(HttpSession session) {
			fSession = session;
		}

		public Object getAttribute(String attr) {
			Object obj = fSession.getAttribute(attr);
			if(obj instanceof DysowebSessionSerializer) {
				try {
					return ((DysowebSessionSerializer)obj).getObject();
				} catch (IOException e) {
					// could not serialize the thing
					return null;
				} catch (ClassNotFoundException e) {
					// could not deserialize the thing
					return null;
				}
			} else {
				return obj;
			}
		}

		public Enumeration getAttributeNames() {
			return fSession.getAttributeNames();
		}

		public long getCreationTime() {
			return fSession.getCreationTime();
		}

		public String getId() {
			return fSession.getId();
		}

		public long getLastAccessedTime() {
			return fSession.getLastAccessedTime();
		}

		public int getMaxInactiveInterval() {
			return fSession.getMaxInactiveInterval();
		}

		public ServletContext getServletContext() {
			return fServletContext;
		}

		public HttpSessionContext getSessionContext() {
			return fSession.getSessionContext();
		}

		public Object getValue(String arg0) {
			return fSession.getValue(arg0);
		}

		public String[] getValueNames() {
			return fSession.getValueNames();
		}

		public void invalidate() {
			fSession.invalidate();
		}

		public boolean isNew() {
			return fSession.isNew();
		}

		public void putValue(String arg0, Object value) {
			fSession.putValue(arg0, value);
		}

		public void removeAttribute(String attr) {
			fSession.removeAttribute(attr);
		}

		public void removeValue(String value) {
			fSession.removeValue(value);
		}

		public void setAttribute(String attr, Object value) {
			if(value == null || value instanceof String || value instanceof Boolean) {
				// simple types
				fSession.setAttribute(attr, value);
			} else if(value instanceof DysowebSessionSerializer) {
				// do not wrap something that is already wrapped
				fSession.setAttribute(attr, value);
			} else if(value instanceof Serializable) {
				DysowebSessionSerializer ser = new DysowebSessionSerializer((Serializable)value);
				fSession.setAttribute(attr, ser);
			} else {
				// this object should be serializable, but so what!
				fSession.setAttribute(attr, value);
			}
		}

		public void setMaxInactiveInterval(int arg0) {
			fSession.setMaxInactiveInterval(arg0);
		}
		
	}
	
}


