package com.requea.dysoweb.portlet.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.WebAppService;
import com.requea.dysoweb.portlet.DysowebPortletContext;
import com.requea.dysoweb.portlet.processor.PortletProcessor;

public class Activator implements BundleActivator {

	PortletProcessor fProcessor;
	private BundleContext fContext;
	
	public void start(BundleContext context) throws Exception {
		fContext = context;
		// create the portlet processor
		fProcessor = new PortletProcessor();
		DysowebPortletContext.setPortletProcessor(fProcessor);
		// get the service ref of all dysoweb apps
		ServiceReference[] srs = context.getServiceReferences(WebAppService.class.getName(), 
				null);
		if(srs != null && srs.length > 0) {
			// auto-deploy all the services
			for(int i=0; i<srs.length; i++) {
				ServiceReference sr = srs[i];
				Object obj = fContext.getService(sr);
				if(obj instanceof WebAppService) {
					fProcessor.deploy((WebAppService)obj);
				}
			}
		}
		
		ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent ev) {
				ServiceReference sr = ev.getServiceReference();
				switch (ev.getType()) {
					case ServiceEvent.REGISTERED: {
						try {
							Object obj = fContext.getService(sr);
							if(obj instanceof WebAppService) {
								fProcessor.deploy((WebAppService)obj);
							}
						} catch (WebAppException e) {
							// TODO: log the error
							e.printStackTrace();
						}
					}
					break;
					case ServiceEvent.UNREGISTERING: {
						try {
							fProcessor.undeploy(sr.getBundle());
						} catch (WebAppException e) {
							// TODO: log the error
						}
					}
					break;
				}
			}
		};
		// adds the filter on the IWebService
		String filter = "(objectclass=" + WebAppService.class.getName() + ")";
		context.addServiceListener(sl, filter);
	}

	public void stop(BundleContext ctx) throws Exception {
		// destroy the portlet processor
		DysowebPortletContext.setPortletProcessor(null);
	}

}
