# Plan: Running Dysoweb on Java 21

## The core strategic decision

There are two very different meanings of "run on Java 21". Pick one before touching anything.

| | **Path A — Run on the Java 21 JVM, keep `javax.*`** | **Path B — Migrate to Jakarta EE 9+ (`jakarta.*`)** |
|---|---|---|
| Servlet namespace | stays `javax.servlet` | rewrite to `jakarta.servlet` |
| Servlet container | Tomcat **9.0.x**, Jetty 9/10, JBoss/EAP 7 (all run on Java 21) | Tomcat 10+, Jetty 11+, WildFly 27+ |
| Embedded Jasper (vendored Tomcat 7 engine in the processor bundle) | keep as-is, recompile | must be replaced/rewritten — enormous |
| Effort | days | weeks-to-months |

**Recommendation: Path A.** The Java 21 *JVM* runs `javax.servlet` bytecode fine. Only *modern containers* (Tomcat 10+) force `jakarta.*`. Dysoweb ships its own embedded servlet/JSP engine (a vendored Tomcat 7 Jasper under `com.requea.dysoweb.processor/src/.../org/apache/tomcat` and `.../jasper`), so a `jakarta` migration means rewriting that engine. This plan assumes Path A and flags where Path B diverges.

## What actually breaks on Java 21

1. **Toolchain too old for Java 21 bytecode (class file v65).**
   - `maven-compiler-plugin` pinned to `source/target = 1.8` (root `pom.xml`).
   - `maven-bundle-plugin` `1.4.3` (root pluginMgmt), `2.5.0` (processor), `1.0.0` (demo1-5, sandbox). These embed an old BND that cannot parse Java 11+ class files; the `bundle` packaging step fails once anything targets a newer release.
2. **`dysoweb.properties` exports Java EE packages the JDK no longer has** (`dysoweb/webapp/src/main/config/dysoweb.properties`). Removed from the JDK in Java 11; on Java 21 the system bundle cannot export them and importers fail to resolve. Offenders: `javax.xml.bind`, `javax.activation`, `javax.xml.ws`, `javax.jws`, `javax.jws.soap`, `javax.xml.rpc.*`, `javax.xml.soap`, `javax.rmi.CORBA`, `javax.transaction*`, `javax.jms`, `javax.resource.*`, `javax.ejb.*`, `javax.annotation`, `javax.mail.*`, `javax.persistence.*`, `javax.enterprise.deploy.*`.
3. **Apache Felix 6.0.2.1 is not a Java 21 framework** (`pom.xml`). Use 7.0.5+.
4. **JSP-compiler path assumes `tools.jar`** (`IntrospectionUtils.java:665-675`), removed in Java 9. Likely dead code (Janino is the real compiler) but a latent trap.
5. **Janino 3.1.6** (`processor/pom.xml`) — bump to >=3.1.12 for Java 21 bytecode.
6. **`bootdelegation` lists `sun.*`, `com.sun.*`, `oracle.*`** (`dysoweb.properties:1`). Strong encapsulation may make some inaccessible. Watch at runtime.
7. **No test suite** — verification is manual (build WAR, deploy, exercise panel + a sample bundle).

## Step-by-step

### Phase 1 — Toolchain (get it compiling & bundling)  ← DONE, verified: `dysoweb.war` builds on JDK 21.0.4
Applied and confirmed by building `mvn clean install -pl dysoweb/webapp -am` on JDK 21 (BUILD SUCCESS; all bundles at bytecode v65):
1. Root `pom.xml`: `maven-compiler-plugin` -> `3.13.0`; replaced `<source>/<target>` with `<release>21</release>`. [done]
2. `maven-bundle-plugin` -> `5.1.9` everywhere declared (root, processor, demo1-5, sandbox, portlet.processor). [done]
3. `org.codehaus.janino:janino` + `commons-compiler` -> `3.1.12` (processor). [done]
4. **webapp `pom.xml` build-plugin bumps** (Maven 3.6.3 defaults are too old for a Java 21 JVM):
   - `maven-war-plugin` pinned to `3.4.0` (default 2.2 fails: "Cannot access defaults field of Properties").
   - `maven-antrun-plugin` -> `3.1.0`, and its `<tasks>` renamed to `<target>` (3.x removed `<tasks>`).
   - `maven-dependency-plugin` -> `3.6.1`. [done]
5. **bnd 5.x strictness fixes** (bnd 2.5.0 only warned):
   - Processor `pom.xml`: added `<Private-Package>com.requea.dysoweb.*, org.apache.tomcat.*</Private-Package>` — modern bnd drops non-exported packages otherwise, which lost the `Bundle-Activator` class. [done]
   - **Restored `com/requea/dysoweb/bundle/Activator.java`** in the processor — it was accidentally deleted by commit `21f9de0` ("security updates", Apr 2025) while the `<Bundle-Activator>` header stayed. This class *is* the platform: it does `new RequestProcessor()`, `registerService(IWebProcessor...)`, and deploy/undeploy on bundle events. Nothing else registers it. bnd 5.x made the dangling header fatal, which surfaced the pre-existing bug. **Confirm this deletion was unintended** (it looks so — the branch would have no active web processor without it). [done — restored from `21f9de0^`]
6. **Nashorn removal (JEP 372, gone since Java 15):** `panel/.../RestServlet.java` imported `jdk.nashorn.api.scripting.JSObject` — an **unused dead import**; removed it. [done]

Not yet built on JDK 21: `dysoweb.demo`, `portlet`, `sandbox` modules (bundle-plugin already bumped; build them if you ship them).

### Phase 2 — Framework & OSGi runtime  ← DONE (rebuilt WAR ready to redeploy)
**Symptom (first Tomcat 9 + JDK 21 boot):** every core bundle failed OSGi resolution with
`missing requirement osgi.wiring.package; (osgi.wiring.package=java.lang)` / `java.io`, and the
processor failed on `org.osgi.framework.launch;version>=1.2.0`.

**Root cause:** `dysoweb.properties` *fully overrides* `org.osgi.framework.system.packages` with a
hand-maintained list. Felix normally appends the JDK's `java.*` set (via `${jre-...}`) and the
`org.osgi.framework.*` subpackages to that property — the override dropped both. On Java 8 it worked
only because old bnd (2.5.0) never emitted `java.*` imports; modern bnd (6.3.1, via bundle-plugin 5.1.9)
imports `java.*` whenever `Import-Package` contains `*` (the default). Felix 6.0.2.1 also only defines
`jre-1.6/1.7/1.8` statically (no `jre-21`), so it can't self-heal on Java 21.

**Fixes applied:**
5. `org.apache.felix.framework` `6.0.2.1` -> **`7.0.5`** (root `pom.xml`; flows into `WEB-INF/lib` via `dysoweb-core`). Confirmed 7.0.5 in the rebuilt WAR. [done]
6. `dysoweb/webapp/src/main/config/dysoweb.properties`, in `org.osgi.framework.system.packages`:
   - Added the missing OSGi framework subpackages: **`org.osgi.framework.launch;version="1.2"`** (fixes the processor) plus `framework.dto/namespace/startlevel/hooks.*`, `org.osgi.resource`, `org.osgi.dto`.
   - Added the full JDK **`java.*` export set (66 packages)** — the canonical Felix `jre-base..1.8` list plus Java 9+ additions (`java.lang.module`, `java.net.http`, `java.lang.constant`, `java.util.random`). Exported unversioned to match bnd's unversioned `java.*` imports. No duplication (the list had zero `java.*` before). [done]

**Still open in Phase 2 (does NOT block boot — dormant until a runtime app needs them):**
- The `javax.*` list still declares removed-JEE packages (`javax.mail`, `javax.persistence`, `javax.xml.bind`, `javax.xml.ws`, `javax.activation`, `javax.jms`, `javax.ejb`, `javax.resource`, `javax.rmi.CORBA`, full `javax.transaction`, `javax.ws.rs`, `javax.validation`, `javax.websocket`, `javax.media`, `javax.enterprise.*`). The system bundle "exports" them but the classes don't exist in JDK 21 -> any bundle importing them resolves but hits `NoClassDefFoundError` at class load. When a deployed app needs one (most likely JAXB + activation), provide it as a real bundle: `jakarta.xml.bind-api` + `jaxb-runtime` 2.3.x (still `javax.xml.bind`), `jakarta.activation` 1.2.x — add to the `webapp` dependency-copy list and `felix.auto.start.1`.
- Apply the same `system.packages` edits to `portlet/portlet/src/main/config/dysoweb.properties` if building the portlet module.

**2nd boot result:** the `java.*` / `framework.launch` errors were gone (7 failures -> 1). Remaining: `utils.tags`
failed on `javax.servlet.jsp;version>=2.3.0 <3.0.0` (and `panel` transitively). Cause: the system bundle
exports `javax.servlet.jsp;version="3.2.3"`, but modern bnd derived the import range `[2.3,3)` from the
JSP-API 2.3.1 artifact -> `3.2.3` is outside it. shell and the processor already pin `version="3.2.3"`
explicitly; utils.tags relied on the bnd default (old bnd left JSP imports unversioned, so it matched).
**Fix:** added an explicit `<Import-Package>` to `com.requea.dysoweb.utils.tags/pom.xml` pinning
`javax.servlet.jsp;version="3.2.3"` and `javax.servlet.jsp.tagext;version="3.2.3"` (open-ended min, matches
the export), with `*` for the rest. Rebuilt: utils.tags now imports `javax.servlet.jsp;version="3.2.3"`. [done]

**3rd boot (after clearing the stale Felix cache):** utils.tags resolved; only `panel` failed on
`org.osgi.framework;version>=1.10.0 <2.0.0`. Cause: Felix 7.0.5 provides `org.osgi.framework` at **1.10**
(R8) and panel was compiled against it, but the `system.packages` override still declared `1.9`.
**Fix:** bumped the export `org.osgi.framework;version="1.9"` -> `"1.10"` in `dysoweb.properties`
(satisfies panel's `[1.10,2)` and stays within the other bundles' `[1.8,2)`). [done]

### Deployment gotcha (cost us a round-trip)
The Felix bundle cache lives at **`${catalina.base}/dysoweb.home/bundles`**. Because the version stays
`1.0.54-SNAPSHOT`, Felix reuses cached bundle revisions across restarts and ignores a rebuilt jar with the
same version. **When redeploying a rebuilt bundle, delete `${catalina.base}/dysoweb.home/bundles` (and the
exploded `webapps/ROOT/`) first.** A pure `dysoweb.properties` change (framework config, read fresh at
startup) does NOT need a cache wipe — but the new WAR must actually be re-exploded so the updated
properties file is on disk.

**-> ACTION: redeploy the rebuilt `dysoweb/webapp/target/dysoweb.war` (ensure `webapps/ROOT` is re-exploded
so the new `dysoweb.properties` is used) and re-check the logs. This was the last bundle in `felix.auto.start.1`.**

### Phase 3 — Code fixes
8. Guard the `tools.jar` lookup in `IntrospectionUtils.java` (no-op on Java 9+) or confirm dead.
9. After Phase 1 compiles, `grep -rn "javax.xml.bind\|javax.activation\|com.sun.\|sun.misc" --include=*.java` and add standalone deps where used.

### Phase 4 — Runtime launcher flags (deployment)
10. Add JVM flags to the container running Dysoweb as needed:
    `--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED`.
11. Deploy into a Java-21-compatible javax container — Tomcat 9.0.x. Verify container detection in `DysowebServlet.startFelix`.

### Phase 5 — Verify (manual)
12. `mvn clean install` on JDK 21 -> `dysoweb/webapp/target/dysoweb.war` builds; all `bundle` modules package.
13. Deploy into Tomcat 9 on Java 21; confirm Felix boots and all `felix.auto.start.1` bundles reach ACTIVE.
14. Load `/dysoweb/panel`, authenticate, install a sample bundle from OBR.
15. Deploy a `dysoweb.demo` bundle; hit a JSP (Jasper/Janino runtime compile — riskiest path) and a servlet/filter.

## Watch-items
- The vendored Tomcat 7 Jasper is the highest-risk component; runtime JSP compilation on Java 21 is where the first real failure is likely.
- Version is duplicated across every module pom + the root `dysowebVersion` property; a release bump moves all together.
- Build pulls `requeabuild:*` and the Requea OBR from `maven.requea.com`; must be reachable.
- Path B trigger: if you must run under Tomcat 10+/WildFly 27+, this plan is insufficient — that's the full `javax->jakarta` rewrite of the processor bundle.

---

## Follow-up: make the `portlet` WAR deploy-ready (deferred)

Status: the `portlet/portlet` module **builds** on JDK 21 and its `dysoweb.properties` OSGi resolution is
fixed, but it will **not boot cleanly** yet due to pre-existing bundle-wiring rot (unrelated to Java 21 —
the module drifted out of sync with the main `dysoweb/webapp`). Deferred by owner decision (2026-07).

### Already done
- `portlet/portlet/pom.xml`: pinned `maven-war-plugin` 3.4.0, `maven-antrun-plugin` 3.1.0 (`<tasks>`->`<target>`), `maven-dependency-plugin` 3.6.1.
- `portlet/portlet/src/main/config/dysoweb.properties`: replaced the stale `bootdelegation` + minimal `system.packages` with the webapp's proven versions (adds `java.*`, `org.osgi.framework.launch`, `org.osgi.framework`=1.10, `javax.servlet*`/`javax.el`/`javax.xml.*`, etc.). Kept the portlet's own `felix.auto.start.1`.

### Remaining work (the reason it won't boot)
`felix.auto.start.1` and the `maven-dependency-plugin` copy-list are out of sync with each other and with
the bundles panel needs. Two concrete problems:

1. **`felix.auto.start.1` lists 3 jars that the build never copies** — `com.requea.dysoweb.commons-logging-1.1.jar`,
   `org.apache.felix.shell-1.0.0.jar`, `org.apache.felix.shell.tui-1.0.0-dysoweb.jar`. The WAR actually contains
   `org.apache.felix.shell-1.4.3.jar` (not in the start list). Felix will fail/skip the phantom entries.
2. **panel imports `com.requea.dysoweb.service.obr`, but the portlet copies neither `service.obr` nor `obr`.**
   So panel can't resolve (same class of failure the webapp hit early on).

### Plan
Use `dysoweb/webapp` as the reference (its `felix.auto.start.1` + dependency-copy list are the known-good set):
1. **Decide portlet scope first:** does the portlet variant actually need the full `panel`/OBR stack?
   - If **yes** (mirror webapp): add `org.osgi.service.obr`, `com.requea.dysoweb.service.obr`,
     `com.requea.dysoweb.obr` to the `maven-dependency-plugin` `<artifactItems>` in `portlet/portlet/pom.xml`
     **and** to `felix.auto.start.1`. Fix the felix.shell entry to `org.apache.felix.shell-1.4.3.jar` and drop
     the 3 phantom jars.
   - If **no** (slim portlet): remove `panel` from both the copy-list and `felix.auto.start.1` instead, so nothing
     imports `service.obr`. Still drop the 3 phantom jars and fix the shell version.
2. Reconcile `felix.auto.start.1` names 1:1 with the jars the dependency-plugin copies (exact filenames; `@version`
   is build-filtered for the dysoweb bundles).
3. Rebuild `mvn install -pl portlet/portlet -am`, deploy on Tomcat 9 + JDK 21 **with a cleared Felix cache**
   (`${catalina.base}/dysoweb.home/bundles`), confirm every auto-start bundle reaches ACTIVE.
4. Then the runtime exercise (portlet render + a JSP) as with the main webapp.

Note: the portlet has its *own* Felix cache dir if deployed under a different context/`dysoweb.home`; clear the
right one. The `com.requea.dysoweb.portlet.processor` is the portlet analogue of the servlet `processor` bundle.
