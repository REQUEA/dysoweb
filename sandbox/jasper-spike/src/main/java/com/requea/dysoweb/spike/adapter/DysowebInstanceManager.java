/*
 * Track-B Phase-1 adapter (proven in the spike; drop into
 * com.requea.dysoweb.processor.jasper when the engine swap lands).
 *
 * Per-bundle InstanceManager for modern Jasper. It instantiates servlets, tag
 * handlers and EL functions through the OWNING BUNDLE's classloader and does no
 * JNDI/@Resource/@PostConstruct injection -- Dysoweb bundles don't use it, and
 * the vendored DefaultInstanceManager didn't either. Jasper reads this from the
 * ServletContext attribute named org.apache.tomcat.InstanceManager.
 */
package com.requea.dysoweb.spike.adapter;

import java.lang.reflect.InvocationTargetException;

import org.apache.tomcat.InstanceManager;

public class DysowebInstanceManager implements InstanceManager {

    private final ClassLoader loader;

    public DysowebInstanceManager(ClassLoader bundleLoader) {
        this.loader = bundleLoader;
    }

    @Override
    public Object newInstance(Class<?> clazz)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException {
        return clazz.getDeclaredConstructor().newInstance();
    }

    @Override
    public Object newInstance(String className)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException, ClassNotFoundException {
        return newInstance(className, loader);
    }

    @Override
    public Object newInstance(String className, ClassLoader cl)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException, ClassNotFoundException {
        return cl.loadClass(className).getDeclaredConstructor().newInstance();
    }

    /** Injection hook for an already-constructed instance -- no injection here. */
    @Override
    public void newInstance(Object o) {
        // no-op
    }

    @Override
    public void destroyInstance(Object o) {
        // no-op
    }
}
