package com.requea.dysoweb.panel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.requea.dysoweb.WebAppService;

public class Activator implements BundleActivator {

	private ServiceRegistration fWebApp;
	private BundleContext 		fContext;
	private static Activator 	fDefault;

	public void start(BundleContext context) throws Exception {
		fContext = context;
		fWebApp = context.registerService(WebAppService.class.getName(), 
				new Service(context.getBundle()), 
				null);
		
		fDefault = this;
	}

	public void stop(BundleContext context) throws Exception {
		// unregister the web app service
		fWebApp.unregister();
		fContext = null;
	}

	public static Activator getDefault() {
		return fDefault;
	}

	public BundleContext getContext() {
		return fContext;
	}
	
	class Service extends WebAppService {

		public Service(Bundle bundle) {
			super(bundle);
		}
		
	}

}
