package com.requea.dysoweb.commons.logging;


import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		context.addBundleListener(new BundleListener() {
			public void bundleChanged(BundleEvent ev) {
				switch (ev.getType()) {
				case BundleEvent.STARTED:
					// release all the logs
					LogFactory.releaseAll();
					break;
				case BundleEvent.STOPPED:
					// release all the logs
					LogFactory.releaseAll();
					break;
				}				
			}
		});
	}

	public void stop(BundleContext context) throws Exception {
	}

}
