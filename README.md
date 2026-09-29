# Copilot interface mapping

Use one folder: **[copilot-mapping-poc](outputs/copilot-mapping-poc)**.
[Download the ZIP](outputs/copilot-mapping-poc.zip), extract it and open that folder in VS Code.
It includes the `/map-interface` skill, Java 8 helper, launcher, instructions, tests and evidence.

1. Set the approved Java 1.8 folder in `java-home.properties`.
2. Give Copilot the XML payload, interface purpose and intended Shopify API endpoint/reference.
3. Run `/map-interface`. Copilot inspects the target API and proposes XML-to-Shopify mappings.
4. Answer clarifications in chat and rerun. `Report.html` is read-only.

No documentation curation, internal JSON editing, extra modules or administrator access is
required from analysts. No Shopify operation is selected by default. Incomplete inputs block
mapping analysis. Changed decisions reopen affected mappings; unrelated answers and the last
successful review are preserved. WSDL provenance and the real interface purpose remain unconfirmed.

[Start here](outputs/copilot-mapping-poc/START-HERE.txt) ·
[Testing](outputs/copilot-mapping-poc/TESTING.md) ·
[Session handoff](outputs/SESSION-HANDOFF.md)

Maintainer validation records are outside the working folder. Old packs have been removed.
Tests use synthetic decisions, never business confirmations. This workflow makes no ERP or
Shopify writes. Visual browser review and an actual analyst Copilot pilot remain unverified.
