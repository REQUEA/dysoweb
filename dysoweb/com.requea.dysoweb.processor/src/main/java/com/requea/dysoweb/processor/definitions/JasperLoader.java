package com.requea.dysoweb.processor.definitions;

public class JasperLoader extends LoaderWrapper {

	public JasperLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	protected synchronized Class loadClass(String name, boolean resolve)
		throws ClassNotFoundException {
		try {
			// load from the processor loader 
			return this.getClass().getClassLoader().loadClass(name);
		} catch(ClassNotFoundException e) {
			// load from the wrapper
			return super.loadClass(name, resolve);
		}
	}
}
