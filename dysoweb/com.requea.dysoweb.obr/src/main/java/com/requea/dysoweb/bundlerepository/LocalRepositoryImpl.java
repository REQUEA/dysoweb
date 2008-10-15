/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.requea.dysoweb.bundlerepository;

import java.net.URL;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;

public class LocalRepositoryImpl implements Repository
{
    private BundleContext m_context = null;
    private final Logger m_logger;
    private long m_currentTimeStamp = 0;
    private long m_snapshotTimeStamp = 0;
    private List m_localResourceList = new ArrayList();
    private BundleListener m_bundleListener = null;

    public LocalRepositoryImpl(BundleContext context, Logger logger)
    {
        m_context = context;
        m_logger = logger;
        initialize();
    }

    public void dispose()
    {
        m_context.removeBundleListener(m_bundleListener);
    }

    public URL getURL()
    {
        return null;
    }

    public String getName()
    {
        return "Locally Installed Repository";
    }

    public synchronized long getLastModified()
    {
        return m_snapshotTimeStamp;
    }

    public synchronized long getCurrentTimeStamp()
    {
        return m_currentTimeStamp;
    }

    public synchronized Resource[] getResources()
    {
        return (Resource[]) m_localResourceList.toArray(new Resource[m_localResourceList.size()]);
    }

    private void initialize()
    {
        // Create a bundle listener to list for events that
        // change the state of the framework.
        m_bundleListener = new SynchronousBundleListener() {
            public void bundleChanged(BundleEvent event)
            {
                synchronized (LocalRepositoryImpl.this)
                {
                    m_currentTimeStamp = new Date().getTime();
                }
            }
        };
        m_context.addBundleListener(m_bundleListener);

        // Generate the resource list from the set of installed bundles.
        // Lock so we can ensure that no bundle events arrive before we 
        // are done getting our state snapshot.
        Bundle[] bundles = null;
        synchronized (this)
        {
            m_snapshotTimeStamp = m_currentTimeStamp = new Date().getTime();
            bundles = m_context.getBundles();
        }

        // Create a local resource object for each bundle, which will
        // convert the bundle headers to the appropriate resource metadata.
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            try
            {
                m_localResourceList.add(new LocalResourceImpl(bundles[i], m_logger));
            }
            catch (InvalidSyntaxException ex)
            {
                // This should never happen since we are generating filters,
                // but ignore the resource if it does occur.
                m_logger.log(Logger.LOG_WARNING, ex.getMessage(), ex);
            }
        }
    }

    public static class LocalResourceImpl extends ResourceImpl
    {
        private Bundle m_bundle = null;

        LocalResourceImpl(Bundle bundle, Logger logger) throws InvalidSyntaxException
        {
            this(null, bundle, logger);
        }

        LocalResourceImpl(ResourceImpl resource, Bundle bundle, Logger logger)
            throws InvalidSyntaxException
        {
            super(resource);
            m_bundle = bundle;
            initialize();
        }

        public Bundle getBundle()
        {
            return m_bundle;
        }

        private void initialize() throws InvalidSyntaxException
        {
            Dictionary dict = m_bundle.getHeaders();

            // Convert bundle manifest header attributes to resource properties.
            convertAttributesToProperties(dict);
            
            // Convert import package declarations into requirements.
            convertImportPackageToRequirement(dict);

            // Convert import service declarations into requirements.
            convertImportServiceToRequirement(dict);

            // Convert export package declarations into capabilities.
            convertExportPackageToCapability(dict);

            // Convert export service declarations into capabilities.
            convertExportServiceToCapability(dict);

            // For the system bundle, add a special platform capability.
            if (m_bundle.getBundleId() == 0)
            {
/* TODO: OBR - Fix system capabilities.
                // Create a case-insensitive map.
                Map map = new TreeMap(new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        return o1.toString().compareToIgnoreCase(o2.toString());
                    }
                });
                map.put(
                    Constants.FRAMEWORK_VERSION,
                    m_context.getProperty(Constants.FRAMEWORK_VERSION));
                map.put(
                    Constants.FRAMEWORK_VENDOR,
                    m_context.getProperty(Constants.FRAMEWORK_VENDOR));
                map.put(
                    Constants.FRAMEWORK_LANGUAGE,
                    m_context.getProperty(Constants.FRAMEWORK_LANGUAGE));
                map.put(
                    Constants.FRAMEWORK_OS_NAME,
                    m_context.getProperty(Constants.FRAMEWORK_OS_NAME));
                map.put(
                    Constants.FRAMEWORK_OS_VERSION,
                    m_context.getProperty(Constants.FRAMEWORK_OS_VERSION));
                map.put(
                    Constants.FRAMEWORK_PROCESSOR,
                    m_context.getProperty(Constants.FRAMEWORK_PROCESSOR));
//                map.put(
//                    FelixConstants.FELIX_VERSION_PROPERTY,
//                    m_context.getProperty(FelixConstants.FELIX_VERSION_PROPERTY));
                Map[] capMaps = (Map[]) bundleMap.get("capability");
                if (capMaps == null)
                {
                    capMaps = new Map[] { map };
                }
                else
                {
                    Map[] newCaps = new Map[capMaps.length + 1];
                    newCaps[0] = map;
                    System.arraycopy(capMaps, 0, newCaps, 1, capMaps.length);
                    capMaps = newCaps;
                }
                bundleMap.put("capability", capMaps);
*/
            }
        }

        private void convertAttributesToProperties(Dictionary dict)
        {
            for (Enumeration keys = dict.keys(); keys.hasMoreElements(); )
            {
                String key = (String) keys.nextElement();
                if (key.equalsIgnoreCase(Constants.BUNDLE_SYMBOLICNAME))
                {
                    put(Resource.SYMBOLIC_NAME, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase(Constants.BUNDLE_NAME))
                {
                    put(Resource.PRESENTATION_NAME, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase(Constants.BUNDLE_VERSION))
                {
                    put(Resource.VERSION, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase("Bundle-Source"))
                {
                    put(Resource.SOURCE_URL, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase(Constants.BUNDLE_DESCRIPTION))
                {
                    put(Resource.DESCRIPTION, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase(Constants.BUNDLE_DOCURL))
                {
                    put(Resource.DOCUMENTATION_URL, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase(Constants.BUNDLE_COPYRIGHT))
                {
                    put(Resource.COPYRIGHT, (String) dict.get(key));
                }
                else if (key.equalsIgnoreCase("Bundle-License"))
                {
                    put(Resource.LICENSE_URL, (String) dict.get(key));
                }
            }
        }

        private void convertImportPackageToRequirement(Dictionary dict)
            throws InvalidSyntaxException
        {
            String target = (String) dict.get(Constants.IMPORT_PACKAGE);
            if (target != null)
            {
                R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
                R4Import[] imports = new R4Import[pkgs.length];
                for (int i = 0; i < pkgs.length; i++)
                {
                    imports[i] = new R4Import(pkgs[i]);
                }

                for (int impIdx = 0; impIdx < imports.length; impIdx++)
                {
                    RequirementImpl req = new RequirementImpl();
                    req.setMultiple("false");
                    req.setOptional(Boolean.toString(imports[impIdx].isOptional()));
                    req.setName("package");
                    req.addText("Import package " + imports[impIdx].toString());
                    
                    String low = imports[impIdx].isLowInclusive()
                                ? "(version>=" + imports[impIdx].getVersion() + ")"
                                : "(!(version<=" + imports[impIdx].getVersion() + "))";

                    if (imports[impIdx].getVersionHigh() != null)
                    {
                        String high = imports[impIdx].isHighInclusive()
                            ? "(version<=" + imports[impIdx].getVersionHigh() + ")"
                            : "(!(version>=" + imports[impIdx].getVersionHigh() + "))";
                        req.setFilter("(&(package="
                            + imports[impIdx].getName() + ")"
                            + low + high + ")");
                    }
                    else
                    {
                        req.setFilter(
                            "(&(package="
                            + imports[impIdx].getName() + ")"
                            + low + ")");
                    }

                    addRequire(req);
                }
            }
        }

        private void convertImportServiceToRequirement(Dictionary dict)
            throws InvalidSyntaxException
        {
            String target = (String) dict.get(Constants.IMPORT_SERVICE);
            if (target != null)
            {
                R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
                for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
                {
                    RequirementImpl req = new RequirementImpl();
                    req.setMultiple("false");
                    req.setName("service");
                    req.addText("Import service " + pkgs[pkgIdx].toString());
                    req.setFilter("(service="
                        + pkgs[pkgIdx].getName() + ")");
                    addRequire(req);
                }
            }
        }

        private void convertExportPackageToCapability(Dictionary dict)
        {
            String target = (String) dict.get(Constants.EXPORT_PACKAGE);
            if (target != null)
            {
                R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
                for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
                {
                    CapabilityImpl cap = new CapabilityImpl();
                    cap.setName("package");
                    cap.addP(new PropertyImpl("package", null, pkgs[pkgIdx].getName()));
                    cap.addP(new PropertyImpl("version", "version", pkgs[pkgIdx].getVersion().toString()));
                    addCapability(cap);
                }
            }
        }

        private void convertExportServiceToCapability(Dictionary dict)
        {
            String target = (String) dict.get(Constants.EXPORT_SERVICE);
            if (target != null)
            {
                R4Package[] pkgs = R4Package.parseImportOrExportHeader(target);
                for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
                {
                    CapabilityImpl cap = new CapabilityImpl();
                    cap.setName("service");
                    cap.addP(new PropertyImpl("service", null, pkgs[pkgIdx].getName()));
                    addCapability(cap);
                }
            }
        }
    }
}