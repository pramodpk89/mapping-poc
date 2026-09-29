# Java 8 validation

The active helpers are a Java 8 JAR with no external libraries. Java 8 is a revised user
requirement, replacing the earlier PowerShell migration request. Java is supplied by the
team via `java-home.properties`; the ZIP does not bundle or install a runtime.

## Run on Windows

Set `java.home` as described in START-HERE.txt, then run:

```bat
Test-Windows.cmd
```

Or `Test-Windows.cmd --output "C:\Mapping review\Test results"` for a different destination.
The result is a browser-friendly HTML report and a machine-readable results.json. A failed
check returns exit code 1. Setup errors return 2. The 43 original Python cases are ported as
cases 01-43; later cases cover encodings, CLI boundaries, duplicate data, secure XML parsing,
reopening, recovery, and Windows launchers. No Pester/JUnit or build tool is required.

Every test uses a temporary folder with spaces, Unicode, ampersand and exclamation mark.
Synthetic purposes, answers and decisions are clearly labelled. Reports validate helper
behavior, not Copilot reasoning, real WSDL provenance or business approval. Never import
their answers into the real pack. Tests do not make ERP or Shopify calls.

Windows-specific tests exercise the CMD launcher, missing/invalid java.home, locked-file
recovery and a Java runtime copied to an ASCII path containing spaces. A Unicode Java-home\nconfiguration is tested for an explicit, actionable rejection. They are
reported as outstanding, never passing, when run on another operating system.

The GitHub workflow `Java 8 Windows acceptance` runs the shipped JAR on a Windows runner
and compiles the sources with a Java 8 compiler. This is separate from an analyst pilot:
organization-specific application controls, Copilot skill discovery, network shares and
Print/PDF still require testing in the team's environment. Do not change execution policies
or disable security controls to make this runner work. Use a local filesystem that supports
atomic replacement; unsupported locations fail with the prior file preserved.

## Maintainer build

Source: `.framework/java/Json.java`, `Mapping.java`, `WorkflowTests.java`.
With an approved **JDK 8**, from the pack root in Command Prompt:

```bat
mkdir .framework\build
"C:\your-jdk8\bin\javac.exe" -encoding UTF-8 -d .framework\build .framework\java\Json.java .framework\java\Mapping.java .framework\java\WorkflowTests.java
"C:\your-jdk8\bin\jar.exe" cfe .framework\mapping.jar Mapping -C .framework\build .
```

Do not include the build directory in the ZIP. Re-run acceptance tests against the rebuilt
JAR. Framework code uses Java 8 standard library APIs, safe XML without external resolution,
explicit UTF-8/BOM-aware UTF-16 reading, strict JSON validation, atomic writes and file locks.
The legacy Python source is archived outside the pack under work/legacy-python solely as
migration reference. It is not used for setup, runs, tests or packaging the active framework.

## Windows path handling

Use map.cmd on Windows, including when paths contain Unicode. It passes filenames through
Unicode environment values and launches the JAR with a relative name; passing those same
paths directly to Java 8's native `-jar` command can lose characters outside the system
code page. The Java installation folder itself must use **ASCII characters** (spaces are supported).
Real Windows tests found native Java 8 DLL/bootstrap failures from a Unicode installation
folder, even using a short-path alias. The launcher checks native startup and rejects a failing configuration with
a clear message; this is an explicit supported-path restriction, not a passing Unicode
runtime test. Use a support-approved ASCII Java path. Unicode pack/evidence/answer paths
are supported and tested separately. No registry or system short-name changes are made.

Git attributes preserve source/documentation bytes across checkouts, because line-ending
conversion must not silently change the evidence SHA-256 hashes. Pack ZIPs preserve those
bytes too. The portable suite is also compiled and run on a real local Java 8 runtime.

## Recorded results — 29 September 2026

- Windows: **75 passed, 0 failed**, Java 1.8.0_504 (Temurin) on Windows Server 2025.
  [Successful CI run](https://github.com/pramodpk89/mapping-poc/actions/runs/36536958866).
  The shipped JAR was executed and all Java sources compiled with a Java 8 compiler.
- macOS: **69 passed, 0 failed**, Java 1.8.0_382 (Amazon Corretto). Six Windows-only
  checks were marked outstanding on that host and subsequently executed in Windows CI.
- The original 43 scenarios are included in these counts, rather than additional runs.
- Three generated HTML scenarios passed static inspection of structure, embedded data,
  read-only controls, answer preservation and reopened status. Visual browser inspection
  is outstanding because the browser tool rejected local-file URLs. No workaround was used.
- ZIP contents were verified byte-for-byte. The real purpose and WSDL provenance remain
  unconfirmed, and original evidence, documentation and real analyst files are unchanged.

Repository reports: outputs/java8-test-results/ and outputs/windows-java8-test-results/.
The latter contains ci-run.json linking the executed implementation commit. Later commits
that only add validation records/documentation do not change that tested JAR.
