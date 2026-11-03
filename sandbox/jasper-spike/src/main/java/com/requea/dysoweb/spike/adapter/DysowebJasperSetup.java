/*
 * Track-B Phase-1 adapter (proven in the spike; drop into
 * com.requea.dysoweb.processor.jasper when the engine swap lands).
 *
 * Installs everything modern Jasper reads from a bundle's ServletContext before
 * its JspServlet is initialised: the scratch/work dir, the compile classpath, the
 * per-bundle InstanceManager, and the TldCache.
 *
 * IMPORTANT (processor integration): these attributes MUST be bundle-local. Today
 * ServletContextWrapper.getAttribute/setAttribute delegate to the SHARED container
 * ServletContext (fContext), so two web bundles would clobber each other's TldCache.
 * Phase 1 must give ServletContextWrapper a bundle-local attribute map for (at least)
 * these Jasper keys. The spike's ServletContext already stores attributes locally,
 * which is exactly the required behavior.
 */
package com.requea.dysoweb.spike.adapter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import jakarta.servlet.ServletContext;

import org.apache.jasper.compiler.TldCache;
import org.apache.tomcat.InstanceManager;
import org.xml.sax.SAXException;

public class DysowebJasperSetup {

    /** ServletContext attribute Jasper reads for the compile classpath. */
    public static final String JSP_CLASSPATH_ATTR = "org.apache.catalina.jsp_classpath";

    private DysowebJasperSetup() { }

    /**
     * @param ctx          the bundle-local ServletContext (must store attributes locally)
     * @param bundleLoader the bundle's classloader (parent of the generated-JSP loader)
     * @param loaderURLs   the URLs Jasper compiles against -- in the processor this is
     *                     {@code getLoaderURL(IWebProcessor.class.getClassLoader())}
     * @param scratchDir   Jasper work dir -- in the processor this is {@code getScratchDir()}
     * @param tlds         TLDs discovered in the bundle (see TldCacheBuilder.TldSource)
     */
    public static void configure(ServletContext ctx, ClassLoader bundleLoader,
                                 URL[] loaderURLs, File scratchDir,
                                 Collection<TldCacheBuilder.TldSource> tlds)
            throws IOException, SAXException {
        ctx.setAttribute(ServletContext.TEMPDIR, scratchDir);
        ctx.setAttribute(JSP_CLASSPATH_ATTR, classpathString(loaderURLs));
        ctx.setAttribute(InstanceManager.class.getName(),
                new DysowebInstanceManager(bundleLoader));
        ctx.setAttribute(TldCache.SERVLET_CONTEXT_ATTRIBUTE_NAME,
                TldCacheBuilder.build(ctx, bundleLoader, tlds));
    }

    /** Render the loader URLs as a platform classpath string for Jasper's compiler. */
    static String classpathString(URL[] urls) {
        StringBuilder sb = new StringBuilder();
        for (URL u : urls) {
            if (sb.length() > 0) sb.append(File.pathSeparatorChar);
            sb.append(toPath(u));
        }
        return sb.toString();
    }

    private static String toPath(URL u) {
        try {
            return new File(u.toURI()).getAbsolutePath();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return u.getPath();
        }
    }
}
