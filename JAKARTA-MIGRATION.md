# Plan: Migrating Dysoweb to Jakarta EE (`javax.*` → `jakarta.*`)

This is "Path B" from `JAVA21-MIGRATION.md`. The Java 21 work (Path A) kept `javax.*` and
runs on Tomcat 9. This plan takes the platform to the `jakarta.*` namespace so it can run
on **Tomcat 10.1+ / Jakarta EE 10**. It builds on the completed Java 21 toolchain work
(compiler `release=21`, bundle-plugin 5.1.9/bnd 6.3.1, Felix 7.0.5).

## Scope decisions (locked)

- **Vendored Jasper engine → rewrite in place** (Track A). Mechanically flip the ~103k-line
  Tomcat 7 fork from `javax.*` to `jakarta.*`, preserving its behavior and OSGi/classloader
  integration. We do **not** replace it with a modern Jasper in this pass (that's a separate,
  weeks-to-months project because of the `DirContext`/classloader coupling — see "Deferred").
- **`portlet/` excluded.** The Portlet spec did not rename to `jakarta.portlet` in Jakarta EE 9
  (it stayed `javax.portlet` through Portlet 3.0), and the portlet WAR is already deferred as
  not-deploy-ready. Leave it on `javax`. Migrate `dysoweb/` and the `dysoweb.demo/` samples only.

## Target coordinates

| Concern | From (today) | To |
|---|---|---|
| Runtime container | Tomcat 9.0 (javax) | **Tomcat 10.1.x** (jakarta, Servlet 6.0 / JSP 3.1 / EL 5.0) |
| Servlet API | `javax.servlet:javax.servlet-api:3.1.0` | `jakarta.servlet:jakarta.servlet-api:6.0.0` |
| JSP API | `javax.servlet.jsp:javax.servlet.jsp-api:2.3.1` | `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.1` |
| EL | `org.apache.tomcat:tomcat-el-api:7.0.109` (`javax.el`) | `jakarta.el:jakarta.el-api:5.0.1` |
| Jasper-EL runtime | `tomcat-jasper-el:7.0.109` | `org.apache.tomcat:tomcat-jasper-el:10.1.x` |
| Package namespace | `javax.servlet*`, `javax.el`, `javax.servlet.jsp*` | `jakarta.*` (same subpackage names) |

**Version-number note:** the OSGi package-export/import qualifiers change too, and they
currently do *not* match the API spec numbers (JSP is stamped `3.2.3`, EL `3.3.0`). Under
Jakarta EE 10 the correct spec versions are: `jakarta.servlet` **6.0.0**, `jakarta.servlet.jsp`
**3.1.0**, `jakarta.el` **5.0.0**. Reconcile every hand-written qualifier to these.

**Namespace-flip vs API-removal — read this before picking the servlet version.** The pure
namespace rename (no API changes) corresponds to **Jakarta EE 9 / Servlet 5.0**. Jakarta EE 10 /
**Servlet 6.0 removed** long-deprecated APIs (`SingleThreadModel`, `HttpSessionContext`,
`ServletRequest.getRealPath`, `ServletResponse.encodeUrl/encodeRedirectUrl`,
`HttpServletResponse.setStatus(int,String)`, `getServlets()`/`getServletNames()`, log(Exception,String)).
The vendored Tomcat 7 Jasper (Servlet 3.0 era) **may call some of these**. Two options:
- **Recommended:** target Servlet 6.0 (Tomcat 10.1, current & supported) and fix the handful of
  removed-API call sites in the vendored fork as a sub-step of Phase 2. Tomcat 10.0 (Servlet 5.0)
  is EOL, so pinning to it just to avoid those fixes is a dead end.
- Grep for the removed members early (Phase 0) so the count is known before committing.

## The mechanical vehicle: Eclipse Transformer / Tomcat migration tool

Do **not** hand-edit 370 files. Use a tool that rewrites both `import javax.*` **and** the
hard-coded `"javax.servlet.*"` attribute-name string constants (which Jakarta EE 9 also renames
to `"jakarta.servlet.*"` — see `Globals.java`, ~24 constants):

- **Eclipse Transformer** (`org.eclipse.transformer:org.eclipse.transformer.cli`) or the
  **Apache Tomcat `migrate-jakarta-ee` / `jakartaee-migration`** tool. Both apply the standard
  `javax→jakarta` rename map (imports, FQNs, *and* string literals like `"javax.servlet.include.*"`).
- Run it over the vendored source tree in-place, review the diff, commit as one mechanical change.
- It correctly **leaves alone** the `javax.*` that stay in the JDK (`javax.naming`, `javax.xml.parsers`,
  `javax.xml.transform`, `javax.net.ssl`, `javax.crypto`, `javax.management`, `javax.security.*`, …).

This is what makes Track A tractable: the ~65 vendored files + ~30 string constants + your own
~58 source files are one tool pass plus manual cleanup, not a month of typing.

---

## What must change (inventory)

### A. Your own source — ~58 files (mostly `dysoweb/`)

Renamed-package imports (`javax.servlet`, `javax.servlet.http`, `javax.servlet.jsp`,
`javax.servlet.jsp.tagext`, `javax.servlet.descriptor`, `javax.servlet.FilterRegistration`):

- **`dysoweb-core`**: `DysowebServlet`, `DysowebFilter`, `DysowebListener`, `webenv/IWebProcessor`.
  These are the classes `web.xml` references directly — they must be `jakarta.servlet` for the
  Tomcat 10.1 container to load them.
- **`processor` (glue, not the vendored engine)**: `processor/*` (RequestProcessor, RequestMapper,
  RequestWrapper, ServletChain, FilterListChain, ServletContextWrapper — the last also uses
  `javax.servlet.descriptor` + `FilterRegistration`), `processor/definitions/*`, `servlet/wrapper/*`,
  `defaultservlet/DefaultServlet`.
- **`panel`**: RedirectFilter, RestServlet, SecurityFilter, SecurityServlet + all `tags/*`
  (11 tag handlers, `javax.servlet.jsp[.tagext]`).
- **`shell`**: `impl/SecurityFilter`, `impl/ShellServlet`.
- **`utils.tags`**: `impl/BundleServlet` + `tags/*` (6 tag handlers).
- **`test`** (sample bundle): FilterTest, ServletTest, TestTag.
- **`dysoweb.demo`**: `demo4/VoteServlet`, `demo5/SecurityFilter` (2 trivial files).

Hard-coded attribute-name **string constants** (`"javax.servlet.*"` → `"jakarta.servlet.*"`) —
the transformer handles these, but verify:
- `processor/defaultservlet/Globals.java` (~24: `include.*`, `forward.*`, `error.*`,
  `request.X509Certificate`, `request.cipher_suite`, `request.key_size`, `request.ssl_session`,
  `context.tempdir`)
- `processor/RequestProcessor.java`, `dysoweb-core/DysowebServlet.java`,
  `processor/defaultservlet/DefaultServlet.java`, `panel/SecurityFilter.java`
  (`javax.servlet.context.tempdir`, `include.path_info`).

**Do NOT touch** (stay `javax`, JDK-provided): `javax.naming[.directory]` (6 files),
`javax.xml.parsers`, `javax.xml.transform[.stream]`, `javax.net.ssl`, `javax.crypto`.

### B. Vendored engine — Track A rewrite (`com.requea.dysoweb.processor`)

- `com/requea/dysoweb/jasper/**` (105 files), `org/apache/tomcat/util/**` (242 files),
  `com/requea/dysoweb/org/**` (Catalina/Jasper/Jetty remnants). ~65 files import
  `javax.servlet`/`javax.servlet.jsp`/`javax.el`. Run the transformer over all of it.
- After the flip, compile against `jakarta.servlet-api:6.0.0` and fix removed-API call sites
  (see the Servlet 6.0 note above). This is the real work of Phase 2 — the rename is automatic,
  the API-delta fixes are manual.

### C. Dependencies (pom.xml)

- Swap in **every** module that declares them (managed in root `<dependencyManagement>`, then
  overridden per-module — api, processor, panel, shell, utils.tags, dysoweb-core):
  - `javax.servlet:javax.servlet-api:3.1.0` → `jakarta.servlet:jakarta.servlet-api:6.0.0`
  - `javax.servlet.jsp:javax.servlet.jsp-api:2.3.1` → `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.1`
- **processor** engine deps:
  - `org.apache.tomcat:tomcat-el-api:7.0.109` → `jakarta.el:jakarta.el-api:5.0.1`
  - `org.apache.tomcat:tomcat-jasper-el:7.0.109` → `org.apache.tomcat:tomcat-jasper-el:10.1.x`
  - `requeabuild:commons-el:5.5.20` → **drop** (superseded by jakarta EL; it's Tomcat-5.5 EOL).
  - `requeabuild:naming-resources:5.5.20` → keep for now (Track A preserves the `DirContext`
    resource path; its removal is part of the deferred engine-replacement track).
  - `tomcat-embed-logging-juli:7.0.109` → reassess; likely bump or drop.
  - Janino 3.1.12 stays (already Java-21-ready).
- **webapp** JAXB/activation stack is on `jakarta` *groupIds* but *javax-namespace versions* —
  bump for a true jakarta namespace: `jakarta.xml.bind-api` 2.3.2 → **4.0.x**,
  `jaxb-runtime` 2.3.2 → **4.0.x**, `jakarta.activation-api` 1.2.1 → **2.1.x**,
  `javax.xml.soap:javax.xml.soap-api:1.4.0` → `jakarta.xml.soap:jakarta.xml.soap-api:3.0.x`.
  (Only relevant if a deployed app actually uses JAXB/SOAP — otherwise these are dormant, as noted
  in the Java 21 plan.)

### D. OSGi wiring — bundle `<instructions>` (4 bundles)

Rename servlet/jsp/el package clauses in the `maven-bundle-plugin` `<Import-Package>` (and the
processor's `<Private-Package>`), with the new spec versions:
- **processor** (Import-Package l.135-152): `javax.servlet*`, `javax.servlet.jsp*;version="3.2.3"`,
  `javax.servlet.jsp.el;version="3.3.0"`, `javax.el;version="3.3.0"`, `javax.el.*`
  → `jakarta.servlet*` (6.0.0), `jakarta.servlet.jsp*` (3.1.0), `jakarta.el` (5.0.0).
  Also flip `org.apache.tomcat.*` in Private-Package if any package roots changed (they don't —
  `org.apache.tomcat.util` keeps its name).
- **panel** (l.152-195), **shell** (l.77-86), **utils.tags** (l.74-78): same servlet/jsp/el rename.
- **api, obr, service.obr** have no explicit javax package clauses (nothing to do there).

### E. OSGi wiring — `dysoweb/webapp/src/main/config/dysoweb.properties`

In `org.osgi.framework.system.packages`, rename + re-version the EE exports (the system bundle
exports these so bundles can import them):
- `javax.servlet;version="3.1.0"` (+ `.annotation`, `.descriptor`, `.resources`, `.http`)
  → `jakarta.servlet*;version="6.0.0"`
- `javax.servlet.jsp*;version="3.2.3"` → `jakarta.servlet.jsp*;version="3.1.0"`
- `javax.servlet.jsp.el;version="3.3.0"`, `javax.el;version="3.3.0"` → `jakarta.el;version="5.0.0"`
- `javax.annotation` (unversioned) → `jakarta.annotation;version="2.1.0"`
- `javax.websocket*;version="1.1"` → `jakarta.websocket*;version="2.1.0"` (only if used)
- The dormant EE set (mail, persistence, validation, ws.rs, xml.bind, xml.soap, xml.ws, jws, ejb,
  jms, resource, inject, interceptor, jacc) → `jakarta.*` equivalents, or delete if unused.
- **Leave untouched:** all JDK `javax.*` (crypto, imageio, management, naming, net, print, rmi,
  security.auth, sound, sql, swing, tools, script, `javax.xml` parsers/transform/etc., xml.crypto).

`bootdelegation` has no `javax` entries — no change there.

### F. Descriptors

- **`dysoweb/webapp/src/main/webapp/WEB-INF/web.xml`** — the only namespaced descriptor. Change
  `xmlns="http://java.sun.com/xml/ns/javaee"` → `https://jakarta.ee/xml/ns/jakartaee`,
  `web-app_3_0.xsd` → `web-app_6_0.xsd`, `version="3.0"` → `"6.0"`.
- The other 8 `web.xml` files are bare `<web-app>` (no namespace) — no edit strictly needed, though
  giving them a `version="6.0"` header is tidy.
- The 3 TLDs are JSP-1.2 DTD-style with custom URIs — the XML needs no namespace change; only the
  `<tag-class>` handlers they point at migrate (covered in A).
- No `.tag`/`.tagx`, no `faces-config`, no JSP `<%@ page import="javax…" %>`, and all `<%@ taglib %>`
  URIs are custom Requea namespaces — **JSP text needs no edits**.

---

## Phased execution

### Phase 0 — Prep & recon (before any edit)  ← DONE (2026-07-06)
1. Branch from the Java-21 work (`2.0.0-SNAPSHOT`). Confirm a clean Tomcat-9/JDK-21 baseline.
   **[done]** `dysoweb.war` (14 MB) builds BUILD SUCCESS on JDK 21.0.4 (`mvn clean install -pl
   dysoweb/webapp -am`). Runtime boot on Tomcat 9 not re-verified this pass (was ACTIVE in the
   Java 21 work). **Env gotcha:** the shell defaults to **JDK 11** (`JAVA_HOME=.../jdk-11.jdk`,
   Maven 3.6.3); JDK 21 is installed at `/Library/Java/JavaVirtualMachines/jdk-21.0.4.jdk/Contents/Home`
   — every build must `export JAVA_HOME=<that>` first, or it silently compiles on 11.
2. Grep the vendored fork for **Servlet 6.0-removed APIs**. **[done]** Surface is tiny — **3 files**:
   - `SingleThreadModel` (2 files): `jasper/servlet/JspServletWrapper.java` (import + `instanceof`
     at :457, `@SuppressWarnings` at :74) and `jasper/compiler/Generator.java:621` (emits the
     `implements` clause in generated JSP). Delete the STM branches — modern Jasper already dropped them.
   - `HttpSessionContext` / `getSessionContext()` (1 file): `processor/RequestWrapper.java` (import
     l.11 + overridden method l.142-143). Delete both.
   - `getRealPath`: **all 13 hits are `ServletContext.getRealPath`, which is RETAINED** in Servlet 6;
     the removed `ServletRequest.getRealPath` has zero hits. No action.
   - `encodeUrl`/`encodeRedirectUrl`/`setStatus(int,String)`/`getServlets`/`getServletNames`/session
     `putValue`/`getValue`/`isRequestedSessionIdFromUrl`: **zero hits.** No action.
3. Transformer tool. **[done]** Neither cached locally, but Maven Central reachable (HTTP 200) —
   `org.eclipse.transformer:org.eclipse.transformer.cli` or `org.apache.tomcat:jakartaee-migration`
   will resolve on demand.

### Phase 1 — Dependencies & OSGi config  ← DONE (2026-07-06)
Swap the API deps (C), rename bundle `<instructions>` (D), rewrite `system.packages` (E), and the
`web.xml` namespace (F). This is the config layer; it won't compile yet but sets the target.
**Applied:**
- Pinned versions (verified on Maven Central): `jakarta.servlet-api` **6.0.0**, `jakarta.servlet.jsp-api`
  **3.1.1**, `jakarta.el-api` **5.0.1**, `tomcat-jasper-el` **10.1.56**.
- Root `dependencyManagement` + every declaring module (api, processor, panel, shell, utils.tags,
  dysoweb-core, test, demo1-5) swapped `javax.servlet*`→`jakarta.servlet*`. Portlet left on javax.
- processor: dropped `commons-el:5.5.20`; `tomcat-el-api:7.0.109`→`10.1.56` (jakarta.el 5.0);
  `tomcat-jasper-el:7.0.109`→`10.1.56` (version-aligned so only one `jakarta.el` copy is embedded).
  Kept `naming-resources:5.5.20` (Track A) and `tomcat-embed-logging-juli:7.0.109` (not a namespace
  concern — flagged for later; note it's a Tomcat-7 artifact and modern Jasper uses `org.apache.juli`
  differently — watch at Phase 2 compile).
- Bundle `<Import-Package>` renamed in processor/panel/shell/utils.tags (+ demo poms) to
  `jakarta.servlet*;version="6.0.0"`, `jakarta.servlet.jsp*;version="3.1.0"`, `jakarta.el;version="5.0.0"`.
- `dysoweb/webapp/.../dysoweb.properties` `system.packages`: renamed servlet/jsp/el (with new spec
  versions), `jakarta.annotation` 2.1.0, `jakarta.websocket` 2.1.0, and the dormant EE set
  (mail/persistence/transaction/validation/ws.rs/xml.bind/xml.soap/xml.ws/jws/ejb/jms/resource/
  inject/interceptor/security.jacc). **Dropped** removed specs `javax.enterprise.deploy.*` (JSR-88)
  and `javax.xml.rpc.*` (JAX-RPC). **Left as javax:** all JDK packages incl. `javax.transaction.xa`,
  `javax.script`, `javax.xml.parsers/transform/stream/crypto`, and (out of scope) `javax.portlet`, `javax.media`.
- `dysoweb/webapp/WEB-INF/web.xml`: javaee 3.0 → jakartaee 6.0 (`web-app_6_0.xsd`).

**Not yet done in Phase 1:** the webapp JAXB/activation dep bumps (jakarta groupId but javax-version:
`jakarta.xml.bind-api` 2.3.2, `jaxb-runtime` 2.3.2, `jakarta.activation-api` 1.2.1, `javax.xml.soap-api`
1.4.0). These are dormant (only matter if a deployed app uses JAXB/SOAP) — deferred to when needed,
matching the Java 21 plan's treatment.

### Phase 2 — Code flip (the bulk)  ← IN PROGRESS (2026-07-06)
4. **[done]** Namespace flip. Instead of an external transformer, used a **deterministic scripted
   two-prefix rename** (`javax.servlet`→`jakarta.servlet`, `javax.el`→`jakarta.el`) over exactly the
   124 in-scope `.java` files under `dysoweb/` + `dysoweb.demo/`. This is safe *for this codebase*
   because the only renamed packages are servlet + el, and neither is a prefix of any kept package
   (`javax.naming/xml/crypto/net/...`), so a prefix rename cannot touch them — and it correctly
   rewrites the `"javax.servlet.*"` attribute-name string constants (`Globals.java` → the Servlet 6.0
   `"jakarta.servlet.*"` names). Fully auditable in `git diff`. Verified: 0 residual in-scope
   `javax.servlet/el`, keep-set intact (naming 9 / xml 12 / crypto 1 / net 14 files), `portlet/` +
   `sandbox/` untouched (still javax).
5. **[done] Removed-API fixups.** The Phase 0 grep under-counted (a broken ERE alternation, `\|`
   instead of `|`, silently hid the `HttpSession` deprecated methods). Corrected scan found the real
   surface — still small and confined to **3 files**:
   - `jasper/servlet/JspServletWrapper.java`: dropped the `SingleThreadModel` import, the
     `@SuppressWarnings` on the class, and the `instanceof SingleThreadModel` service branch → always
     `servlet.service(...)`.
   - `jasper/compiler/Generator.java`: removed the conditional that emitted `implements
     SingleThreadModel` into generated servlets.
   - `processor/RequestWrapper.java`: removed the 5 `HttpSession` methods gone in Servlet 6.0 —
     `getSessionContext` (+`HttpSessionContext` import), `getValue`, `getValueNames`, `putValue`,
     `removeValue` (their bodies delegated to now-removed `fSession.*` methods).
6. **[done] Compiler-surfaced API-delta (`ServletContext` impls).** Beyond the 3 known files, the
   `jakarta.servlet.ServletContext` interface grew/shrank between Servlet 3.1 and 6.0, breaking the
   two in-tree implementations. Fixed both:
   - `processor/ServletContextWrapper.java` (runtime, delegates to the container context): added the
     7 Servlet 4.0+ methods delegating to `fContext` — `getSessionTimeout`/`setSessionTimeout`,
     `get/setRequestCharacterEncoding`, `get/setResponseCharacterEncoding`, `addJspFile`. (Its stale
     `getServlet`/`getServlets`/`getServletNames`/`log(Exception,String)` carry no `@Override`, so they
     compile as dead methods — left as-is.)
   - `jasper/servlet/JspCServletContext.java` (offline JspC batch context): added the same 7 with
     minimal standalone defaults, and **deleted** the 4 stale `@Override` methods that no longer
     override anything (`getServlet`, `getServletNames`, `getServlets`, `log(Exception,String)` — all
     removed from `ServletContext` in Servlet 6.0).
   Processor module then compiled clean on JDK 21.
7. **[done]** Full reactor `mvn clean install` on JDK 21 → **BUILD SUCCESS** (all 24 reactor modules,
   incl. the still-javax portlet/sandbox modules). `dysoweb/webapp/target/dysoweb.war` produced.
   Verified in the WAR: processor bundle `Import-Package` = `jakarta.servlet[.http/.jsp*]`, `jakarta.el`
   with **no residual `javax.servlet/el`**; `dysoweb.properties` `system.packages` carries the jakarta
   exports at the right spec versions; `web.xml` is jakartaee 6.0; `@version` filtered correctly.
   No further API-delta surfaced in panel/shell/utils.tags/test/demos — the servlet/el flip + the
   `ServletContext` fixes were the whole surface.

**Phase 2 build side is COMPLETE.** The jakarta WAR builds. What is NOT yet done: it has not been run
on a container. Only bytecode/manifest correctness is proven, not runtime behavior.

### Phase 3 — Deploy & verify (manual — there is no test suite)  ← DONE (2026-07-06)
Deployed to **Tomcat 10.1.56 on JDK 21.0.4** at `/Library/Tomcat/Tomcat10.1-java21` (WAR as
`webapps/ROOT.war`; `bin/setenv.sh` sets JDK 21 + the three `--add-opens`).

7. **[done]** First boot failed OSGi resolution on **3 bundles** (processor, panel, and shell
   transitively) with `missing requirement osgi.contract; (osgi.contract=JakartaExpressionLanguage)
   (version=5.0.0)`. **Root cause:** the Tomcat 10.1 `tomcat-el-api`/`tomcat-jasper-el` jars carry a
   `Provide-Capability: osgi.contract=JakartaExpressionLanguage`, and bnd 6.3.1 auto-added a matching
   `Require-Capability` to any bundle using `jakarta.el` — which the Felix system bundle can't satisfy.
   (The javax build never hit this: bnd 2.5.0 didn't process contracts, and the old javax jars declared
   no contract.) **Fix:** added `<_contract>off</_contract>` to the processor and panel bundle
   `<instructions>` (only those two carried the requirement) — the EL/servlet APIs still resolve as
   package imports from `system.packages`. Rebuilt, confirmed the requirement is gone from both
   manifests.
8. **[done]** After the fix + a clean redeploy (wiped `dysoweb.home` + exploded `ROOT/`), Felix boots
   with **no resolution errors** and all `felix.auto.start.1` bundles reach ACTIVE (confirmed by the
   processor deploying each app's servlets/filters, and by a clean-shutdown log showing them being
   undeployed).
9. **[done]** `/dysoweb/panel` → HTTP **200**; `/` → 302 → `/dysoweb/panel/secure/bundles.jsp` → 200.
   Panel renders real HTML (`<title>Dysoweb Control Panel</title>`), i.e. the processor + panel +
   utils.tags + shell bundles are live and serving.
10. **[done — the riskiest path]** Runtime **JSP compilation works**: the log shows the vendored
    Tomcat-7 Jasper compiling `secure_jsp`, `top_jsp`, `mbar_jsp`, `footer_jsp` (custom `dw:`/`panel:`
    tags + EL) against the **EL 5.0 / Servlet 6.0** runtime with zero `ELException`/`Exception`/`SEVERE`.
    **[done, 2026-07-06]** Both remaining items exercised after the user registered the server (auth
    key + password):
    - **OBR install works on jakarta:** authenticated via `/dysoweb/panel/auth`, the setup page listed
      OBR products (`com.requea.dynalib`, `.mariadb`), and installing `com.requea.dynalib` downloaded +
      started its bundles (commons-collections, lucene-core, …) from the remote Requea OBR over HTTPS
      (client cert) — progress 17%→90%→"Application installed successfully", no errors. Confirms the
      whole OBR client + RepositoryAdmin + install path runs on jakarta.
    - **Freshly-installed demo bundle works:** installed `com.requea.dysoweb.demo4` via the Felix web
      shell (`/dysoweb/shell/exec`, `install file:…` + `start`), reached ACTIVE. `/demo4/vote`
      (VoteServlet) → 200, `/demo4/vote.jsp` → 200 rendering the vote form, and `/demo4/vote?op=img` →
      a **300×180 PNG** from the embedded **JFreeChart 1.0.2** — servlet + runtime-compiled JSP +
      embedded-lib Java2D rendering all working on jakarta.

**Two findings during the exercise (both outside the core migration):**
- **`apps.jsp` → HTTP 500 (pre-existing, NOT a regression).** `<panel:installedfeatures>`/
  `installedfeature` are referenced by the page but are **not defined in `panel.tld` and have no
  handler class** — `git grep` finds no such handler in any tracked state, so they were never
  implemented. It fails identically on the old javax build; the migration didn't touch `panel.tld`.
  Dead/broken page, unrelated to jakarta.
- **`demo4` needed a bnd import exclusion.** The bundle embeds `jfreechart-1.0.2.jar`, whose
  `org.jfree.chart.servlet.*` utility classes reference `javax.servlet`; bnd's trailing `*` therefore
  added `javax.servlet`/`javax.servlet.http` to demo4's `Import-Package`, which the jakarta system
  bundle no longer exports → the bundle wouldn't resolve. VoteServlet never calls those helpers, so
  added `!javax.servlet, !javax.servlet.http` before the `*` in `demo4/pom.xml`. **General lesson for
  app bundles:** an embedded legacy lib with `javax.servlet` refs must have those imports excluded or
  made `resolution:=optional`. (Only demo4 embeds such a lib; the other demos don't.)

**Environment note:** a pre-existing `SEVERE` about the Apache Tomcat Native library (2.0.9 vs required
2.0.15) is unrelated — the optional APR connector; Tomcat falls back to NIO (`http-nio-8080`) fine.

**Result: the jakarta migration is functionally verified on Tomcat 10.1 / JDK 21.**

### Phase 4 — Container detection & flags
11. `DysowebServlet.startFelix` container detection still keys off `catalina.base` etc. — Tomcat 10.1
    keeps those, so detection should hold; verify.
12. Re-check the JDK-21 `--add-opens` flags still apply under Tomcat 10.1.

## Watch-items / risk

- **Highest risk: runtime JSP compilation.** The vendored Jasper is Tomcat-7-era (JSP 2.2/EL 2.2)
  now generating code against a Servlet 6.0 / EL 5.0 runtime. The namespace flip compiles; the
  question is whether generated servlet code and EL evaluation behave under the newer runtime. Test
  a real JSP with tags, EL, and includes early.
- The `"javax.servlet.*"` **string constants** are as important as the imports — a missed one
  (e.g. an include/forward attribute name) fails silently at runtime, not at compile time. Diff
  `Globals.java` carefully after the transformer runs.
- The processor's EL stack (`tomcat-jasper-el`) jumps 7.0 → 10.1 — bigger behavioral delta than the
  API rename. If EL misbehaves, this is the first suspect.
- Version duplicated across every module pom + root `dysowebVersion` — unchanged concern.
- Build still pulls `requeabuild:*` / Requea OBR from `maven.requea.com`; dropping `commons-el`
  removes one such dependency, but `naming-resources:5.5.20` remains.

## Deferred (out of scope for this pass)

- **Replace the vendored Jasper with modern Tomcat 10.1 Jasper.** The real modernization: delete the
  ~103k-line fork and depend on `org.apache.tomcat:tomcat-jasper`. Blocked on rewriting the resource
  layer — `BundleDirContext`/`DefaultServlet` are built on JNDI `DirContext` (Tomcat 5.5/6 pattern);
  modern Jasper uses `WebResourceRoot`. Also re-integrating the per-bundle OSGi classloader with the
  modern `JspServletWrapper`. Weeks-to-months; its own project.
- **`portlet/` module** → `jakarta` (blocked on the Portlet-spec question + its existing
  deploy-readiness rot from the Java 21 work).
- Drop `naming-resources:5.5.20` and the stale Catalina `ServerInfo`/Jetty remnants (fall out of the
  engine-replacement track above).
