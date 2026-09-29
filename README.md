# Integration mapping POC

AI-assisted mapping from ERP/middleware source evidence to Shopify, starting with item availability. Functional analysts supply payloads, WSDLs, their understanding, and attribute decisions. The workflow produces a read-only HTML mapping report with evidence and unresolved questions.

## Start here

1. Download or clone this repository.
2. Open **`outputs/copilot-mapping-poc`** as the folder in VS Code so Copilot can discover its skill.
3. Follow [START-HERE.txt](outputs/copilot-mapping-poc/START-HERE.txt). The setup requires Copilot Agent capabilities and Python 3.9 or newer.
4. Run `/map-interface` in Copilot chat. Give clarifications in chat and rerun to generate a new report.

The HTML is read-only; typing into a report does not trigger an AI run. The current example requires a business-purpose statement before its mapping run can proceed. Unknown attribute rules remain open questions.

## Contents

- [Current Copilot pack](outputs/copilot-mapping-poc/) and [ZIP download](outputs/copilot-mapping-poc.zip).
- [Test results](outputs/mapping-test-results/Test-results.html), including three synthetic scenarios: initial mapping, clarifications, and a corrected decision.
- [Workflow regression tests](outputs/copilot-mapping-poc/.framework/tests/test_workflow.py).
- `outputs/availability-poc` and `outputs/cowork-mapping-poc`: earlier prototypes retained for reference. Use the Copilot pack for current work.
- `work/`: development scripts and captured Shopify research.

Download or open HTML files locally in a browser; GitHub's file view displays their source.

## Validation status

43 automated checks passed. Browser checks cover the read-only reports and mapping filters. Synthetic analysis fixtures validate workflow behavior, not model reasoning. A real Windows + Copilot pilot and live Shopify validation remain outstanding. No ERP or Shopify writes have been executed.

Run the local regression suite from the repository root:

```sh
python -m unittest discover -s outputs/copilot-mapping-poc/.framework/tests -p "test_*.py"
```
