package com.requea.dysoweb.service.obr;

import org.osgi.service.obr.Resolver;

public interface MonitoredResolver extends Resolver {
	
	public void deploy(boolean resolver, IProgressMonitor monitor);
	
}
