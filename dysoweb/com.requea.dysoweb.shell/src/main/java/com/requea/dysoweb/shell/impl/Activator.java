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

package com.requea.dysoweb.shell.impl;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.requea.dysoweb.WebAppService;

public class Activator implements BundleActivator {

	private static BundleContext fContext;
	private static Activator fInstance;
	private ServiceRegistration fWebApp;
	private ServiceReference fShellRef;
	private ShellService fShell;
	
	public void start(BundleContext ctx) throws Exception {
		fContext = ctx;
		
		// register the shell service
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
            System.err.println("ShellTui: Cannot add service listener.");
            System.err.println("ShellTui: " + ex);
        }

        // Now try to manually initialize the impl service
        // since one might already be available.
        initializeService();		
		
		fWebApp = ctx.registerService(WebAppService.class.getName(), 
				new Service(ctx.getBundle()), 
				null);
		
		fInstance = this;
	}

	
	public void stop(BundleContext ctx) throws Exception {
		fInstance = null;
		fContext = null;
		// unregister the web app service
		fWebApp.unregister();
		fWebApp = null;
	}
	
	public static Activator getInstance() {
		return fInstance;
	}
	
	/**
	 * Retrieve the current context
	 * @return
	 */
	public BundleContext getContext() {
		return fContext;
	}
	
	public ShellService getShell() {
		return fShell;
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
    }
	
	class Service extends WebAppService {

		public Service(Bundle bundle) {
			super(bundle);
		}
		
	}


}
