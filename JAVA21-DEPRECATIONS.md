# Plan: Remove use of "deprecated for removal" APIs (Java 21 hygiene)

Follow-up to `JAVA21-MIGRATION.md`. The build succeeds on JDK 21, but `javac` emits **81
`[removal]` warnings** — uses of APIs annotated `@Deprecated(forRemoval=true)`. These still
compile and run on Java 21; the point of this work is forward-compatibility (several are already
*removed or permanently disabled* in JDK 24/25) and clean builds.

Reproduce the full list:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.0.4.jdk/Contents/Home
mvn -q clean install -DskipTests -Dmaven.compiler.showDeprecation=true 2>&1 | grep "marked for removal"
```

## Inventory (81 warnings)

| API (removal-flagged) | Count | Where | Fix effort | Priority |
|---|---:|---|---|---|
| Boxed constructors `new Long/Integer/Boolean/Byte/Short/Float/Double(...)` | **42** | mostly our code + some vendored | trivial, mechanical | **P1** |
| `java.security.AccessController` (+ `SecurityManager`, `System.getSecurityManager`, `java.security.Policy`) | **33** | almost all vendored Jasper | medium–large | **P3** |
| `java.lang.ThreadDeath` | **3** | vendored `ExceptionUtils` (×2 copies) | trivial | **P2** |
| `jakarta.el.ELResolver.getFeatureDescriptors` | **2** | vendored `ELResolverImpl`, `JspApplicationContextImpl` | trivial | **P2** |

Our-code files (32 sites): `processor/RequestProcessor.java` (15), `defaultservlet/DefaultServlet.java`
(6), `demo4/VoteServlet.java` (4), `panel/utils/Entities.java` (4), `processor/RequestMapper.java` (1),
`bundlerepository/RepositoryImpl.java` (1), `panel/InstallServlet.java` (1). The remaining ~49 sites are
in the vendored Jasper tree (`com/requea/dysoweb/jasper/**`, `com/requea/dysoweb/org/apache/**`,
`org/apache/tomcat/**`).

**Why it matters (removal timeline):**
- **Boxed primitive constructors** — deprecated-for-removal since Java 9; can be dropped in any release.
- **SecurityManager / AccessController / Policy** — JEP 411 (deprecated, Java 17); **JEP 486 permanently
  disabled the Security Manager in JDK 24**; `AccessController.doPrivileged` is now a no-op pass-through
  and the class is on track for removal.
- **ThreadDeath** — deprecated-for-removal (Thread.stop degraded); slated for removal.
- **`ELResolver.getFeatureDescriptors`** — deprecated-for-removal in Jakarta EL 5.0; the runtime no longer
  calls it (default now returns `null`).

Doing **P1 + P2 clears ~47 of 81** with near-zero risk. **P3 (the 33 SecurityManager sites)** is the bulk
of the rest and is best folded into the deferred **Track B — modern-Jasper replacement** (`JAKARTA-MIGRATION.md`),
since modern Tomcat 10.1 Jasper has already removed all SecurityManager usage.

---

## P1 — Boxed primitive constructors (42 sites) — do now

Pure mechanical, behavior-preserving (the `valueOf` forms cache small values, so this is also a minor
improvement). Transforms:

| From | To |
|---|---|
| `new Long(x)` | `Long.valueOf(x)` |
| `new Integer(x)` | `Integer.valueOf(x)` |
| `new Boolean(b)` (boolean) | `Boolean.valueOf(b)` — or `Boolean.TRUE/FALSE` |
| `new Boolean("...")` (String) | `Boolean.valueOf(str)` |
| `new Byte/Short/Long/Float/Double/Integer("...")` (String) | `Xxx.valueOf(str)` (or `Xxx.parseXxx(str)` if a primitive is wanted) |

Approach:
1. **Fix our code first** (32 of the boxed sites live here; the heavy one is `RequestProcessor.java` with
   15 `new Long(long)`/`new Integer(int)`). These are safe hand/`sed` edits — e.g.
   `sed -i '' -E 's/new (Long|Integer|Boolean|Byte|Short|Float|Double)\(/\1.valueOf(/g'` **per file, then
   review the diff** (watch the `Boolean(boolean)`→`valueOf` and any `new Integer(String)` that a caller
   expects as `int` — `Integer.valueOf` returns `Integer`, which autoboxes fine in these sites).
2. The vendored boxed-constructor sites (e.g. `PageContextImpl:866`, jasper `Node`/`PageInfo`/etc.) can take
   the same transform — it's safe there too. Or leave them to fall out of the Jasper replacement (P3/Track B).
   Recommendation: apply to vendored as well since it's a one-line-per-site safe change and clears the warnings.
3. Rebuild, confirm these warnings are gone.

Caveat: do **not** blindly `sed` the whole tree — scope to the files the warning list names, and review each
diff (a `new Boolean(str)` inside a ternary, etc.). ~42 edits total.

## P2 — ThreadDeath (3) and ELResolver.getFeatureDescriptors (2) — do now

**ThreadDeath** — in both `ExceptionUtils` copies (`jasper/util/ExceptionUtils.java` and
`org/apache/tomcat/util/ExceptionUtils.java`), `handleThrowable` starts with:
```java
if (t instanceof ThreadDeath) {
    throw (ThreadDeath) t;
}
```
`ThreadDeath` was only ever thrown by the degraded/removed `Thread.stop()`. Remove this branch (matches
modern Tomcat). The following `StackOverflowError`/`VirtualMachineError` handling is unchanged, so fatal
errors still propagate. Also check the third site (`ScriptingVariabler`/`Node` area) flagged in the log.

**`getFeatureDescriptors`** — `jasper/el/ELResolverImpl.java:127` and `JspApplicationContextImpl` override
`Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext, Object)` and delegate. EL 5.0 deprecated it
for removal and no longer calls it (default returns `null`). **Delete the overrides** (inherit the default).
If a delegate call is kept for older callers, guard/remove per the log sites. 2 edits.

## P3 — SecurityManager / AccessController / Policy (33 sites) — defer to Track B (or strip)

Almost entirely in the vendored Jasper: `PageContextImpl` (12 `doPrivileged`), `JspFactoryImpl` (3),
`ParserUtils` (3), `JspDocumentParser` (3), plus `JspWriterImpl`, `JspRuntimeLibrary`,
`JspApplicationContextImpl`, `JspServlet`, `ELFunctionMapper`, `SecurityClassLoad`, `JasperLoader`,
`JIoEndpoint`. ~28 `doPrivileged` blocks across 10 files, plus `getSecurityManager()`/`Policy` in the
security-integration classes.

Two ways to resolve, pick per how soon you need Java 24+ support:

- **Preferred: fold into Track B (replace the vendored Tomcat-7 Jasper with modern Tomcat 10.1 Jasper).**
  Modern Jasper removed all SecurityManager/`doPrivileged` code, so this category disappears wholesale along
  with the ~103k-line fork. No point hand-editing code we plan to delete.
- **If Track B stays deferred and you must run on JDK 24+ sooner:** mechanically strip the SecurityManager
  layer. Since Dysoweb runs with **no SecurityManager installed**, `AccessController.doPrivileged(action)` is
  already just `action.run()`. Unwrap each block:
  ```java
  // before
  Xxx r = AccessController.doPrivileged((PrivilegedAction<Xxx>) () -> { ... return v; });
  // after
  Xxx r = ( () -> { ... return v; } ).get();   // or inline the body directly
  ```
  Handle `PrivilegedExceptionAction` (unwrap `PrivilegedActionException`), drop `getSecurityManager()`
  null-checks (always null), and remove `SecurityClassLoad`/`Policy` wiring and `JasperLoader`'s
  `AccessControlContext`. This is careful, per-site work (~33 sites) and must be regression-tested by the
  Phase 3 runtime exercise (panel + a demo JSP). **Do not** target JDK 24+ without this or Track B.

## Verification & guardrail

1. After P1+P2: `mvn -q clean install -DskipTests -Dmaven.compiler.showDeprecation=true 2>&1 | grep -c
   "marked for removal"` → should drop from 81 to ~34 (only the P3/SecurityManager set remains).
2. Re-run the Phase 3 runtime smoke (Tomcat 10.1 boot + `/dysoweb/panel` + a demo JSP + `?op=img` chart) —
   these are behavior-preserving changes, so it should be unaffected.
3. **Optional guardrail:** once a category is at zero, add `-Xlint:removal` + `<failOnWarning>` scoped to it,
   or `--release 21`-level `-Werror` on removal only, to stop new uses creeping back in. Don't enable global
   `-Werror` while the P3 set is outstanding.

## Suggested sequencing
1. **P1** (boxed constructors) — own commit, mechanical, review diff. Clears 42.
2. **P2** (ThreadDeath + getFeatureDescriptors) — own commit. Clears 5.
3. **P3** — do as part of Track B (Jasper replacement); only hand-strip if JDK 24+ is needed before that.
