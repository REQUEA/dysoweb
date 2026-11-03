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

package com.requea.dysoweb.processor.jasper;

import java.lang.reflect.InvocationTargetException;

import org.apache.tomcat.InstanceManager;

/**
 * Per-bundle {@link InstanceManager} for modern Jasper. It instantiates servlets,
 * tag handlers and EL functions through the owning bundle's classloader and does no
 * JNDI/@Resource/@PostConstruct injection (Dysoweb bundles do not use it). Jasper
 * reads this from the ServletContext attribute {@code org.apache.tomcat.InstanceManager}.
 */
public class DysowebInstanceManager implements InstanceManager {

    private final ClassLoader loader;

    public DysowebInstanceManager(ClassLoader bundleLoader) {
        this.loader = bundleLoader;
    }

    public Object newInstance(Class<?> clazz)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException {
        return clazz.getDeclaredConstructor().newInstance();
    }

    public Object newInstance(String className)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException, ClassNotFoundException {
        return newInstance(className, loader);
    }

    public Object newInstance(String className, ClassLoader cl)
            throws IllegalAccessException, InvocationTargetException,
                   InstantiationException, NoSuchMethodException, ClassNotFoundException {
        return cl.loadClass(className).getDeclaredConstructor().newInstance();
    }

    /** Injection hook for an already-constructed instance -- no injection here. */
    public void newInstance(Object o) {
        // no-op
    }

    public void destroyInstance(Object o) {
        // no-op
    }
}
