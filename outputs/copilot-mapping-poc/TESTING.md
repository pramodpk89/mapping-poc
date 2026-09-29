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
recovery and a Java runtime copied to a path containing spaces and Unicode. They are
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
