package com.requea.dysoweb.processor.definitions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

import com.requea.webenv.IWebProcessor;

public class JasperLoader extends ClassLoader {

	private Bundle fBundle;

	public JasperLoader(Bundle bundle) {
		super(IWebProcessor.class.getClassLoader());
		fBundle = bundle;
	}

	public URL getResource(String name) {
		return fBundle.getResource(name);
	}

	public InputStream getResourceAsStream(String name) {
		return super.getResourceAsStream(name);
	}

	public Enumeration getResources(String name) throws IOException {
		return fBundle.getResources(name);
	}

	
	protected synchronized Class loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class c = findLoadedClass(name);
		if (c == null) {
		    try {
		    	// try to load from the bundle
			    c = fBundle.loadClass(name);
		    } catch (ClassNotFoundException e) {
		    	try {
		    		// try to load from this class loader (common jasper classes)
					c  = this.getClass().getClassLoader().loadClass(name);
		    	} catch(ClassNotFoundException e2) {
			        // If still not found, then invoke findClass in order
			        // to find the class.
			        c = findClass(name);
		    	}
		    }
		}
		if (resolve) {
		    resolveClass(c);
		}
		return c;
	}
}
