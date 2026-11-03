/*
 * Track-B Phase-1 adapter (proven in the spike; drop into
 * com.requea.dysoweb.processor.jasper when the engine swap lands).
 *
 * Builds the org.apache.jasper.compiler.TldCache that modern Jasper reads from a
 * ServletContext attribute. In OSGi there is NO Catalina TldScanner run, so the
 * caller discovers the bundle's TLDs (WEB-INF/**.tld, embedded-jar META-INF/**.tld,
 * and web.xml <jsp-config><taglib> mappings) and passes them in as TldSource records.
 * This class does the parse + map assembly -- the mechanical part proven by the spike.
 */
package com.requea.dysoweb.spike.adapter;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.apache.jasper.compiler.TldCache;
import org.apache.tomcat.util.descriptor.tld.TaglibXml;
import org.apache.tomcat.util.descriptor.tld.TldParser;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;
import org.xml.sax.SAXException;

public class TldCacheBuilder {

    /**
     * One discovered TLD. {@code url} is a resolvable location; {@code webappPath} is
     * the webapp-relative path (e.g. {@code /WEB-INF/spike.tld}) or null; {@code entryName}
     * is set only when the TLD lives INSIDE an embedded jar (the OSGi-specific case),
     * in which case {@code url} points at the jar and {@code entryName} at the TLD entry.
     */
    public static final class TldSource {
        public final URL url;
        public final String webappPath;
        public final String entryName;

        public TldSource(URL url, String webappPath, String entryName) {
            this.url = url;
            this.webappPath = webappPath;
            this.entryName = entryName;
        }

        /** TLD sitting directly under the bundle's WEB-INF. */
        public static TldSource webInf(URL url, String webappPath) {
            return new TldSource(url, webappPath, null);
        }

        /** TLD packaged inside an embedded jar: {@code jarUrl!/entryName}. */
        public static TldSource inJar(URL jarUrl, String webappPath, String entryName) {
            return new TldSource(jarUrl, webappPath, entryName);
        }
    }

    private TldCacheBuilder() { }

    public static TldCache build(ServletContext ctx, ClassLoader bundleLoader,
                                 Collection<TldSource> sources)
            throws IOException, SAXException {
        // nsAware=true, validate=false (no schema fetch), blockExternal=true (no external entities)
        TldParser parser = new TldParser(true, false, true);
        parser.setClassLoader(bundleLoader);

        Map<String, TldResourcePath> uriMap = new HashMap<>();
        Map<TldResourcePath, TaglibXml> tldMap = new HashMap<>();

        for (TldSource s : sources) {
            TldResourcePath trp = (s.entryName == null)
                    ? new TldResourcePath(s.url, s.webappPath)
                    : new TldResourcePath(s.url, s.webappPath, s.entryName);
            TaglibXml taglib = parser.parse(trp);
            tldMap.put(trp, taglib);
            // Explicit-<uri> taglibs are resolved by uri; path-based references
            // (<%@ taglib uri="/WEB-INF/x.tld" %>) resolve via the webappPath instead.
            if (taglib.getUri() != null) {
                uriMap.put(taglib.getUri(), trp);
            }
            if (s.webappPath != null) {
                uriMap.putIfAbsent(s.webappPath, trp);
            }
        }
        return new TldCache(ctx, uriMap, tldMap);
    }
}
