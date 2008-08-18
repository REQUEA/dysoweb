package com.requea.dysoweb.log4j;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.PropertyConfigurator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.requea.webenv.IWebProcessor;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		// init log4j with the proper properties file
		Thread th = Thread.currentThread();
		ClassLoader contextClassLoader = th.getContextClassLoader();
		
		try {
			th.setContextClassLoader(IWebProcessor.class.getClassLoader());
			InitialContext ic = new InitialContext();
			Context ctx = (Context) ic.lookup("java:comp/env");
			String homeDir = (String) (ctx.lookup("dysoweb.home"));
			File f = new File(homeDir, "config/log4j.properties");
			if(f.exists()) {
				PropertyConfigurator.configureAndWatch(f.getAbsolutePath());
			}
		} catch (NamingException nex) {
			// not defined: use standard logging mechanism
		} finally {
			th.setContextClassLoader(contextClassLoader);
		}
	}

	public void stop(BundleContext context) throws Exception {

	}

}
