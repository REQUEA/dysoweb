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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;

import jakarta.servlet.ServletContext;

import org.apache.jasper.compiler.TldCache;
import org.apache.tomcat.InstanceManager;

/**
 * Installs everything modern Jasper reads from a bundle's ServletContext before its
 * JspServlet is initialised: the scratch/work dir, the compile classpath, the per-bundle
 * InstanceManager, and the TldCache.
 *
 * <p>These attributes must be bundle-local: {@code ServletContextWrapper} keeps the
 * corresponding keys in a per-bundle map rather than the shared container context, so two
 * web bundles cannot clobber each other's Jasper state.
 */
public class DysowebJasperSetup {

    /** ServletContext attribute Jasper reads for the compile classpath. */
    public static final String JSP_CLASSPATH_ATTR = "org.apache.catalina.jsp_classpath";

    private DysowebJasperSetup() { }

    /**
     * @param ctx          the bundle-local ServletContext (stores the Jasper keys locally)
     * @param bundleLoader the bundle's classloader (parent of the generated-JSP loader)
     * @param loaderURLs   the URLs Jasper compiles against (RequestProcessor.getLoaderURL)
     * @param scratchDir   Jasper work dir (RequestProcessor.getScratchDir)
     * @param tldLocator   maps a taglib uri to its TLD file, platform-wide (see {@link DysowebTldCache})
     */
    public static void configure(ServletContext ctx, ClassLoader bundleLoader,
                                 URL[] loaderURLs, File scratchDir,
                                 Function<String, File> tldLocator) {
        ctx.setAttribute(ServletContext.TEMPDIR, scratchDir);
        ctx.setAttribute(JSP_CLASSPATH_ATTR, classpathString(loaderURLs));
        ctx.setAttribute(InstanceManager.class.getName(),
                new DysowebInstanceManager(bundleLoader));
        ctx.setAttribute(TldCache.SERVLET_CONTEXT_ATTRIBUTE_NAME,
                new DysowebTldCache(ctx, bundleLoader, tldLocator));
    }

    /** Render the loader URLs as a platform classpath string for Jasper's compiler. */
    static String classpathString(URL[] urls) {
        StringBuilder sb = new StringBuilder();
        for (URL u : urls) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(toPath(u));
        }
        return sb.toString();
    }

    private static String toPath(URL u) {
        try {
            return new File(u.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            return u.getPath();
        } catch (IllegalArgumentException e) {
            return u.getPath();
        }
    }
}
