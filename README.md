# Copilot interface mapping

Use one folder: **[copilot-mapping-poc](outputs/copilot-mapping-poc)**.
[Download the ZIP](outputs/copilot-mapping-poc.zip), extract it and open that folder in VS Code.
It includes the `/map-interface` skill, Java 8 helper, launcher, instructions, tests and evidence.

1. Set the approved Java 1.8 folder in `java-home.properties`.
2. Give Copilot the XML or business contract. A preferred Shopify API URL is optional.
3. Run `/map-interface`. Copilot discovers suitable APIs and produces concrete attribute mappings with evidence.
4. Answer clarifications in chat and rerun. `Report.html` is read-only.

No documentation curation, internal JSON editing, extra modules or administrator access is
required from analysts. The agent recommends APIs from the source instead of applying a fixed default. Missing source
evidence blocks; missing purpose prose or a URL does not prevent useful proposals. Changed decisions reopen affected mappings; unrelated answers and the last
successful review are preserved. WSDL provenance and the real interface purpose remain unconfirmed.

[Start here](outputs/copilot-mapping-poc/START-HERE.txt) ·
[Testing](outputs/copilot-mapping-poc/TESTING.md) ·
[Session handoff](outputs/SESSION-HANDOFF.md)

Maintainer validation records are outside the working folder. Old packs have been removed.
Tests use synthetic decisions, never business confirmations. This workflow makes no ERP or
Shopify writes. Visual browser review and an actual analyst Copilot pilot remain unverified.
