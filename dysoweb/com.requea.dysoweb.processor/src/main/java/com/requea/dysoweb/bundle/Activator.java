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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import com.requea.dysoweb.processor.RequestProcessor;
import com.requea.webenv.IWebProcessor;

public class Activator implements BundleActivator {

	private ServiceRegistration fProcessorRef;
	private RequestProcessor fRequestProcessor;

    private static Log fLog = LogFactory.getLog(RequestProcessor.class);
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		// create the request processor to handle all incoming ServletRequest
		// from the container webapp
		fRequestProcessor = new RequestProcessor();
		fProcessorRef = context.registerService(IWebProcessor.class.getName(), fRequestProcessor, null);

		
		context.addBundleListener(new BundleListener() {
			public void bundleChanged(BundleEvent ev) {
				Bundle bundle =ev.getBundle();
				switch (ev.getType()) {
				case BundleEvent.STARTED:
					// check if the bundle is a dysoweb application
					URL res = bundle.getEntry("/webapp");
					if(res != null) {
						try {
							fRequestProcessor.deploy(bundle);
						} catch(Exception e) {
							fLog.error("Unable to deploy bundle " + bundle.getBundleId(), e);
						}
					}
					break;
				case BundleEvent.STOPPED:
					try {
						fRequestProcessor.undeploy(bundle);
					} catch(Exception e) {
						fLog.error("Unable to undeploy bundle " + bundle.getBundleId(), e);
					}
					break;
				}				
			}
		});
		
		// get the bundles with definitions directories
		Bundle[] bundles = context.getBundles();
		bundles = sortVersions(bundles);
		for(int i=0; i<bundles.length; i++) {
			Bundle bundle = bundles[i];
			if(bundle.getState() == Bundle.ACTIVE) {
				// check if the bundle is a dynaapp
				URL res = bundle.getEntry("/webapp");
				if(res != null) {
					fRequestProcessor.deploy(bundle);
				}
			}
		}
	}

	private Bundle[] sortVersions(Bundle[] bundles) {
		List lst = new ArrayList();

		for(int i=0; i<bundles.length; i++) {
			Bundle bundle = bundles[i];
			String strVer = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
			if(strVer != null) {
				Version v = Version.parseVersion(strVer);
				// check if we have a better version?
				boolean found = false;
				for(int j=0; j<bundles.length; j++) {
					Bundle b = bundles[j];
					if(i != j && b.getSymbolicName() != null && b.getSymbolicName().equals(bundle.getSymbolicName())) {
						// check the version
						String strBundleVer = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
						if(strBundleVer != null) {
							Version bundleVer = Version.parseVersion(strBundleVer);
							if(bundleVer.compareTo(v) > 0) {
								// this bundle is better than the one:
								found = true;
							}
						}
					}
				}
				if(!found) {
					// we have not found a better version
					lst.add(bundle);
				}
			}
		}
		
		return (Bundle[])lst.toArray(new Bundle[lst.size()]);
		
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

