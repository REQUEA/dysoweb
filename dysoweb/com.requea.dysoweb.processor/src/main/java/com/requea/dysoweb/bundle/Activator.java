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

package com.requea.dysoweb.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.requea.dysoweb.WebAppService;
import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.processor.RequestProcessor;
import com.requea.webenv.IWebProcessor;

public class Activator implements BundleActivator {

	private BundleContext fContext;
	private ServiceRegistration fProcessorRef;
	private RequestProcessor fRequestProcessor;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		fContext = context;
		
		
		// create the request processor to handle all incoming ServletRequest
		// from the container webapp
		fRequestProcessor = new RequestProcessor();
		fProcessorRef = context.registerService(IWebProcessor.class.getName(), fRequestProcessor, null);
		
		// get the service ref of all dysoweb apps
		ServiceReference[] srs = context.getServiceReferences(WebAppService.class.getName(), 
				null);
		if(srs != null && srs.length > 0) {
			// auto-deploy all the services
			for(int i=0; i<srs.length; i++) {
				ServiceReference sr = srs[i];
				Object obj = fContext.getService(sr);
				if(obj instanceof WebAppService) {
					fRequestProcessor.deploy((WebAppService)obj);
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
								fRequestProcessor.deploy((WebAppService)obj);
							}
						} catch (WebAppException e) {
							// TODO: log the error
							e.printStackTrace();
						}
					}
					break;
					case ServiceEvent.UNREGISTERING: {
						try {
							fRequestProcessor.undeploy(sr.getBundle());
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

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// stop processing incoming requests
		fRequestProcessor = null;
		
		// unregister the context service
		if(fProcessorRef != null) {
			fProcessorRef.unregister();
		}
	}

}

