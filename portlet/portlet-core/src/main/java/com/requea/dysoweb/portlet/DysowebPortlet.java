package com.requea.dysoweb.portlet;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class DysowebPortlet extends GenericPortlet implements IPortletProcessorListener {

	Portlet fProxy;
	private PortletConfig fConfig;
	
	public void init(PortletConfig config) throws PortletException {
		// Generic Portlet Default initialization
		super.init(config);

		if(fProxy != null) {
			// destroy the previous one
			fProxy.destroy();
			fProxy = null;
		}
		// store the config for subsequent portlet restart
		fConfig = config;
		initProxy();
	}

	private synchronized void initProxy() throws PortletException {
		if(fProxy != null) {
			// already initialized
			return;
		}
		// get the portlet from the portlet Processor
		if(DysowebPortletContext.getPortletProcessor() == null) {
			// cannot initalize at this point
			return;
		} else {
			Portlet proxy = DysowebPortletContext.getPortletProcessor().createPortlet(this, fConfig.getPortletName());
			if(proxy != null) {
				// initialize the portlet
				proxy.init(fConfig);
			}
			fProxy = proxy;
			return;
		}
	}

	public void destroy() {
		if(fProxy != null) {
			fProxy.destroy();
			fProxy = null;
		}
	}

	public void processAction(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {

		if(fProxy == null) {
			initProxy();
		}
		if(fProxy != null) {
			Thread th = Thread.currentThread();
			ClassLoader cl = th.getContextClassLoader();
			try {
				th.setContextClassLoader(fProxy.getClass().getClassLoader());
				fProxy.processAction(request, response);
			} finally {
				th.setContextClassLoader(cl);
			}
		} else {
			super.processAction(request, response);
		}
	}

	public void render(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {

		if(fProxy == null) {
			initProxy();
		}
		if(fProxy != null) {
			Thread th = Thread.currentThread();
			ClassLoader cl = th.getContextClassLoader();
			try {
				th.setContextClassLoader(fProxy.getClass().getClassLoader());
				fProxy.render(request, response);
			} finally {
				th.setContextClassLoader(cl);
			}
		} else {
			// generic portlet rendering
			super.render(request, response);
		}
		
	}

	public synchronized void onProxyUnavailable() {
		// the proxy is no more available
		fProxy = null;
	}

	protected void doEdit(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {
		renderMaintenanceView(request, response);
	}

	protected void doHelp(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {
		renderMaintenanceView(request, response);
	}

	protected void doView(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {
		renderMaintenanceView(request, response);
	}

	
	protected String getTitle(RenderRequest request) {
		return "Dysoweb Portlet";
	}

	/*
	 * Renders the maintenance view of the portlet
	 */
	private void renderMaintenanceView(RenderRequest request,
			RenderResponse response) throws PortletException, IOException {
		
		PortletContext context = this.getPortletContext();
		PortletRequestDispatcher rd = context.getRequestDispatcher("/dysoweb/portlet/maint.jsp");
		rd.include(request, response);
		
	}

	
	
}
