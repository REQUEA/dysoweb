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

package com.requea.dysoweb.processor.definitions;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;

import com.requea.dysoweb.WebAppException;
import com.requea.dysoweb.processor.IListenerDefinition;

public class ListenerDefinition implements IListenerDefinition {

    private static Log fLog = LogFactory.getLog(ListenerDefinition.class);
	
	
	private String fClassName;
	private Object fInstance;


	private boolean bContextNotified;


	private long fBundleId;


	private ClassLoader fLoader;

	public ListenerDefinition(long bundleId, String className) {
		fBundleId = bundleId;
		fClassName = className;
	}

	public long getBundleId() {
		return fBundleId;
	}
	
	public String getClassName() {
		return fClassName;
	}
	
	public ClassLoader getLoader() {
		return fLoader;
	}

	public synchronized void loadClass(Bundle bundle) throws WebAppException {
		if(fClassName == null) {
			fLoader = null;
		} else {
			Class cls;
			try {
				cls = bundle.loadClass(fClassName);
			} catch (ClassNotFoundException e) {
				throw new WebAppException(e);
			}
			fLoader = new LoaderWrapper(cls.getClassLoader());
		}
	}
	
	public Object getInstance() {
		return fInstance;
	}

	public void load() throws WebAppException {
		if(fInstance != null) {
			// already loaded
			return;
		}
		
		fLog.info("Initializing Servlet Listener " + fClassName);
		
		
		try {
			// instantiate the listener
			Class cls = fLoader.loadClass(fClassName);
			Object obj = cls.newInstance();
			if(obj instanceof ServletContextListener) {
				fInstance = obj;
			} else if(obj instanceof ServletContextAttributeListener) {
				fInstance = obj;
			} else if(obj instanceof HttpSessionListener) {
				fInstance = obj;
			} else if(obj instanceof HttpSessionAttributeListener) {
				fInstance = obj;
			} else {
				throw new WebAppException("The class "+fClassName+" is not a ServletContextListener, ServletContextAttributeListener, HttpSessionListener or HttpSessionAttributeListener. It cannot be used as a Servlet context listener");
			}
		} catch(ClassNotFoundException e) {
			throw new WebAppException(e);
		} catch (InstantiationException e) {
			throw new WebAppException(e);
		} catch (IllegalAccessException e) {
			throw new WebAppException(e);
		}
	}

	public void unload() {
		fLog.info("Unloading Servlet Context Listener " + fClassName);
		fInstance = null;
	}

	public boolean isContextNotified() {
		return bContextNotified;
	}

	public void setContextNotified(boolean b) {
		bContextNotified = b;
	}

}
