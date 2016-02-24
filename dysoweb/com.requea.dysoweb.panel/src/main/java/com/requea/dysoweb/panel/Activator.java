package com.requea.dysoweb.panel;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.obr.RepositoryAdmin;


public class Activator implements BundleActivator {

	private BundleContext 		fContext;
	private static Activator 	fDefault;
	private static Activator fInstance;
	private ServiceReference    fOBRRef;
	private RepositoryAdmin     fRepo;
	private ShellService fShell;
	private ServiceReference<?> fShellRef;

	
	public void start(BundleContext context) throws Exception {
		fContext = context;
		
        // Listen for registering/unregistering impl service.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                synchronized (Activator.this)
                {
                    // Ignore additional services
                    if (event.getType() == ServiceEvent.REGISTERED) {
                        initRepoService();
                    } else if ((event.getType() == ServiceEvent.UNREGISTERING)
                        && event.getServiceReference().equals(fOBRRef))
                    {
                    	fContext.ungetService(fOBRRef);
                    	fOBRRef = null;
                    	fRepo = null;
                        // Try to get another service.
                    	initRepoService();
                    }
                }
            }

        };
        try
        {
        	fContext.addServiceListener(sl,
                "(objectClass="
                + RepositoryAdmin.class.getName()
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            // cannot happen
        }

        // Now try to manually initialize the impl service
        // since one might already be available.
        initRepoService();
        
        
		// register the shell service
        // Listen for registering/unregistering impl service.
        ServiceListener slShell = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                synchronized (Activator.this)
                {
                    // Ignore additional services if we already have one.
                    if ((event.getType() == ServiceEvent.REGISTERED)
                        && (fShellRef != null))
                    {
                        return;
                    }
                    // Initialize the service if we don't have one.
                    else if ((event.getType() == ServiceEvent.REGISTERED)
                        && (fShellRef == null))
                    {
                    	initializeShellService();
                    }
                    // Unget the service if it is unregistering.
                    else if ((event.getType() == ServiceEvent.UNREGISTERING)
                        && event.getServiceReference().equals(fShellRef))
                    {
                    	fContext.ungetService(fShellRef);
                    	fShellRef = null;
                    	fShell = null;
                        // Try to get another service.
                    	initializeShellService();
                    }
                }
            }
        };
        try
        {
        	fContext.addServiceListener(slShell,
                "(objectClass="
                + org.apache.felix.shell.ShellService.class.getName()
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("ShellTui: Cannot add service listener.");
            System.err.println("ShellTui: " + ex);
        }

        // Now try to manually initialize the impl service
        // since one might already be available.
        initializeShellService();		
        
		
		fDefault = this;
	}

	private void initRepoService() {
		// already have one, fuully functionnal?
		if(fOBRRef != null && "true".equals(fOBRRef.getProperty("com.requea.dysoweb.obr.monitor"))) {
			return;
		}
		
		
		// new repo service available?
		ServiceReference[] srs = null;
		try {
			srs = fContext.getServiceReferences(RepositoryAdmin.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// cannot happen
		}
		// do we have some?
		if(srs == null || srs.length == 0) {
			return;
		}
		
		// do we have an extended one available?
		for(int i=0; i<srs.length; i++) {
			ServiceReference sr = srs[i];
			if("true".equals(sr.getProperty("com.requea.dysoweb.obr.monitor"))) {
				// yes: switch
				fOBRRef = sr;
				fRepo = (RepositoryAdmin)fContext.getService(sr);
				return;
			}
		}
		
		// degrade to non monitored service: get the first one
		ServiceReference sr = srs[0]; 
		fOBRRef = sr;
		fRepo = (RepositoryAdmin)fContext.getService(sr);
		return;
	}
	
	public void stop(BundleContext context) throws Exception {
		fContext = null;
		fDefault = null;
	}

	public static Activator getDefault() {
		return fDefault;
	}

	public BundleContext getContext() {
		return fContext;
	}
	
	public RepositoryAdmin getRepo() {
		return fRepo;
	}
	
	
	public ShellService getShell() {
		return fShell;
	}
	
    private synchronized void initializeShellService()
    {
        if (fShell != null)
        {
            return;
        }
        fShellRef = fContext.getServiceReference(
            org.apache.felix.shell.ShellService.class.getName());
        if (fShellRef == null)
        {
            return;
        }
        fShell = (ShellService) fContext.getService(fShellRef);
    }

	
}
