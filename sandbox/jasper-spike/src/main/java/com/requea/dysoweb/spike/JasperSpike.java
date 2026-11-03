/*
 * Track-B Phase-0 spike (throwaway). Drives the REAL modern Jasper
 * (org.apache.jasper.servlet.JspServlet, tomcat-jasper 10.1) to compile and
 * execute a taglib-free JSP, where the ServletContext's ONLY resource access is
 * getResource / getResourceAsStream (bundle-entry style) -- NO javax.naming
 * DirContext, NO Catalina WebResourceRoot. The compiler is ecj.
 *
 * If this prints "SPIKE RESULT: PASS", the plan's core thesis holds: modern
 * Jasper needs a ServletContext + a classloader + an InstanceManager, not the
 * DirContext resource layer -- so Track B and the DirContext rewrite (Track C)
 * are separable.
 */
package com.requea.dysoweb.spike;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspFactory;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;

import com.requea.dysoweb.spike.adapter.DysowebJasperSetup;
import com.requea.dysoweb.spike.adapter.TldCacheBuilder;
import com.requea.dysoweb.spike.adapter.TldDiscovery;

public class JasperSpike {

    /** A method body for the proxy dispatcher; may throw anything. */
    interface Fn { Object apply(Object[] args) throws Throwable; }

    /** Name-dispatched InvocationHandler with type-correct defaults for the rest. */
    static final class H implements InvocationHandler {
        private final String tag;
        private final Map<String, Fn> m = new HashMap<>();
        H(String tag) { this.tag = tag; }
        H on(String name, Fn f) { m.put(name, f); return this; }
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Fn f = m.get(method.getName());
            if (f != null) return f.apply(args);
            switch (method.getName()) {
                case "toString": return tag + "@proxy";
                case "hashCode": return System.identityHashCode(proxy);
                case "equals":   return proxy == (args == null ? null : args[0]);
                default:         return defaultFor(method.getReturnType());
            }
        }
    }

    static Object defaultFor(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return false;
        if (t == void.class)    return null;
        if (t == char.class)    return (char) 0;
        if (t == long.class)    return 0L;
        if (t == int.class)     return 0;
        if (t == short.class)   return (short) 0;
        if (t == byte.class)    return (byte) 0;
        if (t == double.class)  return 0d;
        if (t == float.class)   return 0f;
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> iface, H h) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, h);
    }

    public static void main(String[] args) throws Throwable {
        File base = new File(System.getProperty("spike.base", ".")).getCanonicalFile();
        File webroot = new File(base, "webroot");
        File scratch = new File(base, "target/scratch");
        scratch.mkdirs();
        String cp = System.getProperty("java.class.path");
        final ClassLoader appLoader = JasperSpike.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(appLoader);

        System.out.println("webroot = " + webroot + "  (hello.jsp exists=" + new File(webroot, "hello.jsp").exists() + ")");
        System.out.println("scratch = " + scratch);

        // Belt-and-suspenders: modern Jasper registers this in a static block, but ensure it.
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(new org.apache.jasper.runtime.JspFactoryImpl());
        }

        // Bundle-LOCAL attribute store (the processor's ServletContextWrapper must do the
        // same for the Jasper keys instead of delegating to the shared container context).
        Map<String, Object> attrs = new ConcurrentHashMap<>();
        ServletContext ctx = proxy(ServletContext.class, ctxHandler(webroot, attrs, appLoader));

        // ---- Everything below is the REAL Phase-1 wiring, via the drop-in adapters ----
        // In the processor: loaderURLs = getLoaderURL(IWebProcessor.class.getClassLoader()),
        // scratch = getScratchDir(), bundleLoader = new JasperLoader(bundle), and `tlds` come
        // from enumerating the bundle's entries + embedded jars + web.xml taglib mappings.
        URL[] loaderURLs = classpathToUrls(cp);

        // Build an embedded jar carrying a TLD in META-INF (the OSGi-specific case), then
        // discover ALL of the bundle's TLDs via the real Bundle API -- loose /webapp/WEB-INF
        // TLDs AND the embedded-jar one -- exactly as the processor will.
        File embeddedJar = new File(base, "target/embed/tags.jar");
        buildTldJar(embeddedJar, "META-INF/embed.tld", EMBED_TLD_XML);
        Bundle bundle = fakeBundle(webroot, embeddedJar, "WEB-INF/lib/tags.jar");
        List<TldCacheBuilder.TldSource> tlds = TldDiscovery.discover(bundle, scratch);
        System.out.println("TldDiscovery found " + tlds.size() + " TLD(s):");
        for (TldCacheBuilder.TldSource s : tlds) {
            System.out.println("  - webappPath=" + s.webappPath + (s.entryName != null ? "  (in jar: " + s.entryName + ")" : ""));
        }

        DysowebJasperSetup.configure(ctx, appLoader, loaderURLs, scratch, tlds);
        System.out.println("Phase-1 adapters configured: tempdir + jsp_classpath + InstanceManager + TldCache");

        // ---- JspServlet init params (options come from the ServletConfig) ----
        Map<String, String> ip = new LinkedHashMap<>();
        ip.put("development", "false");
        ip.put("fork", "false");
        ip.put("classpath", cp);
        ip.put("scratchdir", scratch.getAbsolutePath());
        ip.put("compilerSourceVM", "17");
        ip.put("compilerTargetVM", "17");
        ServletConfig cfg = proxy(ServletConfig.class, new H("cfg")
                .on("getServletName", a -> "jsp")
                .on("getServletContext", a -> ctx)
                .on("getInitParameter", a -> ip.get(a[0]))
                .on("getInitParameterNames", a -> Collections.enumeration(ip.keySet())));

        Servlet jsp = (Servlet) Class.forName("org.apache.jasper.servlet.JspServlet")
                .getDeclaredConstructor().newInstance();
        jsp.init(cfg);
        System.out.println("JspServlet initialised: " + jsp.getClass().getName());

        // ---- Drive both a plain JSP and a custom-tag JSP ----
        boolean plainOk = check("/hello.jsp", render(jsp, ctx, "/hello.jsp"),
                "SPIKE_OK", "result=42", "loop=10");
        boolean tagOk = check("/hellotag.jsp", render(jsp, ctx, "/hellotag.jsp"),
                "SPIKE_TAG_OK", "TAG[name=dysoweb]");
        boolean embedOk = check("/embedtag.jsp", render(jsp, ctx, "/embedtag.jsp"),
                "SPIKE_EMBED_OK", "EMBED[x=jar]");

        boolean ok = plainOk && tagOk && embedOk;
        System.out.println("SPIKE RESULT: " + (ok
                ? "PASS (plain JSP + WEB-INF tag + embedded-jar tag; TLDs found via TldDiscovery over the OSGi Bundle API)"
                : "FAIL"));
        System.exit(ok ? 0 : 1);
    }

    /** Service one request for {@code path} against a getResource-only ServletContext. */
    static String render(Servlet jsp, ServletContext ctx, String path) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        HttpSession session = proxy(HttpSession.class, new H("session")
                .on("getId", a -> "spikeSession")
                .on("getServletContext", a -> ctx)
                .on("isNew", a -> true));
        HttpServletRequest req = proxy(HttpServletRequest.class, new H("req")
                .on("getServletPath", a -> path)
                .on("getRequestURI", a -> path)
                .on("getPathInfo", a -> null)
                .on("getQueryString", a -> null)
                .on("getMethod", a -> "GET")
                .on("getProtocol", a -> "HTTP/1.1")
                .on("getScheme", a -> "http")
                .on("getContextPath", a -> "")
                .on("getServletContext", a -> ctx)
                .on("getDispatcherType", a -> DispatcherType.REQUEST)
                .on("getLocale", a -> Locale.getDefault())
                .on("getLocales", a -> Collections.enumeration(List.of(Locale.getDefault())))
                .on("getAttribute", a -> null)
                .on("getAttributeNames", a -> Collections.enumeration(Collections.emptyList()))
                .on("getParameter", a -> null)
                .on("getSession", a -> session)
                .on("getRequestDispatcher", a -> null));
        HttpServletResponse res = proxy(HttpServletResponse.class, new H("res")
                .on("getWriter", a -> pw)
                .on("isCommitted", a -> false)
                .on("getBufferSize", a -> 8192)
                .on("flushBuffer", a -> { pw.flush(); return null; })
                .on("getCharacterEncoding", a -> "UTF-8")
                .on("getContentType", a -> "text/plain")
                .on("getLocale", a -> Locale.getDefault())
                .on("containsHeader", a -> false)
                .on("encodeURL", a -> a[0])
                .on("encodeRedirectURL", a -> a[0]));
        try {
            jsp.service(req, res);
            pw.flush();
        } catch (Throwable t) {
            pw.flush();
            return sw + "\n<<EXCEPTION>> " + stack(t);
        }
        return sw.toString();
    }

    static boolean check(String path, String out, String... expects) {
        System.out.println("----- " + path + " -----");
        System.out.println(out);
        boolean ok = true;
        for (String e : expects) if (!out.contains(e)) { ok = false; System.out.println("  MISSING: " + e); }
        System.out.println("  => " + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    static String stack(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** ServletContext whose resource access is getResource / getRealPath ONLY. */
    static H ctxHandler(File webroot, Map<String, Object> attrs, ClassLoader loader) {
        return new H("ctx")
                .on("getResource", a -> {
                    File f = resolve(webroot, (String) a[0]);
                    return f.exists() ? f.toURI().toURL() : null;
                })
                .on("getResourceAsStream", a -> {
                    File f = resolve(webroot, (String) a[0]);
                    try { return f.exists() ? new FileInputStream(f) : null; }
                    catch (Exception e) { return null; }
                })
                .on("getResourcePaths", a -> {
                    File dir = resolve(webroot, (String) a[0]);
                    java.util.Set<String> s = new java.util.HashSet<>();
                    File[] kids = dir.listFiles();
                    if (kids != null) for (File k : kids) s.add((k.isDirectory() ? k.getName() + "/" : k.getName()));
                    return s;
                })
                .on("getRealPath", a -> resolve(webroot, (String) a[0]).getAbsolutePath())
                .on("getMimeType", a -> null)
                .on("getInitParameter", a -> null)
                .on("getInitParameterNames", a -> Collections.enumeration(Collections.emptyList()))
                .on("getAttribute", a -> attrs.get(a[0]))
                .on("setAttribute", a -> { attrs.put((String) a[0], a[1]); return null; })
                .on("removeAttribute", a -> { attrs.remove(a[0]); return null; })
                .on("getAttributeNames", a -> Collections.enumeration(attrs.keySet()))
                .on("getContextPath", a -> "")
                .on("getServletContextName", a -> "spike")
                .on("getServerInfo", a -> "JasperSpike/1.0")
                .on("getMajorVersion", a -> 6)
                .on("getMinorVersion", a -> 0)
                .on("getEffectiveMajorVersion", a -> 6)
                .on("getEffectiveMinorVersion", a -> 0)
                .on("getVirtualServerName", a -> "spike")
                .on("getSessionTimeout", a -> 30)
                .on("getRequestCharacterEncoding", a -> "UTF-8")
                .on("getResponseCharacterEncoding", a -> "UTF-8")
                .on("getClassLoader", a -> loader)
                .on("getJspConfigDescriptor", a -> null)
                .on("log", a -> { System.out.println("[ctx.log] " + a[0]); if (a.length > 1 && a[1] instanceof Throwable) ((Throwable) a[1]).printStackTrace(System.out); return null; });
    }

    static File resolve(File webroot, String path) {
        if (path == null) path = "/";
        if (path.startsWith("/")) path = path.substring(1);
        return new File(webroot, path);
    }

    /** Split a platform classpath string into file URLs for the compile-classpath adapter. */
    static URL[] classpathToUrls(String cp) throws Exception {
        String[] parts = cp.split(java.util.regex.Pattern.quote(File.pathSeparator));
        URL[] urls = new URL[parts.length];
        for (int i = 0; i < parts.length; i++) urls[i] = new File(parts[i]).toURI().toURL();
        return urls;
    }

    static final String EMBED_TLD_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<taglib xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" version=\"3.1\">\n"
            + "  <tlib-version>1.0</tlib-version>\n"
            + "  <short-name>embed</short-name>\n"
            + "  <uri>http://requea.com/embed</uri>\n"
            + "  <tag>\n"
            + "    <name>shout</name>\n"
            + "    <tag-class>com.requea.dysoweb.spike.tags.EmbedTag</tag-class>\n"
            + "    <body-content>empty</body-content>\n"
            + "    <attribute>\n"
            + "      <name>x</name>\n"
            + "      <required>true</required>\n"
            + "      <rtexprvalue>true</rtexprvalue>\n"
            + "    </attribute>\n"
            + "  </tag>\n"
            + "</taglib>\n";

    /** Write a jar containing a single TLD entry (plus a manifest). */
    static void buildTldJar(File dest, String entryName, String tldXml) throws IOException {
        dest.getParentFile().mkdirs();
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(dest), mf)) {
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(tldXml.getBytes("UTF-8"));
            jos.closeEntry();
        }
    }

    /**
     * A stand-in org.osgi.framework.Bundle backed by a webapp dir + one embedded jar, exposing
     * exactly the three methods TldDiscovery uses: findEntries, getEntry, getHeaders. The webapp
     * dir is presented under the "/webapp" prefix, matching Dysoweb's real bundle layout.
     */
    static Bundle fakeBundle(File webroot, File embeddedJar, String jarToken) {
        Hashtable<String, String> headers = new Hashtable<>();
        headers.put("Bundle-ClassPath", ".," + jarToken);
        return proxy(Bundle.class, new H("bundle")
                .on("getSymbolicName", a -> "spike-bundle")
                .on("getHeaders", a -> (Dictionary<String, String>) headers)
                .on("getEntry", a -> {
                    String path = (String) a[0];
                    if (path.equals(jarToken) || path.equals("/" + jarToken)) {
                        return embeddedJar.toURI().toURL();
                    }
                    String rel = path.startsWith(TldDiscovery.WEBAPP_PREFIX)
                            ? path.substring(TldDiscovery.WEBAPP_PREFIX.length()) : path;
                    File f = new File(webroot, rel.startsWith("/") ? rel.substring(1) : rel);
                    return f.exists() ? f.toURI().toURL() : null;
                })
                .on("findEntries", a -> {
                    String path = (String) a[0];       // e.g. /webapp/WEB-INF
                    String pattern = (String) a[1];    // e.g. *.tld
                    String rel = path.startsWith(TldDiscovery.WEBAPP_PREFIX)
                            ? path.substring(TldDiscovery.WEBAPP_PREFIX.length()) : path;
                    File dir = new File(webroot, rel.startsWith("/") ? rel.substring(1) : rel);
                    List<URL> hits = new ArrayList<>();
                    collectEntries(dir, TldDiscovery.WEBAPP_PREFIX + rel, pattern, hits);
                    return Collections.enumeration(hits);
                }));
    }

    /** Recursively collect files matching a "*.ext" glob, as URLs whose path is the /webapp-rooted entry path. */
    static void collectEntries(File dir, String logicalDir, String pattern, List<URL> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        String suffix = pattern.startsWith("*") ? pattern.substring(1) : pattern;
        for (File k : kids) {
            String logical = logicalDir + "/" + k.getName();
            if (k.isDirectory()) {
                collectEntries(k, logical, pattern, out);
            } else if (k.getName().endsWith(suffix)) {
                try {
                    // Present a bundle-style URL whose path is the entry path (matches real bundle:// URLs).
                    out.add(new URL(null, "bundleentry://spike" + logical, new BackingFileHandler(k)));
                } catch (Exception ignore) { }
            }
        }
    }

    /** URL handler that makes a bundleentry:// URL stream from a backing file (mimics an OSGi entry URL). */
    static final class BackingFileHandler extends java.net.URLStreamHandler {
        private final File file;
        BackingFileHandler(File file) { this.file = file; }
        protected java.net.URLConnection openConnection(URL u) {
            return new java.net.URLConnection(u) {
                public void connect() { }
                public InputStream getInputStream() throws IOException { return new FileInputStream(file); }
            };
        }
    }

    static void listTree(File dir, String indent) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        java.util.Arrays.sort(kids);
        for (File k : kids) {
            System.out.println(indent + k.getName() + (k.isDirectory() ? "/" : " (" + k.length() + "b)"));
            if (k.isDirectory()) listTree(k, indent + "  ");
        }
    }
}
