package com.requea.dysoweb.xmpp.impl;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;


public class Activator implements BundleActivator {

	private XMPPShell fMessenger;
	private BundleContext fContext;
    private ServiceReference fShellRef = null;
    private ShellService fShell = null;

	public void start(BundleContext context) throws Exception {
		fContext = context;
        
        // Listen for registering/unregistering impl service.
        ServiceListener sl = new ServiceListener() {
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
                        initializeService();
                    }
                    // Unget the service if it is unregistering.
                    else if ((event.getType() == ServiceEvent.UNREGISTERING)
                        && event.getServiceReference().equals(fShellRef))
                    {
                        fContext.ungetService(fShellRef);
                        fShellRef = null;
                        fShell = null;
                        // Try to get another service.
                        initializeService();
                    }
                }
            }
        };
        try
        {
        	fContext.addServiceListener(sl,
                "(objectClass="
                + org.apache.felix.shell.ShellService.class.getName()
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("XMPP Shell: Cannot add service listener.");
            System.err.println("XMPP Shell: " + ex);
        }
        // a shell service may already be available
        initializeService();
        
	}

	private synchronized void initializeService()
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

        // stop the previous one if any
		if(fMessenger != null) {
			fMessenger.stop();
			fMessenger = null;
		}
        
		// register the shell command as a service for the shell
		fMessenger = new XMPPShell(fShell);
		fMessenger.start();
        fContext.registerService(
            org.apache.felix.shell.Command.class.getName(),
            new XMPPCommandImpl(fMessenger), null);
        
    }

	

	public void stop(BundleContext context) throws Exception {
		if(fMessenger != null) {
			fMessenger.stop();
			fMessenger = null;
		}
	}

}
