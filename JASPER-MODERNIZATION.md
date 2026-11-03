# Plan: Replace the vendored Jasper with modern Tomcat 10.1 Jasper (Track B)

This is the deferred "Track B" from `JAKARTA-MIGRATION.md`. The jakarta migration flipped the
~103k-line vendored Tomcat-7 Jasper fork to `jakarta.*` **in place** (Track A) and it is
functionally verified on Tomcat 10.1 / JDK 21. Track B is the real modernization: **delete the
fork and depend on the maintained `org.apache.tomcat:tomcat-jasper:10.1.x`** so we get current
JSP 3.1 / EL 5.0 semantics, security fixes, and no 103k lines of frozen code to carry forward.

This is a weeks-to-months project, not a mechanical pass. This document scopes it into
independently-shippable tracks so it can be done incrementally without a big-bang rewrite.

## The reframing that makes this tractable

The jakarta plan filed this as "blocked on rewriting the `DirContext` resource layer
(`BundleDirContext`/`DefaultServlet`) + re-integrating the OSGi classloader." Reading the code,
that framing is **wrong in a useful way**:

- **Jasper never touches `DirContext`.** `JspCompilationContext` reads JSP source purely through
  `ServletContext.getResource` / `getResourceAsStream` / `getResourcePaths` / `getRealPath`
  (`jasper/JspCompilationContext.java:332-374`). Those are already backed per-bundle by
  `ServletContextWrapper.getResource` → `RequestProcessor.getEntryInfo(path)` (bundle entries)
  with a container fallback (`processor/ServletContextWrapper.java:139-190`).
- **`DirContext` is `DefaultServlet`'s private concern.** Only `DefaultServlet` + `BundleDirContext`
  (822 lines) use `org.apache.naming.resources.ProxyDirContext/Resource/…` — static file serving
  (GET/PUT/DELETE, ranges, welcome files, directory listing). `DefaultServlet` is constructed
  directly by `RequestProcessor` (`RequestProcessor.java:134`) and has nothing to do with JSP
  compilation.

**Consequence:** swapping in modern Jasper does **not** require the `DirContext` rewrite. The two
are separable. That rewrite becomes an optional, independent **Track C** (retire `naming-resources`
5.5.20), sequenced *after* — or never, if we keep the fork's `DefaultServlet`.

## What "modern Jasper" actually needs from us

Modern Jasper is more coupled to Catalina's runtime *context state* than the standalone fork was.
The fork was hacked to be self-contained; `org.apache.jasper` in 10.1 assumes the container
populated a set of `ServletContext` attributes and provides a few services. The real work of
Track B is **supplying those in an OSGi world where there is no Catalina context per bundle.**

The integration surface (each item = a concrete deliverable below):

1. **A `ServletContext` per web bundle** that serves bundle entries via `getResource*` (we already
   have this — `ServletContextWrapper`) **and** carries the attributes modern Jasper reads:
   - `org.apache.tomcat.InstanceManager` — Jasper instantiates servlets/tags/EL functions through
     it. We already ship `org.apache.tomcat.DefaultInstanceManager` + `InstanceManagerFactory` in
     the fork; provide one bound to the **bundle classloader**.
   - `org.apache.catalina.jsp_classpath` (`Constants.SERVLET_CLASSPATH`) — the classpath string
     Jasper hands the Java compiler. Today the fork builds a per-bundle `URLClassLoader` chain
     (`RequestProcessor.java:1399-1404`, `getLoaderURL`); we must render that chain as the
     classpath string / provide it via the loader.
   - **TLD cache** (`org.apache.tomcat.util.descriptor.tld.TldCache`, stored as a context
     attribute) — see #3, the big one.
2. **The per-bundle classloader.** Modern Jasper reads the loader from the `ServletContext` /
   `JspRuntimeContext` and compiles + loads generated classes through it. We already assemble the
   OSGi bundle loader (`JasperLoader(bundle)` wrapped in a `URLClassLoader`,
   `RequestProcessor.java:1402`). This maps cleanly — but modern Jasper's own `JasperLoader`
   (child-first, per-JSP) must sit **on top of** the bundle loader as parent so JSP classes see the
   bundle's exports. Getting this parent chain right is the classloader-integration risk.
3. **TLD discovery — the largest gap.** The fork scans `.tld` itself (`jasper/compiler/
   TldLocationsCache.java`, `TagLibraryInfoImpl.java`). Modern Jasper does **not** scan; it expects
   a pre-built `TldCache` that Catalina's `TldScanner` populated from `WEB-INF/**.tld`, `web.xml`
   `<taglib>` mappings, and jar `META-INF/**.tld`. In OSGi there is no `TldScanner` run — **we must
   build the `TldCache` ourselves** from the bundle's own entries + its embedded jars + the web.xml
   parsed by the processor. This is the single most involved piece.
4. **The compiler — DECIDED: `ecj`.** The fork compiles with **Janino** via
   `jasper/compiler/AntCompiler.java`. We adopt modern Jasper's default, the **Eclipse compiler
   (`org.eclipse.jdt:ecj`)** — the maintained path, no `Compiler`-SPI porting to own. This drops
   Janino (`org.codehaus.janino:janino` + `commons-compiler`) and adds `ecj` as one embedded/
   exported dependency of the processor bundle. `ecj` is self-contained (no `javac`/JDK-tools
   dependency), which suits the OSGi bundle. Pin it to a version whose bytecode target covers
   JDK 21 (any recent 3.x, e.g. the `ecj` shipped alongside Tomcat 10.1.x).
5. **The Jasper runtime classes that generated JSPs extend.** Generated servlets `extend
   org.apache.jasper.runtime.HttpJspBase` and call `PageContextImpl`, `JspWriterImpl`,
   `JspRuntimeLibrary`, `TagHandlerPool`, etc. The fork relocated these to
   `com.requea.dysoweb.org.apache.jasper.runtime.*` (19 files under `com/requea/dysoweb/org/apache/
   jasper/runtime/`). With modern Jasper, generated code references the **real**
   `org.apache.jasper.runtime.*`, so that package must be **exported by the system bundle / an API
   bundle and importable by every web bundle.** This is an OSGi wiring change, not just a code swap.

## Dependency / artifact plan

Add (all `org.apache.tomcat`, version-aligned to the same 10.1.x already used for
`tomcat-jasper-el`, currently **10.1.56**):

- `tomcat-jasper` — the compiler + `JspServlet` + runtime.
- `tomcat-jasper-el` — already present (10.1.56); keep, version-aligned.
- `tomcat-el-api` — already present (10.1.56); keep.
- `tomcat-util` + `tomcat-util-scan` — buf/codec/digester/scan utilities modern Jasper needs
  (TLD/jar scanning, XML digester). Today these are **vendored** under `org/apache/tomcat/util/**`
  (242 files); adopting the real jars lets us delete large swaths of the fork (see "What gets
  deleted").
- `tomcat-api` — `InstanceManager` interface etc.
- `org.eclipse.jdt:ecj` — the JSP compiler (**decided**). Embedded in / exported from the
  processor bundle.

Drop (as the fork is deleted):

- Vendored `com/requea/dysoweb/jasper/**` (105 files) — replaced by `tomcat-jasper`.
- Vendored `org/apache/tomcat/util/**` (242) — replaced by `tomcat-util`/`tomcat-util-scan`
  **to the extent the processor glue doesn't use them directly** (mapper + fileupload are used by
  our own request handling — audit before deleting; those may need to stay or move).
- `com/requea/dysoweb/org/apache/jasper/runtime/**` (19) — replaced by `tomcat-jasper`'s runtime.
- Catalina/Jetty remnants `org/apache/catalina/util/*`, `org/mortbay/**` (in `com/requea/dysoweb/
  org/`) — audit; `Mapper` still uses `org.mortbay...PathMap`, so tied to the mapper decision.
- `org.codehaus.janino:janino` + `commons-compiler` — dropped (superseded by `ecj`).
- `requeabuild:naming-resources:5.5.20` — **only** when Track C retires `DefaultServlet`'s
  `DirContext`. Not part of Track B proper.

**OSGi wiring:** modern Jasper's packages (`org.apache.jasper.*`, `org.apache.jasper.runtime.*`,
`org.apache.tomcat.*`) must be exported so web bundles can import the runtime classes their
generated JSPs reference. Two options:
- Embed `tomcat-jasper` (+ runtime) **inside the processor bundle** (as today) and **export**
  `org.apache.jasper.runtime` from it; web bundles `Import-Package` it. Simplest.
- Or add the servlet/jsp system-bundle exports in `dysoweb.properties` `system.packages`.
Recommend the processor-bundle export — keeps it versioned with the engine.

## What gets deleted (the payoff)

Rough counts from the current tree (`com.requea.dysoweb.processor/src/main/java`):

| Area | Files | Fate |
|---|---|---|
| `jasper/**` (compiler+servlet+el+xmlparser+tagplugins) | 105 | **delete** → `tomcat-jasper` |
| `org/apache/tomcat/util/**` | 242 | **mostly delete** → `tomcat-util(-scan)`; keep what glue uses |
| `com/requea/dysoweb/org/apache/jasper/runtime/**` | 19 | **delete** → jasper runtime |
| `com/requea/dysoweb/org/{catalina,mortbay}/**` | 4 | audit (mapper-coupled) |
| `defaultservlet/{DefaultServlet,BundleDirContext,Globals}` | 3 | **keep** (Track C, not B) |

Net: on the order of **~350 of ~370 vendored files retired.** That is the whole point.

---

## Phased execution

### Phase 0 — Recon & spike (no production edits)
1. **Prove the resource path. ✅ DONE (2026-07-17) — PASS.** Built `sandbox/jasper-spike/`
   (standalone, not in the reactor): drove the real `org.apache.jasper.servlet.JspServlet`
   (`tomcat-jasper:10.1.56`) against a proxy `ServletContext` whose ONLY resource access is
   `getResource`/`getResourceAsStream` (no `DirContext`, no `WebResourceRoot`), a supplied
   classloader + classpath attribute, and a minimal `InstanceManager`. A taglib-free JSP
   (scriptlet + expression + loop) **compiled via `ecj` and executed** →
   `SPIKE_OK who=dysoweb result=42 loop=10`, generating `org/apache/jsp/hello_jsp.{java,class}`
   in the scratch dir. **Thesis confirmed:** modern Jasper needs only a `ServletContext` +
   classloader + `InstanceManager`; the `DirContext` layer is `DefaultServlet`'s concern alone, so
   Track B and Track C are separable. **Also confirmed the Phase-2 export requirement concretely:**
   generated code `extends org.apache.jasper.runtime.HttpJspBase`, implements
   `org.apache.jasper.runtime.{JspSourceDependent,JspSourceImports,JspSourceDirectives}`, and calls
   `org.apache.jasper.runtime.InstanceManagerFactory` — so `org.apache.jasper.runtime.*` must be
   OSGi-exported to every web bundle. Deps all resolved from Maven Central at 10.1.56
   (`tomcat-jasper`, `tomcat-util`, `tomcat-util-scan`, `tomcat-api`) + `ecj:3.33.0` (already cached).
   Spike kept under `sandbox/jasper-spike/` as a reference harness; delete when Track B lands.
1a. **De-risk the TLD path. ✅ DONE (2026-07-17) — PASS.** Extended the same spike with a custom
   tag (`HelloTag extends SimpleTagSupport`), a `WEB-INF/spike.tld` (uri `http://requea.com/spike`),
   and a JSP using `<%@ taglib %>` + `<s:hello name="dysoweb"/>`. Crucially, **no scanner runs** —
   we hand-build the cache modern Jasper reads: parse the TLD with
   `org.apache.tomcat.util.descriptor.tld.TldParser(nsAware=true, validate=false, blockExternal=true)`
   → `TaglibXml`; wrap the TLD URL in a `TldResourcePath(url, "/WEB-INF/spike.tld")`; construct
   `org.apache.jasper.compiler.TldCache(ctx, {uri→TldResourcePath}, {TldResourcePath→TaglibXml})`;
   store it under `TldCache.SERVLET_CONTEXT_ATTRIBUTE_NAME`. The tag JSP compiled + executed →
   `SPIKE_TAG_OK TAG[name=dysoweb]`. **This is the exact shape of the Phase-1.7 `TldCache` builder,
   proven end-to-end.** Generated tag code does `new com.requea.dysoweb.spike.tags.HelloTag()`
   (a *compiled* reference → the tag class must be visible on the JSP's/bundle's classloader) and
   `org.apache.jasper.runtime.InstanceManagerFactory.getInstanceManager(getServletConfig())` for
   `newInstance(tag)`/`destroyInstance(tag)` injection hooks. **Note the class locations:** the
   `TldCache` we populate is `org.apache.jasper.compiler.TldCache` (in `tomcat-jasper`), while
   `TldParser`/`TldResourcePath`/`TaglibXml` are in `org.apache.tomcat.util.descriptor.tld`
   (in `tomcat-util-scan`).
2. **Map the util dependencies of our own glue.** `grep` the processor glue (RequestProcessor,
   Mapper, RequestWrapper, fileupload users) for `org.apache.tomcat.util.*` to learn which vendored
   util packages are used by *us* vs *only by the fork*. Determines how much of the 242 can go.
3. **Inventory generated-JSP runtime references.** Confirm the set of `org.apache.jasper.runtime.*`
   symbols generated code depends on, so the OSGi export list is complete.
4. **Pin versions.** `tomcat-jasper`, `tomcat-util`, `tomcat-util-scan`, `tomcat-api`, `ecj` — all
   resolvable on Maven Central, aligned to 10.1.56 (matching the EL jars already in use).

### Phase 1 — Container services (the integration layer)

> **Adapters written & proven (2026-07-17).** All FOUR Phase-1 adapter classes now exist as real,
> compiling, spike-verified code under `sandbox/jasper-spike/src/main/java/com/requea/dysoweb/spike/adapter/`:
> `DysowebInstanceManager` (impl of `org.apache.tomcat.InstanceManager`, instantiates via the bundle
> loader), `TldCacheBuilder` (parses `TldSource`s → `org.apache.jasper.compiler.TldCache`),
> `TldDiscovery` (enumerates a bundle's TLDs — loose `/webapp/WEB-INF/**.tld` **and** embedded-jar
> `META-INF/**.tld` — into `TldSource`s, written against the real `org.osgi.framework.Bundle`), and
> `DysowebJasperSetup.configure(ctx, bundleLoader, loaderURLs, scratchDir, tlds)` (installs tempdir +
> `jsp_classpath` + InstanceManager + TldCache). The spike drives the real `JspServlet` through these
> and renders **three** JSPs: a plain JSP, a WEB-INF-tag JSP, and a JSP whose tag's TLD lives inside an
> **embedded jar** (`SPIKE_EMBED_OK EMBED[x=jar]`) — the OSGi-specific case, now proven end-to-end
> (discover → cache → compile → execute). They lift into `com.requea.dysoweb.processor.jasper` verbatim
> (just change the package). Insertion point in the real code is `RequestProcessor.java:1399-1404` (the
> per-bundle Jasper init loop): call `TldDiscovery.discover(bundle, getScratchDir())` then
> `DysowebJasperSetup.configure(...)` on the bundle's `ServletContextWrapper` right before
> `jasperServlet.init(...)`, passing the existing `getLoaderURL(...)` / `getScratchDir()` values and the
> bundle's `new JasperLoader(bundle)`. **`TldDiscovery` modernizes the fork's existing
> `RequestProcessor.processTLDinBundleClassPath` (lines ~878-930)** — same conventions (`/webapp` prefix,
> Bundle-ClassPath tokenized by `,`), emitting `TldSource`s instead of populating `TldLocationsCache`.
>
> **Two non-obvious gotchas the discovery work surfaced (baked into `TldDiscovery`):**
> - **Pass embedded jars as a plain `file:` URL, not `jar:...!/`.** Tomcat's `JarFactory` rewrites a
>   pre-wrapped `jar:` base into a `war:` URL whose handler isn't registered outside a running Tomcat
>   (`unknown protocol: war`). Give it the bare jar URL; it adds the `jar:...!/<entry>` wrapper itself.
>   `TldDiscovery` also **materializes** each embedded jar to the work dir first, so a `bundle://` URL
>   never has to be opened as a nested jar (not reliably supported across frameworks).
> - **A jar TLD's `webappPath` must be `null`.** `TldCache.getLastModified` calls
>   `ServletContext.getResource(webappPath)` for any non-null path and NPEs if it resolves to null. A
>   jar TLD is not a webapp resource — Jasper resolves it by its `<uri>` — so pass `null` (the fork's
>   old `"<jar>$<tld>"` synthetic key must NOT be reused here).

> **Two refinements this step surfaced (both verified against the real code):**
> 1. **The engine swap is ATOMIC, not "add alongside."** `org.apache.tomcat.InstanceManager` (and the
>    whole `org.apache.tomcat.util.*` tree) exist BOTH vendored in the processor module AND in
>    `tomcat-api`/`tomcat-util`. You cannot add the modern deps while the vendored copies remain —
>    split-package/duplicate-class collision. So deleting the fork's `org/apache/tomcat/**` +
>    `jasper/**` and adding `tomcat-jasper`/`tomcat-util(-scan)`/`tomcat-api` must happen in ONE
>    commit. Phases 1 and 2 below are therefore a single landing, not sequential "alongside" steps.
> 2. **Jasper's context attributes must be BUNDLE-LOCAL.** `ServletContextWrapper` is already per-bundle
>    (`RequestProcessor.fServletContextWrappers`, keyed by bundle id), but its `getAttribute`/
>    `setAttribute` (lines 228-240) delegate to the SHARED container context `fContext`. Two web bundles
>    would clobber each other's `TldCache`/`InstanceManager`. Fix: give `ServletContextWrapper` a
>    bundle-local attribute map for (at least) the Jasper keys instead of delegating to `fContext`.

Build the OSGi-side adapters modern Jasper reads (the three above are done; the remaining item is the
bundle-side TLD *discovery* that feeds `TldCacheBuilder`):
5. **`InstanceManager`** bound to the bundle classloader; publish as the
   `org.apache.tomcat.InstanceManager` context attribute on each bundle's `ServletContextWrapper`.
   **✅ done — `DysowebInstanceManager` + `DysowebJasperSetup.configure`.**
6. **Classpath provider** — render the per-bundle `URLClassLoader` chain (`getLoaderURL`) as the
   `org.apache.catalina.jsp_classpath` string + confirm the loader parent chain
   (JSP loader → bundle loader → API/system) is correct.
   **✅ done — `DysowebJasperSetup.classpathString(loaderURLs)`.** (Loader parent chain already
   established by the existing `RequestProcessor` wiring; reused unchanged.)
7. **`TldCache` builder + discovery** — **DONE & proven (`TldCacheBuilder` + `TldDiscovery`).**
   `TldCacheBuilder` turns `TldSource(url, webappPath, entryName)` records into the
   `org.apache.jasper.compiler.TldCache`; `TldDiscovery.discover(bundle, workDir)` produces those
   records from the real `org.osgi.framework.Bundle` — loose `/webapp/WEB-INF/**.tld` via
   `findEntries` **and** embedded-jar `META-INF/**.tld` via the `Bundle-ClassPath` scan (no
   `JarScanner`). The spike proves both paths compile+execute. **Remaining polish only:** fold in
   web.xml `<jsp-config><taglib>` explicit mappings (the processor already parses web.xml), and
   handle implicit taglibs — reuse the fork's `TagLibraryInfoImpl`/`TldLocationsCache` as a reference.

### Phase 2 — Swap the engine
8. Replace `new JspServlet()` (fork) with `org.apache.jasper.servlet.JspServlet`
   (`RequestProcessor.java:1399`), initialized with the Phase-1 context + attributes. Compiler is
   `ecj` (Jasper's default — set/confirm `compilerClassName` unset so Jasper uses `ecj`; drop the
   fork's Janino `AntCompiler` wiring).
9. Wire OSGi exports so web bundles import `org.apache.jasper.runtime.*` (+ needed `org.apache.
   tomcat.*`) — from the processor bundle. Update the panel/utils.tags/test/demo bundle
   `Import-Package` clauses accordingly.
10. Delete the vendored `jasper/**`, the jasper `runtime/**`, and the util packages proven
    unused-by-glue in Phase 0. Keep `DefaultServlet`/`BundleDirContext` (Track C).

### Phase 3 — Deploy & verify (no test suite — same protocol as the jakarta migration)
Deploy on the same Tomcat 10.1.56 / JDK 21 rig. Exercise, in order of risk:
11. Panel JSPs (custom `dw:`/`panel:` tags + EL + includes) compile & render — the exact path the
    jakarta migration proved on the fork; it's the regression oracle.
12. OBR remote install still works.
13. A freshly-installed demo bundle with its own servlet + JSP + embedded lib (demo4 / JFreeChart)
    renders — proves per-bundle classloader + runtime exports end to end.
14. TLD edge cases: a bundle providing a `.tld` inside an embedded jar; an implicit taglib.

### Phase 4 (optional, independent) — Track C: retire the `DirContext` static layer
Only if we want to drop `naming-resources:5.5.20`. Rewrite `DefaultServlet` against
`ServletContext.getResource*` (it already flows through `ServletContextWrapper`) or adopt Tomcat
10.1's `WebResourceRoot`/`DefaultServlet`. Fully decoupled from Jasper; can ship separately or be
skipped indefinitely — the fork's `DefaultServlet` works.

## Risks / watch-items (ranked)

1. **TLD discovery in OSGi (Phase 1.7).** No `TldScanner` runs; if the `TldCache` is incomplete,
   custom tags fail to resolve at compile time. Was the highest-risk item — **now de-risked**: the
   Phase-0 spike (item 1a) drove a custom tag through a hand-built `TldCache` end-to-end. Residual
   effort is enumeration breadth (embedded-jar TLDs via bundle APIs, implicit taglibs), not
   feasibility.
2. **Classloader parent chain (Phase 1.6 / 2).** Modern Jasper's child-first `JasperLoader` must
   have the OSGi bundle loader as parent, or generated JSPs won't see bundle exports (and could
   double-load API classes → `LinkageError`). The fork already solved a version of this
   (`RequestProcessor.java:1402`); replicate the intent, not the code.
3. **Runtime package exports (Phase 2.9).** If `org.apache.jasper.runtime` isn't exported/imported
   correctly, every generated JSP fails to link. OSGi-wiring bug, not a code bug — catch it on the
   first panel JSP.
4. **Glue's use of vendored `org.apache.tomcat.util` (mapper/fileupload).** If our own request
   handling depends on vendored util that we delete, the processor breaks outside JSP. Phase 0.2
   scopes this before any deletion.
5. **Compiler (`ecj`) as an OSGi dependency.** Decided; the residual risk is only wiring — `ecj`
   must be embedded/exported by the processor bundle and visible to Jasper's classloader. No
   behavioral risk (it's Jasper's own default compiler).
6. **EL behavior delta** already absorbed by the jakarta migration (fork runs on EL 5.0 today), so
   Track B carries *less* EL risk than Track A did.

## Recommendation

Do **Track B without Track C**, with **`ecj` as the JSP compiler (decided)**. Keep the fork's
`DirContext`-based `DefaultServlet` (it works and is isolated), and spend the effort on the JSP
engine where the maintenance payoff is — deleting ~350 files of frozen Tomcat-7 code. Gate the whole
thing behind the **Phase-0 spike**: if modern Jasper compiles one JSP through our
`ServletContextWrapper` + a supplied loader (with `ecj`), the thesis holds and the rest is
integration work with a known shape. If the spike fights us on the resource/classloader model, stop
and reassess before committing weeks.

## Locked decisions

- **JSP compiler: `ecj`** (`org.eclipse.jdt:ecj`) — Jasper's default; drop Janino. (2026-07-17)

## Landing status (branch `track-b-jasper-modernization`)

**The atomic swap is LANDED and the full reactor builds green (24 modules); a deployable 14 MB
`dysoweb-2.0.1-SNAPSHOT.war` is produced.** Done: modern deps in the processor pom (tomcat-jasper/
util/util-scan/api/juli 10.1.56 + ecj 3.33.0; dropped janino/ant/old-juli); bundle instructions
(export `org.apache.jasper.runtime`, `Private-Package` trimmed to `com.requea.dysoweb.*`, explicit
`Embed-Dependency`, unused Ant/JSR-199/Graal imports made optional); 461 fork files deleted; the 4
adapters added under `com.requea.dysoweb.processor.jasper`; `RequestProcessor` repointed to
`org.apache.jasper.servlet.JspServlet` + `TldDiscovery`/`DysowebJasperSetup`; `ServletContextWrapper`
Jasper attributes made bundle-local. Also fixed a **pre-existing** war-packaging bug (the "war
versionning" commit versioned `finalName` but left the bundle-copy paths hardcoded at `target/dysoweb/`,
so the war shipped with no `WEB-INF/bundle`).

Verified in the artifact: no vendored fork classes, modern `tomcat-jasper`/`ecj` embedded,
`org.apache.jasper.runtime` exported, no double-inclusion. Classloader check: web bundles need **no**
`org.apache.jasper.runtime` import — `JasperLoader` falls back to the processor bundle's own loader.

**Phase 3 deploy verification: DONE (2026-11-03) on Tomcat 10.1.56 / JDK 21.0.4.** Clean boot, all
core bundles ACTIVE; every panel JSP compiles via modern Jasper + ecj and renders (custom `dw:`/`panel:`
tags + EL); authenticated pages OK; OBR remote install of `com.requea.dynalib` succeeds (6 bundles
downloaded + ACTIVE); runtime-installed demo4 serves its servlet, JSP and embedded-JFreeChart PNG; the
`test` bundle renders a JSP using its own TLD **and** a cross-bundle TLD; a TLD inside an embedded jar
(`Bundle-ClassPath`) resolves; bundle `update` recompiles cleanly. `apps.jsp` still 500s — the
pre-existing missing `<panel:installedfeatures>` tag, identical on the fork.

Fixes the deploy surfaced (all in the working tree):
- **Taglibs are platform-wide, not per-bundle.** The panel uses `dw:` tags whose TLD ships in
  `utils.tags`; the per-bundle `TldDiscovery` snapshot couldn't see it. Replaced `TldDiscovery` +
  `TldCacheBuilder` with **`DysowebTldCache extends TldCache`**, which resolves each uri *live* through
  `RequestProcessor.getTaglibFile(uri)` — the fork's existing global registry (`fTagLocations`, fed by
  every bundle's `WEB-INF/**.tld`, embedded-jar `META-INF/*.tld` and web.xml `<taglib>`, cleaned up on
  undeploy), i.e. the direct port of the deleted `DysowebTldLocationCache`. Parsed TLDs are cached and
  re-parsed when the scratch copy changes.
- **`JasperLoader.getResource` falls back to the processor bundle** (as `loadClass` already did). ecj
  reads `org/apache/jasper/runtime/*.class` as *resources*; web bundles don't import that package.
- **Processor `Import-Package`: `!org.apache.felix.*`.** `Private-Package com.requea.dysoweb.*` inlines
  dysoweb-core's bootstrap classes (pre-existing, also in 2.0.0); the new trailing `*` made bnd import
  their unused Felix launcher refs → processor unresolvable.
- **Shell bundle `_contract=off`** — jakarta-migration leftover (`osgi.contract=JakartaServlet`
  requirement Felix can't satisfy); same fix the processor/panel already had.
- **`test` sample bundle imports `com.requea.dysoweb.utils.tags`** — its JSP uses `dw:` tags but bnd
  can't see JSP-only references (pre-existing; the panel pom already does this).
