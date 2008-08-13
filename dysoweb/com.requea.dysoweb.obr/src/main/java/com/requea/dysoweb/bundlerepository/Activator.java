package com.requea.dysoweb.bundlerepository;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.obr.RepositoryAdmin;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		// register the OBR
		RepositoryAdmin repo = new RepositoryAdminImpl(context);
		Hashtable props = new Hashtable();
		props.put("com.requea.dysoweb.obr.monitor", "true");		
		context.registerService(
                RepositoryAdmin.class.getName(),
                repo, null);
		
		// register the obr command
        try
        {
            // Register "obr" impl command service as a
            // wrapper for the bundle repository service.
            context.registerService(
                org.apache.felix.shell.Command.class.getName(),
                new ObrCommandImpl(context, repo), null);
        }
        catch (Throwable th)
        {
        	System.err.println("Unable to register repo command: " + th.getMessage());
        }
	}

	public void stop(BundleContext context) throws Exception {

	}

}
