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

package com.requea.dysoweb.utils.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.requea.dysoweb.WebAppService;

public class Activator implements BundleActivator {

	private static BundleContext fContext;
	private static Activator fInstance;
	private ServiceRegistration fWebApp;
	
	public void start(BundleContext ctx) throws Exception {
		fContext = ctx;
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
	

	class Service extends WebAppService {

		public Service(Bundle bundle) {
			super(bundle);
		}
		
	}


}
