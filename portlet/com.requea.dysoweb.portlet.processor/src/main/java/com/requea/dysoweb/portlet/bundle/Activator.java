package com.requea.dysoweb.portlet.bundle;

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
import org.osgi.framework.Version;

import com.requea.dysoweb.portlet.DysowebPortletContext;
import com.requea.dysoweb.portlet.processor.PortletProcessor;

public class Activator implements BundleActivator {

	PortletProcessor fProcessor;
    private static Log fLog = LogFactory.getLog(Activator.class);
	
	public void start(BundleContext context) throws Exception {
		
		// create the portlet processor
		fProcessor = new PortletProcessor();
		DysowebPortletContext.setPortletProcessor(fProcessor);
		
		context.addBundleListener(new BundleListener() {
			public void bundleChanged(BundleEvent ev) {
				Bundle bundle =ev.getBundle();
				switch (ev.getType()) {
				case BundleEvent.STARTED:
					// check if the bundle is a dysoweb application
					URL res = bundle.getEntry("/webapp");
					if(res != null) {
						try {
							fProcessor.deploy(bundle);
						} catch(Exception e) {
							fLog.error("Unable to deploy bundle " + bundle.getBundleId(), e);
						}
					}
					break;
				case BundleEvent.STOPPED:
					try {
						fProcessor.undeploy(bundle);
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
					fProcessor.deploy(bundle);
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
	
	public void stop(BundleContext ctx) throws Exception {
		// destroy the portlet processor
		DysowebPortletContext.setPortletProcessor(null);
	}

}
