package org.apache.tomcat;

import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;

public class DefaultInstanceManager implements InstanceManager {

	@Override
	public Object newInstance(Class<?> clazz) throws IllegalAccessException,
			InvocationTargetException, NamingException, InstantiationException {
		return clazz.newInstance();
	}

	@Override
	public Object newInstance(String className) throws IllegalAccessException,
			InvocationTargetException, NamingException, InstantiationException,
			ClassNotFoundException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = this.getClass().getClassLoader();
		}
		Class<?> clazz = cl.loadClass(className);
		return newInstance(clazz);
	}

	@Override
	public Object newInstance(String fqcn, ClassLoader classLoader)
			throws IllegalAccessException, InvocationTargetException,
			NamingException, InstantiationException, ClassNotFoundException {
		Class<?> clazz = classLoader.loadClass(fqcn);
		return newInstance(clazz);
	}

	@Override
	public void newInstance(Object o) throws IllegalAccessException,
			InvocationTargetException, NamingException {

	}

	@Override
	public void destroyInstance(Object o) throws IllegalAccessException,
			InvocationTargetException {


	}

}
