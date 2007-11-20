package com.requea.dysoweb.panel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private BundleContext 		fContext;
	private static Activator 	fDefault;

	public void start(BundleContext context) throws Exception {
		fContext = context;
		fDefault = this;
	}

	public void stop(BundleContext context) throws Exception {
		fContext = null;
	}

	public static Activator getDefault() {
		return fDefault;
	}

	public BundleContext getContext() {
		return fContext;
	}
}
