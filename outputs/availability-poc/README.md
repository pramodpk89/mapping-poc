# Availability mapping POC

1. Open **report.html** in Edge, Chrome or another modern browser. No installation is needed to view or answer.
2. Describe the interface and answer what you know. Click **Download answers (.json)**. Edits are not saved automatically. **Save report with answers** creates a new HTML file containing your answers.
3. Give your AI assistant the extracted POC folder and downloaded JSON. Use the prompt below.

> Read skills/map-interface/SKILL.md and use it to re-analyze this interface with my downloaded input JSON. Preserve unresolved questions, update analysis.json, and regenerate report.html. Do not execute any Shopify updates.

The AI assistant performs the analysis. The report does not call an AI service. The Python script validates and renders the AI output; it does not invent mappings or interpret answers.

## Files

- `input.json`: purpose, source references, known rules, answers and additional context.
- `analysis.json`: all seven mappings, questions, target requirements and evidence.
- `report.html`: self-contained HTML review with offline answer entry and export.
- `source/`: the two supplied WSDLs, transcribed XML excerpt, normalized copy and extracted fields.
- `schemas/`: structured input and output contracts.
- `skills/map-interface/SKILL.md`: portable AI workflow; can also be attached/read as instructions if the chosen assistant has no skill discovery.
- `templates/input.json`: starting input for the next interface. Populate its fields and generate a new analysis rather than reusing availability results.

## Regenerate on Windows

Viewing needs only a browser. Regenerating from JSON needs Python 3.9 or newer, with no additional packages.

Double-click `Regenerate report.cmd`, or run:

```text
py -3 render.py
py -3 render.py --input web-item-availability-input.json
```

On macOS/Linux use `python3 render.py`. The script works from any working directory; relative custom arguments resolve from your current directory. If input changes without a new AI analysis, the report marks the old mappings as needing re-analysis.

## Scope and evidence

This is a mapping POC, not an integration runtime. No model/API credentials are embedded and no store is connected. The initial analysis used official Shopify public documentation retrieved on 2026-09-29 for API version 2026-07. Documentation URLs, retrieval metadata and content hashes are in analysis.json. Full downloaded documentation is not redistributed.

The original chat XML was transcribed, preserving its values and incomplete structure. The normalized copy adds only the missing result and root closing tags; it does not convert the payload into the WSDL structure. No external WSDL import endpoints were called.

The first analysis has **0 confirmed mappings**. This is intentional: business purpose, quantity ownership, location scope and identity rules are not confirmed. The candidate inventory write is provisional. Current input documentation uses `changeFromQuantity`; older prose/examples on the mutation page also mention legacy fields. Live schema validation remains outstanding.

The Python renderer, answer entry, status filtering, changed-input marking and JSON import were tested on the available macOS environment. Export handlers ran without page errors; this browser did not expose the downloaded files for round-trip verification. The Windows command file is provided but has not been run on Windows. Claude/Copilot compatibility depends on the specific product and its access to local files; no tool-specific installation is assumed.
