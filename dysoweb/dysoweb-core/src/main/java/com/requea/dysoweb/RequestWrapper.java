package com.requea.dysoweb;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


public class RequestWrapper extends HttpServletRequestWrapper {


	private String fPrefix;
	private String fContextPath;

	public RequestWrapper(HttpServletRequest request, String prefix) {
		super(request);
		fContextPath = request.getContextPath();
		fPrefix = prefix;
	}
	
	public String getContextPath() {
		return fPrefix == null ? fContextPath : fContextPath + fPrefix;
	}


	public StringBuffer getRequestURL() {
		StringBuffer sb = super.getRequestURL();
		if(fPrefix != null) {
			int length = fPrefix.length();
			if(sb.length() >= length && sb.substring(0, length-1).equals(fPrefix)) {
				StringBuffer sb2 = new StringBuffer(sb.length());
				sb2.append(fContextPath);
				sb2.append(sb.substring(length));
				return sb2;
			} else { 
				return sb;
			}
		} else {
			return sb;
		}
	}

	public String getServletPath() {
		String str = super.getServletPath();
		if(str != null && fPrefix != null && str.startsWith(fPrefix)) {
			return str.substring(fPrefix.length());
		} else {
			return str;
		}
	}
	public String getRealPath(String path) {
		if(fPrefix != null)
			return super.getRealPath(fPrefix+path);
		else
			return super.getRealPath(path);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		if(fPrefix != null)
			return super.getRequestDispatcher(fPrefix+path);
		else
			return super.getRequestDispatcher(path);
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
			return fSession.getServletContext();
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


