/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package com.requea.dysoweb.util.xml;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * <p>Wrapper class for resource bundles. Property files are used to store
 * resource strings, which are the only types of resources available.
 * Property files can inherit properties from other files so that
 * a base property file can be used and a small number of properties
 * can be over-ridden by another property file. For example you may
 * create an english version of a resource file named "resource.properties".
 * You then decide that the British English version of all of the properties
 * except one are the same, so there is no need to redefine all of the
 * properties in "resource_en_GB", just the one that is different.</p>
 * <p>The basename is the name of the property file without the ".properties"
 * extension.</p>
 * <p>Properties will be cached for performance.<p>
 * <p>Property values stored in the property files can also contain dynamic
 * variables. Any dynamic variable defined in PropertiesUtil.getVariableValue()
 * can be used (such as {date}), as well as arguments in the form {0}, {1}, etc.
 * Argument values are specified in the various overloaded getString() methods.</p>
 * 
 * @author Richard A. Sitze (rsitze@us.ibm.com)
 * @author Karl Moss (kmoss@macromedia.com)
 * @author Glen Daniels (gdaniels@macromedia.com)
 */
public class ProjectResourceBundle extends ResourceBundle {
//    protected static Logger log =
//        Logger.getLogger(ProjectResourceBundle.class.getName());


    // The static cache of ResourceBundles.
    // The key is the 'basename + locale + default locale'
    // The element is a ResourceBundle object
    private static final Hashtable bundleCache = new Hashtable();

    private static final Locale defaultLocale = Locale.getDefault();

    private final ResourceBundle resourceBundle;
    private final String resourceName;

    
    protected Object handleGetObject(String key)
        throws MissingResourceException
    {
//            return resourceBundle.handleGetObject(key);
        Object obj;
        try {
            obj = resourceBundle.getObject(key);
        } catch (MissingResourceException e) {
            /* catch missing resource, ignore, & return null
             * if this method doesn't return null, then parents
             * are not searched
             */
            obj = null;
        }
        return obj;
    }
    
    public Enumeration getKeys() {
        Enumeration myKeys = resourceBundle.getKeys();
        if (parent == null) {
            return myKeys;
        } else {
            final HashSet set = new HashSet();
            while (myKeys.hasMoreElements()) {
                set.add(myKeys.nextElement());
            }
            
            Enumeration pKeys = parent.getKeys();
            while (pKeys.hasMoreElements()) {
                set.add(pKeys.nextElement());
            }
            
            return new Enumeration() {
                    private Iterator it = set.iterator();
                    public boolean hasMoreElements() { return it.hasNext(); }
                    public Object nextElement() { return it.next(); }
                };
        }
    }
    

    /**
     * Construct a new ProjectResourceBundle
     * 
     * @param projectName The name of the project to which the class belongs.
     *        It must be a proper prefix of the caller's package.
     * 
     * @param caller The calling class.
     *        This is used to get the package name to further construct
     *        the basename as well as to get the proper ClassLoader.
     * 
     * @param resourceName The name of the resource without the
     *        ".properties" extension
     * 
     * @throws MissingResourceException if projectName is not a prefix of
     *         the caller's package name, or if the resource could not be
     *         found/loaded.
     */
    public static ProjectResourceBundle getBundle(String projectName,
                                                  String packageName,
                                                  String resourceName)
        throws MissingResourceException
    {
        return getBundle(projectName, packageName, resourceName, null, null, null);
    }

    /**
     * Construct a new ProjectResourceBundle
     * 
     * @param projectName The name of the project to which the class belongs.
     *        It must be a proper prefix of the caller's package.
     * 
     * @param caller The calling class.
     *        This is used to get the package name to further construct
     *        the basename as well as to get the proper ClassLoader.
     * 
     * @param resourceName The name of the resource without the
     *        ".properties" extension
     * 
     * @throws MissingResourceException if projectName is not a prefix of
     *         the caller's package name, or if the resource could not be
     *         found/loaded.
     */
    public static ProjectResourceBundle getBundle(String projectName,
                                                  Class  caller,
                                                  String resourceName,
                                                  Locale locale)
        throws MissingResourceException
    {
        return getBundle(projectName,
                         caller,
                         resourceName,
                         locale,
                         null);
    }

    /**
     * Construct a new ProjectResourceBundle
     * 
     * @param projectName The name of the project to which the class belongs.
     *        It must be a proper prefix of the caller's package.
     * 
     * @param caller The calling class.
     *        This is used to get the package name to further construct
     *        the basename as well as to get the proper ClassLoader.
     * 
     * @param resourceName The name of the resource without the
     *        ".properties" extension
     * 
     * @param locale The locale
     * 
     * @throws MissingResourceException if projectName is not a prefix of
     *         the caller's package name, or if the resource could not be
     *         found/loaded.
     */
    public static ProjectResourceBundle getBundle(String projectName,
                                                  String packageName,
                                                  String resourceName,
                                                  Locale locale,
                                                  ClassLoader loader)
        throws MissingResourceException
    {
        return getBundle(projectName, packageName, resourceName, locale, loader, null);
    }

    /**
     * Construct a new ProjectResourceBundle
     * 
     * @param projectName The name of the project to which the class belongs.
     *        It must be a proper prefix of the caller's package.
     * 
     * @param caller The calling class.
     *        This is used to get the package name to further construct
     *        the basename as well as to get the proper ClassLoader.
     * 
     * @param resourceName The name of the resource without the
     *        ".properties" extension
     * 
     * @param locale The locale
     * 
     * @param extendsBundle If non-null, then this ExtendMessages will
     *         default to extendsBundle.
     * 
     * @throws MissingResourceException if projectName is not a prefix of
     *         the caller's package name, or if the resource could not be
     *         found/loaded.
     */
    public static ProjectResourceBundle getBundle(String projectName,
                                                  Class  caller,
                                                  String resourceName,
                                                  Locale locale,
                                                  ResourceBundle extendsBundle)
        throws MissingResourceException
    {
        return getBundle(projectName,
                         getPackage(caller.getClass().getName()),
                         resourceName,
                         locale,
                         caller.getClass().getClassLoader(),
                         extendsBundle);
    }

    /**
     * Construct a new ProjectResourceBundle
     * 
     * @param projectName The name of the project to which the class belongs.
     *        It must be a proper prefix of the caller's package.
     * 
     * @param caller The calling class.
     *        This is used to get the package name to further construct
     *        the basename as well as to get the proper ClassLoader.
     * 
     * @param resourceName The name of the resource without the
     *        ".properties" extension
     * 
     * @param locale The locale
     * 
     * @param extendsBundle If non-null, then this ExtendMessages will
     *         default to extendsBundle.
     * 
     * @throws MissingResourceException if projectName is not a prefix of
     *         the caller's package name, or if the resource could not be
     *         found/loaded.
     */
    public static ProjectResourceBundle getBundle(String projectName,
                                                  String packageName,
                                                  String resourceName,
                                                  Locale locale,
                                                  ClassLoader loader,
                                                  ResourceBundle extendsBundle)
        throws MissingResourceException
    {
       
        Context context = new Context();
        context.setLocale(locale);
        context.setLoader(loader);
        context.setProjectName(projectName);
        context.setResourceName(resourceName);
        context.setParentBundle(extendsBundle);

        packageName = context.validate(packageName);

        ProjectResourceBundle bundle = null;
        try {
            bundle = getBundle(context, packageName);
        } catch (RuntimeException e) {
            throw e;
        }
        
        if (bundle == null) {
            throw new MissingResourceException("Cannot find resource '" +
                                               packageName + '.' + resourceName + "'",
                                               resourceName, "");
        }
        
        return bundle;
    }

    /**
     * get bundle...
     * - check cache
     * - try up hierarchy
     * - if at top of hierarchy, use (link to) context.getParentBundle()
     */
    private static synchronized ProjectResourceBundle getBundle(Context context, String packageName)
        throws MissingResourceException
    {
        String cacheKey = context.getCacheKey(packageName);
        
        ProjectResourceBundle prb = (ProjectResourceBundle)bundleCache.get(cacheKey);

        if (prb == null) {
            String name = packageName + '.' + context.getResourceName();
            ResourceBundle rb = context.loadBundle(packageName);
            ResourceBundle parent = context.getParentBundle(packageName);
            
            if (rb != null) {
                prb = new ProjectResourceBundle(name, rb);
                prb.setParent(parent);
            } else {
                if (parent != null) {
                    if (parent instanceof ProjectResourceBundle) {
                        prb = (ProjectResourceBundle)parent;
                    } else {
                        prb = new ProjectResourceBundle(name, parent);
                    }
                }
            }

            if (prb != null) {
                // Cache the resource
                bundleCache.put(cacheKey, prb);
            }
        }

        return prb;
    }

    private static final String getPackage(String name) {
        return name.substring(0, name.lastIndexOf('.')).intern();
    }
    
    /**
      * Construct a new ProjectResourceBundle
      */
    private ProjectResourceBundle(String name, ResourceBundle bundle)
        throws MissingResourceException
    {
        this.resourceBundle = bundle;
        this.resourceName = name;
    }
    
    public String getResourceName() {
        return resourceName;
    }

    public String toString() {
        return resourceName;
    }


    private static class Context {
        private Locale _locale;
        private ClassLoader _loader;
        private String _projectName;
        private String _resourceName;
        private ResourceBundle _parent;
        
        void setLocale(Locale l) {
            /* 1. Docs indicate that if locale is not specified,
             *    then the default local is used in it's place.
             * 2. A null value for locale is invalid.
             * 
             * Therefore, default...
             */
            _locale = (l == null) ? defaultLocale : l;
        }

        void setLoader(ClassLoader l) {
            _loader = (l != null) ? l : this.getClass().getClassLoader();
        }
        
        void setProjectName(String name) { _projectName = name.intern(); }
        void setResourceName(String name) { _resourceName = name.intern(); }
        void setParentBundle(ResourceBundle b) { _parent = b; }
        
//        Locale getLocale() { return _locale; }
//        ClassLoader getLoader() { return _loader; }
//        String getProjectName() { return _projectName; }
        String getResourceName() { return _resourceName; }
//        ResourceBundle getParentBundle() { return _parent; }
    
        String getCacheKey(String packageName)
        {
            String loaderName = (_loader == null) ? "" : (":" + _loader.hashCode());
            return packageName + "." + _resourceName + ":" + _locale + ":" + defaultLocale + loaderName;
        }

        ResourceBundle loadBundle(String packageName)
        {
            try {
                return ResourceBundle.getBundle(packageName + '.' + _resourceName,
                                                _locale,
                                                _loader);
            } catch (MissingResourceException e) {
                // Deliberately surpressing print stack.. just the string for info.
            }
            return null;
        }
    
        ResourceBundle getParentBundle(String packageName)
        {
            ResourceBundle p;
            if (packageName != _projectName) {
                p = getBundle(this, getPackage(packageName));
            } else {
                p = _parent;
                _parent = null;
            }
            return p;
        }
        
        String validate(String packageName)
            throws MissingResourceException
        {
            if (_projectName == null  ||  _projectName.length() == 0) {
                throw new MissingResourceException("Project name not specified",
                                                   "", "");
            }

            if (packageName == null  ||  packageName.length() == 0) {
                throw new MissingResourceException("Package not specified",
                                                   packageName, "");
            }
            packageName = packageName.intern();
    
            /* Ensure that project is a proper prefix of class.
             * Terminate project name with '.' to ensure proper match.
             */
            if (packageName != _projectName  &&  !packageName.startsWith(_projectName + '.')) {
                throw new MissingResourceException("Project '" + _projectName
                                 + "' must be a prefix of Package '"
                                 + packageName + "'",
                                 packageName + '.' + _resourceName, "");
            }
                
            return packageName;
        }
    }
}
