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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.servlet.ServletContext;

import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.TldCache;
import org.apache.tomcat.util.descriptor.tld.TaglibXml;
import org.apache.tomcat.util.descriptor.tld.TldParser;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;
import org.xml.sax.SAXException;

/**
 * Modern-Jasper TldCache backed by the processor's platform-wide taglib registry.
 *
 * In OSGi a JSP routinely uses tags whose TLD ships in another bundle (e.g. the panel uses
 * the dw: tags from utils.tags), and bundles come and go at runtime. So instead of a
 * snapshot built at init, taglib uris are resolved live through {@code locator}, which maps
 * a uri to the TLD copy the processor keeps in its scratch dir (null if unknown). Parsed
 * TLDs are cached and re-parsed when that file changes (bundle update).
 */
public class DysowebTldCache extends TldCache {

    private final Function<String, File> fLocator;
    private final ClassLoader fLoader;
    private final Map<TldResourcePath, Entry> fParsed = new ConcurrentHashMap<TldResourcePath, Entry>();

    public DysowebTldCache(ServletContext ctx, ClassLoader loader, Function<String, File> locator) {
        super(ctx, Collections.<String, TldResourcePath>emptyMap(),
                Collections.<TldResourcePath, TaglibXml>emptyMap());
        fLocator = locator;
        fLoader = loader;
    }

    @Override
    public TldResourcePath getTldResourcePath(String uri) {
        File f = fLocator.apply(uri);
        if (f == null || !f.isFile()) {
            return null;
        }
        try {
            // no webappPath: the TLD is not a resource of this bundle's context
            return new TldResourcePath(f.toURI().toURL(), null);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public TaglibXml getTaglibXml(TldResourcePath path) throws JasperException {
        long lastModified = lastModified(path);
        Entry entry = fParsed.get(path);
        if (entry == null || entry.lastModified != lastModified) {
            // nsAware=true, validate=false (no schema fetch), blockExternal=true (no external entities)
            TldParser parser = new TldParser(true, false, true);
            parser.setClassLoader(fLoader);
            try {
                entry = new Entry(parser.parse(path), lastModified);
            } catch (IOException | SAXException e) {
                throw new JasperException(e);
            }
            fParsed.put(path, entry);
        }
        return entry.taglib;
    }

    private static long lastModified(TldResourcePath path) {
        try {
            return new File(path.getUrl().toURI()).lastModified();
        } catch (Exception e) {
            return -1;
        }
    }

    private static final class Entry {
        final TaglibXml taglib;
        final long lastModified;

        Entry(TaglibXml taglib, long lastModified) {
            this.taglib = taglib;
            this.lastModified = lastModified;
        }
    }
}
