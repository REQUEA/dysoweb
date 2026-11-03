# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Dysoweb is a runtime that embeds an OSGi container (Apache Felix) inside a single Java
servlet WAR. It lets standard servlet/JSP web applications be packaged as OSGi bundles
and installed, updated, and removed *at runtime* over the network — without redeploying
the WAR. The shipped `dysoweb.war` is a thin bootstrap: it starts Felix, auto-starts a
small set of core bundles, and delegates every HTTP request to whichever OSGi bundle is
currently registered as the active web processor.

Target runtime is Java 8 (`maven-compiler-plugin` source/target = 1.8). It deploys into
standard servlet containers (Tomcat, JBoss, JOnAS, WebLogic — see container detection in
`DysowebServlet.startFelix`).

## Build

Maven multi-module reactor, parent `com.requea.dysoweb:main`. Build from the repo root:

```bash
mvn clean install          # build all modules; produces dysoweb/webapp/target/dysoweb.war
mvn clean install -pl dysoweb/com.requea.dysoweb.panel -am   # one bundle + its deps
mvn -o install             # offline (after deps are cached)
```

Releases use `maven-release-plugin` with tag format `dysoweb-@{project.version}`.

Notes that will bite you:
- The build resolves dependencies from `maven.requea.com` and a few other remote repos
  (see `<repositories>` in the root `pom.xml`). Some artifacts (e.g. `requeabuild:*`,
  the Requea OBR) live only there. A build can require network access to that repo.
- The version number is duplicated across every module `pom.xml` **and** the
  `dysowebVersion` property in the root pom (which must equal the version without
  `-SNAPSHOT`). A version bump touches all of them.
- There is **no automated unit test suite.** `dysoweb/com.requea.dysoweb.test` is a
  *deployable sample bundle* (FilterTest/ServletTest/TestTag), not JUnit tests. The
  `dysoweb.demo` modules are likewise sample bundles installed at runtime to exercise
  the platform, not standalone WARs (their `web.xml` is intentionally empty).

## Module layout

The reactor has four top-level modules: `dysoweb` (the platform), `dysoweb.demo`,
`portlet`, and `sandbox`. The platform lives under `dysoweb/`:

- **`dysoweb-core`** — plain JAR, the only code that runs *outside* OSGi. Contains
  `DysowebServlet`, `DysowebListener`, `AutoActivator`, and the `com.requea.webenv`
  bridge interfaces (`IWebProcessor`). This is what `web.xml` references directly.
- **`webapp`** — packaging `war`, `finalName=dysoweb`. Assembles the deliverable: it
  copies the core bundle JARs (api, processor, utils.tags, shell, panel, obr,
  service.obr) into `WEB-INF/bundle/`, and filters `dysoweb.properties` to inject the
  build version (`@version` token → `${project.version}` via antrun).
- **`com.requea.dysoweb.processor`** — packaging `bundle`. The heavyweight bundle: a
  reimplemented servlet/JSP engine (embedded **Jasper** + Janino/tomcat-el compiler,
  `DefaultServlet`, request mapping/wrapping). Registers an `IWebProcessor` service.
- **`com.requea.dysoweb.panel`** — packaging `bundle`. The management UI at
  `/dysoweb/panel` (JSP + custom tags + REST/Swagger servlet): authenticates with an
  auth key, then installs/updates bundles from the OBR. Also bundles `com.btr.proxy`
  for proxy autodetection (PAC/IE settings).
- **`com.requea.dysoweb.obr` / `.service.obr`** — OSGi Bundle Repository client; the
  remote install/update mechanism the panel drives.
- **`com.requea.dysoweb.api`**, **`.shell`**, **`.utils.tags`**, **`.log4j`** —
  supporting bundles (public API, Felix shell, JSP tag utils, log4j wrapper).

Other top-level modules: `portlet/` (portlet-API variant of the processor),
`sandbox/` (experimental, e.g. an XMPP shell).

## Runtime architecture (the important part)

Read these together to understand request flow; no single file tells the whole story.

1. **Bootstrap.** `web.xml` maps `DysowebServlet` to `/`, `` and `*.jsp`, and registers
   `DysowebListener`. On `init()`, `DysowebServlet.startFelix()` loads
   `WEB-INF/classes/dysoweb.properties`, creates an embedded Felix `Framework`, and
   starts it. The framework's bundle cache goes to **`dysoweb.home`** — resolved from
   the JNDI env-entry `dysoweb.home`, else auto-detected from container system
   properties (`catalina.base`, `jboss.home.dir`, `jonas.base`, WebLogic `DOMAIN_HOME`),
   else the servlet temp dir.

2. **`dysoweb.properties`** is Felix configuration, not app config. It defines:
   - `felix.auto.start.1` — the core bundles auto-started at boot (resolved against
     `WEB-INF/bundle/`; `@version` is build-filtered).
   - `org.osgi.framework.system.packages` — the (large, hand-maintained) list of JDK /
     `javax.*` packages exported from the system bundle so bundles can import them.
   - `org.osgi.framework.bootdelegation` — packages delegated straight to the boot
     classloader. Adding library support to the platform usually means editing these
     lists here.

3. **Active processor pattern.** Bundles register `com.requea.webenv.IWebProcessor` as
   an OSGi service. `DysowebServlet` tracks these via a `ServiceListener` and keeps a
   single **active processor** — always the one with the highest `Bundle-Version`
   (`getBestVersion`). `service()` forwards every request to
   `fActiveProcessor.process(...)`; `DysowebListener` forwards all context/session
   lifecycle events the same way. This is how a freshly installed newer bundle
   transparently takes over request handling at runtime. The processor is also published
   as the servlet-context attribute `com.requea.dysoweb.processor`.

4. **Inside a bundle.** The processor bundle parses each installed bundle's `web.xml`
   and runs its servlets/filters/JSPs through its own Jasper-based engine, isolated by
   the bundle's OSGi classloader. JSPs are compiled at runtime.

## Conventions

- A new platform bundle is an OSGi bundle: packaging `bundle`, built with
  `maven-bundle-plugin`, with `Bundle-SymbolicName` / `Bundle-Activator` /
  `Import-Package` / `Export-Package` declared in the pom `<instructions>` (see
  `com.requea.dysoweb.processor/pom.xml`). To have it auto-start with the platform, add
  its JAR to the `webapp` module's dependency-copy list **and** to `felix.auto.start.1`
  in `dysoweb.properties`. Otherwise it is installed at runtime via the panel/OBR.
- Exported package versions use the `${dysowebVersion}` property, not the full
  `-SNAPSHOT` project version.
- License header (Apache 2.0, "Copyright 2007 Requea") is prepended to source and pom
  files; preserve it.

## History / changelog

`CHANGELIST` is a human-written running log of notable changes per version (container
compatibility fixes, Felix/Jasper upgrades, OBR/proxy behavior). Skim it for context on
why platform-specific workarounds exist.
