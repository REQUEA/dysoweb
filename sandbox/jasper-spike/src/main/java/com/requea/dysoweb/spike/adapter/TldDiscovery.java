/*
 * Track-B Phase-1 adapter (proven in the spike; drop into
 * com.requea.dysoweb.processor.jasper when the engine swap lands).
 *
 * The OSGi-side TLD DISCOVERY half that feeds TldCacheBuilder. In OSGi there is no
 * Catalina TldScanner run, so we enumerate a bundle's TLDs ourselves with bundle
 * APIs -- NOT Catalina's JarScanner. This modernizes the fork's existing
 * RequestProcessor.processTLDinBundleClassPath: same conventions (web content under
 * the "/webapp" prefix; Bundle-ClassPath tokenized by ','), but it emits
 * TldCacheBuilder.TldSource records for the modern org.apache.jasper.compiler.TldCache
 * instead of populating the fork's internal TldLocationsCache.
 *
 * Two TLD locations are covered:
 *   1. Loose TLDs under /webapp/WEB-INF/**.tld           (bundle.findEntries)
 *   2. TLDs inside embedded jars on Bundle-ClassPath      (bundle.getEntry + zip scan)
 *
 * Embedded jars are MATERIALIZED to a work dir so a jar:file:...!/ URL can be opened
 * uniformly. (A raw bundle:// URL is not reliably openable as a nested jar across
 * frameworks, so we copy the bytes out once -- this is the robust cross-framework path.)
 */
package com.requea.dysoweb.spike.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.Bundle;

public class TldDiscovery {

    /** Dysoweb bundles root their web content under this prefix (see RequestProcessor). */
    public static final String WEBAPP_PREFIX = "/webapp";

    private TldDiscovery() { }

    /**
     * @param bundle  the web bundle to scan
     * @param workDir where embedded jars are materialized (in the processor: getScratchDir())
     */
    public static List<TldCacheBuilder.TldSource> discover(Bundle bundle, File workDir)
            throws IOException {
        List<TldCacheBuilder.TldSource> out = new ArrayList<>();
        discoverLooseTlds(bundle, out);
        discoverJarTlds(bundle, workDir, out);
        return out;
    }

    /** 1) Loose TLDs under /webapp/WEB-INF (recursive). */
    static void discoverLooseTlds(Bundle bundle, List<TldCacheBuilder.TldSource> out) {
        Enumeration<URL> e = bundle.findEntries(WEBAPP_PREFIX + "/WEB-INF", "*.tld", true);
        while (e != null && e.hasMoreElements()) {
            URL u = e.nextElement();
            out.add(TldCacheBuilder.TldSource.webInf(u, webappPathOf(u)));
        }
    }

    /** 2) TLDs inside embedded jars listed on Bundle-ClassPath. */
    static void discoverJarTlds(Bundle bundle, File workDir, List<TldCacheBuilder.TldSource> out)
            throws IOException {
        Object classPath = bundle.getHeaders().get("Bundle-ClassPath");
        if (classPath == null) {
            return;
        }
        File jarDir = new File(workDir, "tld-jars");
        jarDir.mkdirs();
        StringTokenizer st = new StringTokenizer(classPath.toString(), ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (!token.endsWith(".jar")) {
                continue;
            }
            URL jarEntry = bundle.getEntry(token);
            if (jarEntry == null) {
                continue;
            }
            File local = new File(jarDir, token.replace('/', '_').replace('\\', '_'));
            materialize(jarEntry, local);
            // Plain jar file URL: TldResourcePath/JarFactory adds the jar:...!/<entry> wrapper
            // itself. (Passing a pre-wrapped jar:...!/ URL makes JarFactory emit a war: URL whose
            // handler isn't registered outside a running Tomcat.)
            URL jarUrl = local.toURI().toURL();
            for (String tldEntry : scanJarForTlds(local)) {
                // webappPath MUST be null for a jar TLD: it is not a webapp resource, and Jasper
                // resolves it by its <uri> (parsed from the TaglibXml). A non-null path would make
                // TldCache.getLastModified call ServletContext.getResource(path) -> NPE.
                out.add(TldCacheBuilder.TldSource.inJar(jarUrl, null, tldEntry));
            }
        }
    }

    /** Normalize a bundle-entry URL to the webapp-relative path (strip the /webapp prefix). */
    static String webappPathOf(URL u) {
        String path = u.getPath();
        int i = path.indexOf("/WEB-INF/");
        return i >= 0 ? path.substring(i) : path;
    }

    static List<String> scanJarForTlds(File jar) throws IOException {
        List<String> tlds = new ArrayList<>();
        try (InputStream is = new java.io.FileInputStream(jar);
             ZipInputStream zip = new ZipInputStream(is)) {
            for (ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()) {
                String name = ze.getName();
                if (!ze.isDirectory()
                        && name.startsWith("META-INF/")
                        && name.toLowerCase().endsWith(".tld")) {
                    tlds.add(name);
                }
            }
        }
        return tlds;
    }

    static void materialize(URL src, File dst) throws IOException {
        if (dst.exists()) {
            return;
        }
        try (InputStream in = src.openStream(); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > -1) {
                out.write(buf, 0, n);
            }
        }
    }
}
