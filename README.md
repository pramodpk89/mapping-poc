# Integration mapping POC

The active framework runs on **Java 1.8** with no external libraries. Set the approved Java
folder in [java-home.properties](outputs/copilot-mapping-poc/java-home.properties); a JRE is
sufficient. No Python, Node.js, PowerShell helpers, compiler or admin access is needed by
analysts. Company application controls must permit the supplied CMD launcher and JAR.

1. Download the [Copilot pack ZIP](outputs/copilot-mapping-poc.zip) and extract it.
2. Set `java.home` and open `copilot-mapping-poc` as the folder in VS Code.
3. Follow [START-HERE.txt](outputs/copilot-mapping-poc/START-HERE.txt), then send
   `/map-interface` in Copilot Agent mode. Give clarifications in chat and rerun.

Reports are read-only HTML. Mandatory input/evidence checks block incomplete runs. A changed
decision reopens affected mappings and preserves unrelated answers. Last-review.html retains
the last successful review through failed reruns. The real business purpose and the supplied
WSDLs' role/provenance remain **unconfirmed**. All test/demo decisions are synthetic.

- [Workflow](outputs/copilot-mapping-poc/RUN.md) and [testing/build guide](outputs/copilot-mapping-poc/TESTING.md).
- [Windows Java 8 test results](outputs/windows-java8-test-results/Test-results.html): 75 passed;
  [successful Windows CI](https://github.com/pramodpk89/mapping-poc/actions/runs/36536958866).
- [Java 8 local test results](outputs/java8-test-results/Test-results.html), 69 passed on macOS, with generated review scenarios.
- [Shopify reference](outputs/copilot-mapping-poc/Shopify/README.md): eight saved official pages.
- [Updated session handoff](outputs/SESSION-HANDOFF.md).

Open downloaded HTML in a browser; GitHub's file view displays source. Earlier
`availability-poc`, `cowork-mapping-poc` and `mapping-test-results` are historical prototypes
and evidence. Legacy Python helpers are archived in `work/legacy-python`, outside the active
pack. The Java migration supersedes the earlier PowerShell plan at the user's request.

The Java installation path must use ASCII characters; spaces are supported. Unicode pack,
evidence and answer paths passed Windows tests. A real analyst Copilot pilot, visual browser
review and live Shopify validation remain outstanding. No ERP or
Shopify writes are performed by this framework.
