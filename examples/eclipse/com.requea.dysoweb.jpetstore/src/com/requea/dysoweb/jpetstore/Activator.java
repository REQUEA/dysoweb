package com.requea.dysoweb.jpetstore;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.requea.dysoweb.WebAppService;

public class Activator implements BundleActivator {

	private ServiceRegistration fWebApp;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		fWebApp = context.registerService(WebAppService.class.getName(), 
				new WebAppService(context.getBundle()) { },
				null);
		
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// unregister the web app service
		fWebApp.unregister();
		fWebApp = null;
	}

}
