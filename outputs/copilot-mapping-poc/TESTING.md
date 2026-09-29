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
recovery and a Java runtime copied to an ASCII path containing spaces. A Unicode Java-home configuration is tested for an explicit, actionable rejection. They are
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
The active pack contains only the Java helper; obsolete Python helpers and old packs were removed.

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

## Current validation

Actual Java 8 execution:

- macOS / Corretto 1.8.0_382: **83 passed, 0 failed**. Six Windows-only checks were marked
  outstanding on this host and then executed in Windows CI.
- Windows Server 2025 / Temurin 1.8.0_504: **89 passed, 0 failed, 0 outstanding**.
  [Successful CI run](https://github.com/pramodpk89/mapping-poc/actions/runs/36542653010)
  executed the shipped JAR/launcher and compiled the sources with JDK 8 at implementation
  commit `0c41ee8b7f54c224e8731bc4b7fe2998ad6367ab`.

Subsequent validation-record/documentation commits do not change the tested JAR or sources.

Cases 76-89 cover XML extraction without WSDL/research bundles, verified normalization,
orphaned copies, namespaces and duplicate names, SOAP/WSDL metadata, endpoint-only target
setup, operation/version clarification, target changes, official citations, saved chat input,
internal-only target tampering and REST method/reference handling. These use synthetic fixtures;
no selected operation, decision or research finding in a test establishes real business facts.

Four generated scenarios are checked statically: basic, clarified, reopened and alternate
target. The checks inspect HTML/embedded JSON, XML paths, saved answers and statuses. Browser
visual inspection is **unverified**: the browser tool could not verify the admin-enforced
policy and denied access to the local report preview. No bypass was attempted. Browser
JavaScript, Print/PDF and an actual analyst Copilot pilot were not exercised.

The real pack blocks on missing purpose and Shopify API reference. The supplied XML is usable
through its verified closing-tag-only copy. WSDL provenance remains unconfirmed. Original
XML/WSDL bytes and saved Shopify documentation are preserved. The JAR makes no live store calls.

Repository execution records: outputs/java8-test-results/ and outputs/windows-java8-test-results/.
