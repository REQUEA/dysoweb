package com.requea.dysoweb.processor.definitions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import com.requea.webenv.IWebProcessor;

public class LoaderWrapper extends ClassLoader {

//    private static Log fLog = LogFactory.getLog(LoaderWrapper.class);
	
	private ClassLoader fLoader;
	private ClassLoader fWebLoader;


	public LoaderWrapper(ClassLoader classLoader) {
		super(IWebProcessor.class.getClassLoader());
		fWebLoader = IWebProcessor.class.getClassLoader();
		fLoader = classLoader;
	}

	public URL getResource(String name) {
		return fLoader.getResource(name);
	}

	public InputStream getResourceAsStream(String name) {
		return fLoader.getResourceAsStream(name);
	}

	public Enumeration getResources(String name) throws IOException {
		return fLoader.getResources(name);
	}

	
	protected synchronized Class loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class c = findLoadedClass(name);
		if (c == null) {
			if (name != null && name.startsWith("com.requea.webenv")){
				try {
					c = fWebLoader.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		if (c == null) {

			try {
				c = fLoader.loadClass(name);
			} catch (ClassNotFoundException e) {
				// If still not found, then invoke findClass in order
				// to find the class.
				c = findClass(name);
			}
		}
		if (resolve) {
			resolveClass(c);
		}
		return c;
	}

}
