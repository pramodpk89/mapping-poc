import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;

/** Dependency-free acceptance tests. Every purpose, answer and decision here is SYNTHETIC. */
final class WorkflowTests {
  interface Check {
    void run() throws Exception;
  }

  final Path template, output;
  Path temp;
  Mapping m;
  final List<Object> results = Mapping.list();
  int passed, failed, skipped;

  WorkflowTests(Path template, Path output) {
    this.template = template;
    this.output = output.toAbsolutePath();
  }

  static void require(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  static void eq(Object actual, Object expected) {
    require(Objects.equals(actual, expected), "Expected " + expected + " but got " + actual);
  }

  static Map<String, Object> o(Object x) {
    return Mapping.obj(x);
  }

  static List<Object> a(Object x) {
    return Mapping.arr(x);
  }

  static Map<String, Object> map(Object... pairs) {
    return Mapping.map(pairs);
  }

  static List<Object> list(Object... values) {
    return Mapping.list(values);
  }

  static void copyTree(Path from, Path to) throws IOException {
    Files.walkFileTree(
        from,
        new SimpleFileVisitor<Path>() {
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            String name = dir.getFileName().toString();
            if (!dir.equals(from)
                && Arrays.asList("history", "Test-results", "tests", "java", "build", "__pycache__")
                    .contains(name)) return FileVisitResult.SKIP_SUBTREE;
            Files.createDirectories(to.resolve(from.relativize(dir)));
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (!Arrays.asList("Last-review.html", "run-history.jsonl", "run.lock")
                .contains(file.getFileName().toString()))
              Files.copy(
                  file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  static void deleteTree(Path path) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
    Files.walkFileTree(
        path,
        new SimpleFileVisitor<Path>() {
          public FileVisitResult visitFile(Path p, BasicFileAttributes a) throws IOException {
            Files.delete(p);
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult postVisitDirectory(Path p, IOException e) throws IOException {
            Files.delete(p);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  void test(String name, Check check) throws Exception {
    temp = Files.createTempDirectory("mapping-tests-");
    Path root = temp.resolve("Analyst folder \u03a9 \u65e5\u672c & notes !");
    copyTree(template, root);
    m = new Mapping(root);
    try {
      // Tests start with draft dispositions; never copy confirmations into the real pack.
      Map<String, Object> fixture = m.load("analysis.json");
      for (Object row : a(fixture.get("mappings"))) {
        o(row).put("status", "needs_input");
        o(row).put("confirmed_by", null);
        o(row).put("decision_basis", "proposal");
      }
      Mapping.save(m.fw.resolve("analysis.json"), fixture);
      // Fixture target only: never copied into the real analyst pack.
      m.setUnderstanding(map("Shopify endpoint/reference",
          "https://shopify.dev/docs/api/admin-graphql/2026-07/mutations/inventorySetQuantities"));
      check.run();

      passed++;
      results.add(map("name", name, "status", "passed"));
      System.out.println("PASS " + name);
    } catch (Throwable e) {
      failed++;
      results.add(map("name", name, "status", "FAILED", "detail", e.toString()));
      System.err.println("FAIL " + name + ": " + e);
      e.printStackTrace();
    } finally {
      deleteTree(temp);
    }
  }

  void skip(String name, String why) {
    skipped++;
    results.add(map("name", name, "status", "outstanding", "detail", why));
    System.out.println("OUTSTANDING " + name + ": " + why);
  }

  void prepare() throws Exception {
    prepare(map());
  }

  void prepare(Map<String, Object> edits) throws Exception {
    Map<String, Object> value =
        map("purpose", "SYNTHETIC TEST: provide availability information to ecommerce.");
    value.putAll(edits);
    eq(m.prepare(value, null), 0);
  }

  void synthetic(BiConsumer<Map<String, Object>, Map<String, Object>> change) throws Exception {
    Map<String, Object> in = m.load("input.json"), an = m.load("analysis.json");
    an.put("revision", ((Number) an.get("revision")).longValue() + 1);
    an.put(
        "summary",
        "SYNTHETIC TEST: analysis fixture for workflow verification; no business confirmations.");
    for (Object v : a(in.get("known_rules"))) {
      Map<String, Object> rule = o(v);
      a(an.get("evidence")).removeIf(e -> Objects.equals(o(e).get("id"), rule.get("id")));
      a(an.get("evidence"))
          .add(
              map(
                  "id",
                  rule.get("id"),
                  "title",
                  "Synthetic test analyst decision",
                  "url",
                  "Decisions.csv",
                  "note",
                  rule.get("statement")));
    }
    Map<String, Object> target = o(in.get("target"));
    String operation = Mapping.str(target.get("selected_operation"));
    an.put("target_candidates", list(map("name", "SYNTHETIC target fixture", "operation", operation,
        "state", "selected", "when", "Synthetic workflow verification only", "evidence_ids", list("synthetic-target"))));
    a(an.get("evidence")).removeIf(e -> "synthetic-target".equals(o(e).get("id")));
    a(an.get("evidence")).add(map("id", "synthetic-target", "title", "Synthetic citation fixture",
        "url", "https://shopify.dev/docs/api/admin-graphql/" + target.get("api_version") + "/mutations/" + operation,
        "api_version", target.get("api_version"), "retrieved_on", "2026-09-29",
        "note", "SYNTHETIC citation fixture for validation only; no live research executed by this test."));
    if (change != null) change.accept(in, an);

    an.put("input_sha256", Mapping.fingerprint(in));
    Mapping.save(m.fw.resolve("input.json"), in);
    Mapping.save(m.fw.resolve("analysis.json"), an);
  }

  void synthetic() throws Exception {
    synthetic(null);
  }

  String generate(int expected) throws Exception {
    eq(m.generate(), expected);
    return Mapping.read(m.p("Report.html"));
  }

  Map<String, Object> row(Map<String, Object> an, String field) {
    return Mapping.find(a(an.get("mappings")), "source_field", field);
  }

  Map<String, Object> decision(String attr, String statement, String by) {
    return map(
        "attribute",
        attr,
        "decision",
        statement,
        "explanation",
        "Synthetic fixture only.",
        "confirmed_by",
        by);
  }

  void excluded() throws Exception {
    prepare();
    synthetic();
    generate(0);
    eq(
        m.prepare(
            map(
                "answers",
                map("Q08", "SYNTHETIC: N is normal stock; do not send this attribute."),
                "decisions",
                list(decision("SKUType", "Exclude SKUType from target payload.", "Test analyst"))),
            null),
        0);
    synthetic(
        (in, an) -> {
          Map<String, Object> r = row(an, "SKUType");
          r.putAll(
              map(
                  "status",
                  "excluded",
                  "decision_basis",
                  "confirmed",
                  "confirmed_by",
                  "Test analyst",
                  "target",
                  "Excluded by synthetic agreement",
                  "reason",
                  "The synthetic analyst confirmed internal classification only.",
                  "proposed_rule",
                  "Do not send SKUType; retain source evidence."));
          a(r.get("evidence_ids")).add("decision:SKUType");
          Mapping.find(a(an.get("questions")), "id", "Q08")
              .putAll(
                  map(
                      "review_status",
                      "resolved",
                      "resolution_note",
                      "Synthetic exclusion reviewed."));
        });
    generate(0);
  }

  void ready() throws Exception {
    prepare(
        map(
            "answers",
            map(
                "Q01",
                "Update sellable inventory.",
                "Q03",
                "ERP owns absolute sellable totals.",
                "Q04",
                "One fulfillment location."),
            "decisions",
            list(
                decision(
                    "AvailableQuantity",
                    "Map absolute sellable totals to available inventory.",
                    "Test analyst"))));
    synthetic(
        (in, an) -> {
          o(in.get("target")).put("selected_operation", "inventorySetQuantities");
          Map<String, Object> r = row(an, "AvailableQuantity");
          r.putAll(
              map(
                  "status",
                  "ready",
                  "decision_basis",
                  "confirmed",
                  "confirmed_by",
                  "Test analyst",
                  "target",
                  "inventorySetQuantities.input.quantities[].quantity",
                  "reason",
                  "Synthetic analyst confirmed absolute sellable stock for one location.",
                  "proposed_rule",
                  "Use integer; preserve zero. Look up item/location IDs separately."));
          a(r.get("evidence_ids")).add("decision:AvailableQuantity");
        });
    generate(0);
  }

  void writeRows(String delimiter, String encoding, String... values) throws Exception {
    String text =
        String.join(
                delimiter, Arrays.asList("Attribute", "Decision", "Explanation", "Confirmed by"))
            + "\r\n";
    for (int n = 0; n < values.length; n += 4) {
      List<String> cells = new ArrayList<>();
      for (int k = 0; k < 4; k++) cells.add("\"" + values[n + k].replace("\"", "\"\"") + "\"");
      text += String.join(delimiter, cells) + "\r\n";
    }
    Files.write(m.p("Decisions.csv"), text.getBytes(Charset.forName(encoding)));
  }

  void sourceMissing() throws Exception {
    deleteTree(m.p("Current/Source"));
    Files.createDirectories(m.p("Current/Source"));
  }

  Map<String, Object> sourceHashes() throws Exception {
    Map<String, Object> hashes = map();
    for (Path p : m.files(m.p("Current")))
      hashes.put(m.root.relativize(p).toString(), Mapping.hash(Files.readAllBytes(p)));
    for (Path p : m.files(m.p("Shopify")))
      hashes.put(m.root.relativize(p).toString(), Mapping.hash(Files.readAllBytes(p)));
    return hashes;
  }

  void exportScenario(String name) throws Exception {
    Path dir = output.resolve("scenarios").resolve(name);
    Files.createDirectories(dir);
    for (String file :
        Arrays.asList(
            "Report.html",
            "Last-review.html",
            "Understanding.txt",
            "Questions.txt",
            "Decisions.csv"))
      if (Files.exists(m.p(file)))
        Files.copy(m.p(file), dir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
    for (String folder : Arrays.asList("Current", "Shopify"))
      copyTree(m.p(folder), dir.resolve(folder));
    Mapping.write(
        dir.resolve("README.txt"),
        "SYNTHETIC TEST EVIDENCE ONLY. Do not import these answers or decisions into the real pack."
            + " This is a read-only report scenario, not an executable pack.\n");
  }

  void suite() throws Exception {
    test(
        "01 Missing purpose allows evidence-backed proposals",
        () -> {
          eq(m.prepare(map(), null), 0);
          require(a(m.load("preflight.json").get("warnings")).stream()
              .anyMatch(w -> w.toString().contains("purpose has not been confirmed")), "Purpose uncertainty retained");
        });
    test(
        "02 Basic seven-field report",
        () -> {
          prepare();
          synthetic();
          require(generate(0).contains("mapping-rows"), "Mapping report");
          eq(a(m.load("analysis.json").get("mappings")).size(), 7);
          require(Files.exists(m.p("Last-review.html")), "Saved review");
          exportScenario("basic");
        });
    test(
        "03 Multiline description",
        () -> {
          Mapping.write(
              m.p("Understanding.txt"),
              Mapping.read(m.p("Understanding.txt"))
                  .replace(
                      "[Please add one sentence describing the business purpose]",
                      "\nSYNTHETIC: Provides stock information."));
          eq(m.prepare(map(), null), 0);
        });
    test(
        "04 UTF-16 notes",
        () -> {
          prepare();
          Files.write(
              m.p("Understanding.txt"),
              Mapping.read(m.p("Understanding.txt")).getBytes(StandardCharsets.UTF_16));
          eq(m.prepare(map(), null), 0);
        });
    test("05 Unknown purpose is not invented", () -> eq(m.prepare(map("purpose", "Unknown."), null), 0));
    test(
        "06 Missing source blocks",
        () -> {
          prepare();
          deleteTree(m.p("Current/Source"));
          eq(m.prepare(map(), null), 2);
        });
    test(
        "07 Empty XML is not evidence",
        () -> {
          prepare();
          sourceMissing();
          Mapping.write(m.p("Current/Source/empty.xml"), "<empty/>");
          eq(m.prepare(map(), null), 2);
        });
    test(
        "08 Incomplete original with verified closing-tag copy",
        () -> {
          prepare();
          require(
              a(m.load("preflight.json").get("warnings")).stream()
                  .anyMatch(x -> x.toString().contains("availability-excerpt.xml")),
              "Malformed sample warning");
        });
    test(
        "09 Missing URL allows agent discovery",
        () -> {
          Files.deleteIfExists(m.fw.resolve("target-discovery.json"));
          m.setUnderstanding(map("Shopify endpoint/reference", ""));
          eq(m.prepare(map(), null), 0);
          eq(o(m.load("input.json").get("target")).get("selected_operation"), null);
          synthetic(); generate(2);
        });
    test(
        "10 Unsafe endpoint reference blocks",
        () -> {
          prepare();
          eq(m.prepare(map("shopify_reference", "https://secret@shopify.dev/docs/api"), null), 2);
        });
    test(
        "11 Clarification preserves other answers and invalidates analysis",
        () -> {
          prepare(map("answers", map("Q01", "Display information.")));
          synthetic();
          generate(0);
          eq(m.prepare(map("answers", map("Q03", "Absolute stock totals.")), null), 0);
          eq(o(m.load("input.json").get("answers")).get("Q01"), "Display information.");
          generate(3);
        });
    test(
        "12 Clearing answer is not ignored",
        () -> {
          prepare(map("answers", map("Q03", "An absolute total.")));
          eq(m.prepare(map("answers", map("Q03", "")), null), 0);
          eq(o(m.load("input.json").get("answers")).get("Q03"), "");
        });
    test(
        "13 Rerun after clarification",
        () -> {
          excluded();
          eq(row(m.load("analysis.json"), "SKUType").get("status"), "excluded");
          exportScenario("clarified");
        });
    test(
        "14 Changed decision reopens old confirmation",
        () -> {
          excluded();
          Map<String, Object> old = o(Mapping.copy(row(m.load("analysis.json"), "SKUType")));
          eq(
              m.prepare(
                  map(
                      "decisions",
                      list(decision("SKUType", "Keep SKUType; rules are unclear.", ""))),
                  null),
              0);
          eq(row(m.load("analysis.json"), "SKUType").get("status"), "needs_input");
          synthetic((i, an) -> row(an, "SKUType").putAll(old));
          generate(2);
        });
    test(
        "15 Conflicting decisions allow discovery not confirmation",
        () -> {
          prepare();
          writeRows(
              ",",
              "UTF-8",
              "SKUType",
              "Exclude",
              "",
              "Analyst A",
              "SKUType",
              "Keep",
              "",
              "Analyst B");
          eq(m.prepare(map(), null), 0);
          eq(m.load("input.json").get("known_rules"), list());
          synthetic();
          require(generate(0).contains("Conflicting"), "Conflict shown");
        });
    test(
        "16 Excel semicolon and quoted comma",
        () -> {
          prepare();
          writeRows(
              ";",
              "UTF-8",
              "SKUType",
              "Exclude, for now",
              "A quoted, detailed note",
              "Test analyst");
          eq(m.prepare(map(), null), 0);
          eq(
              o(a(m.load("input.json").get("known_rules")).get(0)).get("statement"),
              "Exclude, for now");
        });
    test(
        "17 Bad CSV columns blocks",
        () -> {
          prepare();
          Mapping.write(m.p("Decisions.csv"), "Bad,Columns\nfoo,bar\n");
          eq(m.prepare(map(), null), 2);
        });
    test(
        "18 No invented reviewer",
        () -> {
          prepare(map("answers", map("Q08", "N means normal.")));
          synthetic(
              (i, an) ->
                  row(an, "SKUType")
                      .putAll(
                          map(
                              "status",
                              "excluded",
                              "decision_basis",
                              "confirmed",
                              "confirmed_by",
                              "Imaginary reviewer")));
          generate(2);
        });
    test(
        "19 Unknown is not an answer",
        () -> {
          prepare(
              map(
                  "answers",
                  map("Q08", "unknown"),
                  "decisions",
                  list(decision("SKUType", "Exclude", "Test analyst"))));
          synthetic(
              (i, an) -> {
                Map<String, Object> r = row(an, "SKUType");
                r.putAll(
                    map(
                        "status",
                        "excluded",
                        "decision_basis",
                        "confirmed",
                        "confirmed_by",
                        "Test analyst"));
                a(r.get("evidence_ids")).add("decision:SKUType");
              });
          generate(2);
        });
    test(
        "20 Missing field rejected",
        () -> {
          prepare();
          synthetic((i, an) -> a(an.get("mappings")).remove(0));
          generate(2);
        });
    test(
        "21 Duplicate field rejected",
        () -> {
          prepare();
          synthetic(
              (i, an) -> a(an.get("mappings")).add(Mapping.copy(a(an.get("mappings")).get(0))));
          generate(2);
        });
    test(
        "22 Unknown evidence rejected",
        () -> {
          prepare();
          synthetic((i, an) -> a(row(an, "SKUType").get("evidence_ids")).add("invented"));
          generate(2);
        });
    test(
        "23 Blank source fields rejected",
        () -> {
          prepare();
          synthetic(
              (i, an) -> {
                o(i.get("source")).put("fields", list());
                an.put("mappings", list());
              });
          generate(2);
        });
    test(
        "24 Source type mismatch rejected",
        () -> {
          prepare();
          synthetic((i, an) -> row(an, "AvailableQuantity").put("source_type", "xs:string"));
          generate(2);
        });
    test(
        "25 Changed source invalidates report",
        () -> {
          prepare();
          synthetic();
          generate(0);
          Files.write(
              m.p("Current/Source/checkout-1.wsdl"),
              "\n".getBytes(StandardCharsets.UTF_8),
              StandardOpenOption.APPEND);
          generate(3);
        });
    test(
        "26 Last review survives failed input",
        () -> {
          prepare();
          synthetic();
          generate(0);
          byte[] saved = Files.readAllBytes(m.p("Last-review.html"));
          sourceMissing();
          eq(m.prepare(map(), null), 2);
          generate(2);
          require(
              Arrays.equals(saved, Files.readAllBytes(m.p("Last-review.html"))),
              "Last review byte preservation");
          require(Mapping.read(m.p("Report.html")).contains("Last-review.html"), "Historical link");
        });
    test(
        "27 Legacy answer import and rerun",
        () -> {
          prepare();
          synthetic();
          generate(0);
          Map<String, Object> value = m.load("input.json");
          o(value.get("answers"))
              .put("Q06", "The future date means unavailable; do not promise it.");
          Path file = m.p("saved-answers.json");
          Mapping.save(file, value);
          eq(m.prepare(map(), file), 0);
          require(
              o(m.load("input.json").get("answers")).get("Q06").toString().contains("future date"),
              "Imported");
          generate(3);
          synthetic();
          generate(0);
        });
    test(
        "28 Stale import preserves newer notes",
        () -> {
          prepare();
          Path f = m.p("old-answers.json");
          Mapping.save(f, m.load("input.json"));
          eq(m.prepare(map("answers", map("Q01", "A newer clarification.")), null), 0);
          byte[] before = Files.readAllBytes(m.p("Questions.txt"));
          eq(m.prepare(map(), f), 2);
          require(
              Arrays.equals(before, Files.readAllBytes(m.p("Questions.txt"))),
              "No overwritten answers");
        });
    test(
        "29 Foreign interface import rejected",
        () -> {
          prepare();
          Map<String, Object> v = m.load("input.json");
          o(v.get("interface")).put("id", "other");
          Path f = m.p("wrong.json");
          Mapping.save(f, v);
          eq(m.prepare(map(), f), 2);
        });
    test(
        "30 No internal-only clarification",
        () -> {
          prepare();
          synthetic((i, an) -> o(i.get("answers")).put("Q01", "Invented business answer."));
          generate(2);
        });
    test(
        "31 Resolved question needs review note",
        () -> {
          prepare(map("answers", map("Q08", "Normal stock.")));
          synthetic(
              (i, an) ->
                  Mapping.find(a(an.get("questions")), "id", "Q08")
                      .putAll(map("review_status", "resolved", "resolution_note", "")));
          generate(2);
        });
    test(
        "32 HTML injection remains inert",
        () -> {
          prepare(map("context", "</script><img src=x onerror=alert(1)>"));
          synthetic();
          String html = generate(0);
          require(
              !html.contains("</script><img src=x") && html.contains("\\u003c/script"),
              "Escaped embedded JSON");
        });
    test(
        "33 Different working directory and spaces",
        () -> {
          prepare();
          synthetic();
          eq(invokeJar("generate"), 0);
        });
    test(
        "34 Boolean settings must be boolean",
        () -> {
          Map<String, Object> cfg = m.load("Required-inputs.json");
          cfg.put("require_description", "false");
          Mapping.save(m.fw.resolve("Required-inputs.json"), cfg);
          eq(m.prepare(map(), null), 2);
        });
    test(
        "35 Duplicate question IDs rejected",
        () -> {
          prepare();
          Mapping.write(
              m.p("Questions.txt"),
              Mapping.read(m.p("Questions.txt")) + "\nQ01: duplicate\nAnswer: conflicting\n");
          eq(m.prepare(map(), null), 2);
        });
    test(
        "36 Original evidence and Shopify docs preserved",
        () -> {
          Map<String, Object> before = sourceHashes();
          prepare(map("answers", map("Q02", "Unknown origin.")));
          synthetic();
          generate(0);
          eq(sourceHashes(), before);
        });
    test(
        "37 Ready field with synthetic business confirmation",
        () -> {
          ready();
          require(
              a(m.load("analysis.json").get("interface_gates")).stream()
                  .anyMatch(v -> "open".equals(o(v).get("status"))),
              "Interface gates remain open");
        });
    test(
        "38 Ready requires target choice",
        () -> {
          ready();
          synthetic((i, an) -> o(i.get("target")).put("selected_operation", null));
          generate(2);
        });
    test(
        "39 New XML field requires new mapping",
        () -> {
          prepare();
          for (String name : Arrays.asList("availability-excerpt.xml", "availability-normalized.xml")) {
            Path f = m.p("Current/Source/" + name);
            Mapping.write(f, Mapping.read(f).replace("</WebAvailabilityData>",
                "<NewField>new</NewField></WebAvailabilityData>"));
          }
          eq(m.prepare(map(), null), 0);
          synthetic();
          generate(2);
        });
    test(
        "40 Unknown question does not erase answers",
        () -> {
          prepare(map("answers", map("Q01", "Known answer.")));
          String before = Mapping.read(m.p("Questions.txt"));
          eq(m.prepare(map("answers", map("Q999", "Mistyped ID.")), null), 2);
          eq(Mapping.read(m.p("Questions.txt")), before);
        });
    test(
        "41 Unicode multiline and shell characters preserved",
        () -> {
          String answer =
              "Analyst\u2019s note: \"available\" means stock.\n"
                  + "Keep \u20b9, commas, $(text), %PATH%, !literal! and `literal` unchanged.";
          prepare(map("answers", map("Q03", answer)));
          eq(o(m.load("input.json").get("answers")).get("Q03"), answer);
        });
    test(
        "42 Malformed configuration actionable error",
        () -> {
          prepare();
          Mapping.write(m.fw.resolve("Required-inputs.json"), "{broken");
          eq(m.prepare(map(), null), 2);
          require(
              Mapping.read(m.p("Report.html")).contains("Input needs attention"),
              "Actionable notice");
        });
    test(
        "43 Status change cannot keep old explanation",
        () -> {
          excluded();
          synthetic(
              (i, an) ->
                  row(an, "SKUType")
                      .putAll(
                          map(
                              "status",
                              "needs_input",
                              "decision_basis",
                              "proposal",
                              "confirmed_by",
                              null)));
          generate(2);
        });
    test(
        "44 UTF-8 BOM and UTF-16LE/BE roundtrip",
        () -> {
          prepare();
          for (Charset cs :
              Arrays.asList(
                  StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE)) {
            String text = Mapping.read(m.p("Understanding.txt"));
            byte[] body = text.getBytes(cs),
                bom =
                    cs == StandardCharsets.UTF_8
                        ? new byte[] {(byte) 239, (byte) 187, (byte) 191}
                        : cs == StandardCharsets.UTF_16LE
                            ? new byte[] {(byte) 255, (byte) 254}
                            : new byte[] {(byte) 254, (byte) 255};
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            b.write(bom);
            b.write(body);
            Files.write(m.p("Understanding.txt"), b.toByteArray());
            eq(m.prepare(map(), null), 0);
          }
        });
    test(
        "45 Invalid text encoding rejected",
        () -> {
          Files.write(m.p("Understanding.txt"), new byte[] {(byte) 0xc3, 0x28});
          eq(m.prepare(map(), null), 2);
          require(Mapping.read(m.p("Report.html")).contains("UTF-8"), "Encoding guidance");
        });
    test(
        "46 Excel tabs multiline quotes and UTF-16",
        () -> {
          prepare();
          writeRows(
              "\t",
              "UTF-16",
              "SKUType",
              "Exclude",
              "Quoted \"note\"\r\nsecond line",
              "Synthetic reviewer");
          eq(m.prepare(map(), null), 0);
          eq(a(m.load("input.json").get("known_rules")).size(), 1);
        });
    test(
        "47 Extra CSV cells rejected",
        () -> {
          prepare();
          Mapping.write(
              m.p("Decisions.csv"),
              "Attribute,Decision,Explanation,Confirmed by\r\n"
                  + "SKUType,Exclude,note,reviewer,extra\r\n");
          eq(m.prepare(map(), null), 2);
        });
    test(
        "48 Duplicate JSON properties rejected",
        () -> {
          boolean rejected = false;
          try {
            Json.parse("{\"purpose\":\"one\",\"purpose\":\"two\"}");
          } catch (IllegalArgumentException e) {
            rejected = true;
          }
          require(rejected, "Duplicate keys rejected");
        });
    test(
        "49 DTD external entity cannot supply evidence",
        () -> {
          prepare();
          sourceMissing();
          Mapping.write(
              m.p("Current/Source/entity.xml"),
              "<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///does-not-exist'>]><x>&secret;</x>");
          eq(m.prepare(map(), null), 2);
          require(a(m.load("preflight.json").get("usable_source_files")).isEmpty(), "DTD rejected");
        });
    test(
        "50 Import-only WSDL cannot supply contract",
        () -> {
          prepare();
          sourceMissing();
          Mapping.write(
              m.p("Current/Source/import.wsdl"),
              "<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\"><import"
                  + " location=\"https://invalid.example/schema\"/></definitions>");
          eq(m.prepare(map(), null), 2);
        });
    test(
        "51 Unsupported payload cannot reuse stale fields",
        () -> {
          prepare();
          sourceMissing();
          Mapping.write(m.p("Current/Source/payload.json"), "{\"SKU\":\"a\"}");
          eq(m.prepare(map(), null), 2);
          require(
              Mapping.read(m.p("Report.html")).contains("readable XML"),
              "Explicit scope limitation");
        });
    test(
        "52 Conflicting WSDL schemas do not determine payload fields",
        () -> {
          prepare();
          String text = Mapping.read(m.p("Current/Source/checkout-1.wsdl"));
          Mapping.write(
              m.p("Current/Source/alternate.wsdl"),
              text.replace(
                  "name=\"AvailableQuantity\" type=\"xs:int\"",
                  "name=\"AvailableQuantity\" type=\"xs:string\""));
          eq(m.prepare(map(), null), 0);
          eq(a(o(m.load("input.json").get("source")).get("fields")).size(), 7);
        });
    test(
        "53 Same-reviewer revised decision cannot retain old confirmation",
        () -> {
          excluded();
          Map<String, Object> old = o(Mapping.copy(row(m.load("analysis.json"), "SKUType")));
          eq(
              m.prepare(
                  map(
                      "decisions",
                      list(
                          decision(
                              "SKUType",
                              "Keep SKUType in target classification.",
                              "Test analyst"))),
                  null),
              0);
          synthetic((i, an) -> row(an, "SKUType").putAll(old));
          generate(2);
        });
    test(
        "54 Reopening preserves unrelated answers and recovers",
        () -> {
          excluded();
          eq(m.prepare(map("answers", map("Q06", "Synthetic unrelated date answer.")), null), 0);
          eq(
              m.prepare(
                  map(
                      "decisions",
                      list(decision("SKUType", "Keep SKUType; classification is unclear.", ""))),
                  null),
              0);
          eq(o(m.load("input.json").get("answers")).get("Q06"), "Synthetic unrelated date answer.");
          eq(
              o(m.load("input.json").get("answers")).get("Q08"),
              "SYNTHETIC: N is normal stock; do not send this attribute.");
          synthetic();
          generate(0);
          eq(row(m.load("analysis.json"), "SKUType").get("status"), "needs_input");
          exportScenario("reopened");
        });
    test(
        "55 Failed analysis preserves last review then recovers",
        () -> {
          prepare();
          synthetic();
          generate(0);
          byte[] saved = Files.readAllBytes(m.p("Last-review.html"));
          Map<String, Object> an = m.load("analysis.json");
          Mapping.write(m.fw.resolve("analysis.json"), "{broken");
          generate(2);
          require(
              Arrays.equals(saved, Files.readAllBytes(m.p("Last-review.html"))),
              "Last successful review retained");
          Mapping.save(m.fw.resolve("analysis.json"), an);
          generate(0);
        });
    test(
        "56 Concurrent run leaves successful report untouched",
        () -> {
          prepare();
          synthetic();
          generate(0);
          String before = Mapping.read(m.p("Report.html"));
          try (Mapping.RunLock lock = m.new RunLock()) {
            eq(m.generate(), 2);
            eq(m.prepare(map("purpose", "concurrent"), null), 2);
          }
          eq(Mapping.read(m.p("Report.html")), before);
        });
    test(
        "57 Read-only report structure",
        () -> {
          prepare();
          synthetic();
          String html = generate(0);
          require(!PatternHolder.EDIT.matcher(html).find(), "No answer inputs or contenteditable");
          for (String token : Arrays.asList("answer-count", "updateAnswers", "input.answers", "recorded answers", "your answers", "save-answers", "id='answer-"))
            require(!html.contains(token), "Removed answer feature: " + token);
          require(html.contains("field.path") && html.contains("Source evidence"), "Payload trace rendered");

          require(
              html.contains("Read-only report")
                  && html.contains("id=\"print\"")
                  && html.contains("id=\"all-fields\""),
              "Read-only controls retained");
        });
    test(
        "58 Real pack purpose and provenance still unconfirmed",
        () -> {
          require(
              Mapping.unknown(m.understanding().get("what this interface does")),
              "No synthetic purpose in real pack");
          require(
              Mapping.unknown(m.understanding().get("payload origin")), "Unknown payload origin");
          eq(m.prepare(map(), null), 0);
          require(
              a(m.load("preflight.json").get("warnings")).contains(Mapping.PROVENANCE),
              "Provenance warning");
        });
    test(
        "59 CLI edits file preserves Unicode and literal shell text",
        () -> {
          Path f = m.p("clarifications \u65e5\u672c.json");
          String answer = "\u20b9 \u03a9 $(not-a-command) %PATH% !keep! & <script>";
          Mapping.save(
              f, map("purpose", "SYNTHETIC command boundary test.", "answers", map("Q03", answer)));
          eq(invokeJar("prepare", "--edits", f.toString()), 0);
          eq(o(m.load("input.json").get("answers")).get("Q03"), answer);
        });
    test(
        "60 Evidence traversal rejected",
        () -> {
          prepare();
          synthetic(
              (i, an) ->
                  a(an.get("evidence"))
                      .add(
                          map(
                              "id",
                              "escape",
                              "title",
                              "escape",
                              "url",
                              "Current/../../outside.xml",
                              "note",
                              "Synthetic hostile path",
                              "sha256",
                              "fake")));
          generate(2);
        });
    test(
        "61 Stale decision evidence statement rejected",
        () -> {
          excluded();
          synthetic(
              (i, an) ->
                  Mapping.find(a(an.get("evidence")), "id", "decision:SKUType")
                      .put("note", "Old statement"));
          generate(2);
        });
    test(
        "62 Duplicate decision edits do not partially save",
        () -> {
          prepare();
          String before = Mapping.read(m.p("Understanding.txt"));
          eq(
              m.prepare(
                  map(
                      "purpose",
                      "Should not be saved.",
                      "decisions",
                      list(decision("SKUType", "keep", "A"), decision("SKUType", "exclude", "B"))),
                  null),
              2);
          eq(Mapping.read(m.p("Understanding.txt")), before);
        });
    test(
        "63 Schema booleans are not integers",
        () -> {
          prepare();
          synthetic((i, an) -> an.put("revision", true));
          generate(2);
        });
    test(
        "64 Question heading injection rejected without answer loss",
        () -> {
          prepare(map("answers", map("Q01", "Preserved")));
          String before = Mapping.read(m.p("Questions.txt"));
          eq(m.prepare(map("answers", map("Q03", "text\nQ01: replaced\nAnswer: attack")), null), 2);
          eq(Mapping.read(m.p("Questions.txt")), before);
        });
    test(
        "65 Invalid CLI options fail",
        () -> eq(invokeJar("generate", "--edits", "ignored.json"), 2));
    test(
        "66 Clarification backups preserve prior bytes",
        () -> {
          prepare();
          byte[] before = Files.readAllBytes(m.p("Questions.txt"));
          eq(m.prepare(map("answers", map("Q03", "New answer")), null), 0);
          Path backups = m.fw.resolve("history/input-edits");
          boolean found = false;
          try (java.util.stream.Stream<Path> stream = Files.walk(backups)) {
            for (Path p :
                (Iterable<Path>)
                    stream.filter(x -> x.getFileName().toString().equals("Questions.txt"))
                        ::iterator) if (Arrays.equals(before, Files.readAllBytes(p))) found = true;
          }
          require(found, "Backup includes exact prior answers");
        });
    test(
        "67 Interrupted output save retains last successful review",
        () -> {
          prepare();
          synthetic();
          generate(0);
          byte[] saved = Files.readAllBytes(m.p("Last-review.html"));
          Files.delete(m.p("Report.html"));
          Files.createDirectory(m.p("Report.html"));
          Mapping.write(m.p("Report.html/obstruction.txt"), "Synthetic I/O failure");
          eq(m.generate(), 2);
          require(
              Arrays.equals(saved, Files.readAllBytes(m.p("Last-review.html"))),
              "Last review rolled back after output failure");
          deleteTree(m.p("Report.html"));
          generate(0);
        });
    test(
        "73 Malformed CLI clarification preserves history and blocks current report",
        () -> {
          prepare();
          synthetic();
          generate(0);
          byte[] last = Files.readAllBytes(m.p("Last-review.html"));
          Path edits = m.p("bad edits.json");
          Mapping.write(edits, "{broken");
          eq(invokeJar("prepare", "--edits", edits.toString()), 2);
          require(
              Mapping.read(m.p("Report.html")).contains("Input needs attention"),
              "Current blocker notice");
          require(
              Arrays.equals(last, Files.readAllBytes(m.p("Last-review.html"))),
              "Preserved last review");
        });
    test(
        "74 Preparing changed input immediately marks current report pending",
        () -> {
          prepare();
          synthetic();
          generate(0);
          byte[] last = Files.readAllBytes(m.p("Last-review.html"));
          eq(m.prepare(map("answers", map("Q03", "Changed synthetic meaning.")), null), 0);
          String current = Mapping.read(m.p("Report.html"));
          require(
              current.contains("Copilot needs to analyse") && !current.contains("mapping-rows"),
              "Stale review not presented as current");
          require(
              Arrays.equals(last, Files.readAllBytes(m.p("Last-review.html"))),
              "Preserved historical review");
        });
    test("76 XML fields survive removal of all WSDL and saved research", () -> {
      Files.delete(m.p("Current/Source/checkout-1.wsdl"));
      Files.delete(m.p("Current/Source/checkout-2.wsdl"));
      Files.delete(m.p("Shopify/Target-reference.json"));
      deleteTree(m.p("Shopify/Docs"));
      prepare();
      List<Object> fields = a(o(m.load("input.json").get("source")).get("fields"));
      eq(fields.size(), 7);
      eq(o(fields.get(0)).get("path"), "/WebItemAvailability/result/WebAvailabilityData/AvailableDate");
      eq(o(fields.get(0)).get("optional"), null);
    });
    test("77 Modified normalization cannot invent source values", () -> {
      prepare();
      Path f = m.p("Current/Source/availability-normalized.xml");
      Mapping.write(f, Mapping.read(f).replace("LV4000000", "INVENTED"));
      eq(m.prepare(map(), null), 2);
    });
    test("78 Normalized copy alone cannot substitute for original", () -> {
      prepare();
      Files.delete(m.p("Current/Source/availability-excerpt.xml"));
      eq(m.prepare(map(), null), 2);
    });
    test("79 Arbitrary XML names, namespaces, attributes and duplicate leaves", () -> {
      sourceMissing();
      Mapping.write(m.p("Current/Source/order.xml"),
          "<o:Order xmlns:o='urn:orders' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' id='a'>"
          + "<o:Billing><o:Code>A</o:Code></o:Billing><o:Shipping><o:Code xsi:nil='true'/></o:Shipping></o:Order>");
      prepare();
      List<Object> fields = a(o(m.load("input.json").get("source")).get("fields"));
      eq(fields.size(), 3);
      Set<String> names = Mapping.ids(fields, "name", "field");
      require(names.contains("@id") && names.contains("/{urn:orders}Order/{urn:orders}Billing/{urn:orders}Code"), "Distinct payload paths");
      require(!names.contains("@nil") && !names.contains("@o"), "No XML metadata attributes");
    });
    test("80 SOAP headers and WSDL disguised as XML are not mapped", () -> {
      sourceMissing();
      Mapping.write(m.p("Current/Source/service.xml"),
          "<definitions xmlns='http://schemas.xmlsoap.org/wsdl/' name='Service'><service name='Metadata'/></definitions>");
      Mapping.write(m.p("Current/Source/message.xml"),
          "<s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'><s:Header><Token>secret</Token></s:Header>"
          + "<s:Body><Order><Sku>a</Sku></Order></s:Body></s:Envelope>");
      prepare();
      List<Object> fields = a(o(m.load("input.json").get("source")).get("fields"));
      eq(fields.size(), 1); eq(o(fields.get(0)).get("name"), "Sku");
    });
    test("81 Generic GraphQL URL reaches agent research before clarification", () -> {
      eq(m.prepare(map("purpose", "SYNTHETIC test", "shopify_reference",
          "https://example.myshopify.com/admin/api/2026-07/graphql.json"), null), 0);
      eq(o(m.load("input.json").get("target")).get("selected_operation"), null);
      eq(m.prepare(map("shopify_operation", "productUpdate"), null), 0);
      eq(o(m.load("input.json").get("target")).get("selected_operation"), "productUpdate");
      synthetic(); generate(0);
      exportScenario("alternate-target");
    });
    test("82 Versionless API reference persists pending essential clarification", () -> {
      eq(m.prepare(map("purpose", "SYNTHETIC test", "shopify_reference",
          "https://shopify.dev/docs/api/admin-graphql/latest/mutations/productUpdate"), null), 0);
      require(Mapping.read(m.p("Understanding.txt")).contains("productUpdate"), "Saved reference");
      eq(m.prepare(map("shopify_version", "2026-07"), null), 0);
      eq(o(m.load("input.json").get("target")).get("selected_operation"), "productUpdate");
    });
    test("83 Target reference changes reopen confirmation but preserve answers", () -> {
      excluded();
      eq(m.prepare(map("shopify_reference", "https://shopify.dev/docs/api/admin-graphql/2026-07/mutations/productUpdate"), null), 0);
      eq(row(m.load("analysis.json"), "SKUType").get("status"), "needs_input");
      require(!Mapping.unknown(o(m.load("input.json").get("answers")).get("Q08")), "Saved clarification retained");
      generate(3);
    });
    test("84 Agent must cite selected operation and version before generating", () -> {
      prepare(); synthetic((i, an) -> Mapping.find(a(an.get("evidence")), "id", "synthetic-target")
          .put("url", "https://shopify.dev.evil.example/docs/api/admin-graphql/2026-07/mutations/inventorySetQuantities"));
      generate(2);
      synthetic((i, an) -> Mapping.find(a(an.get("evidence")), "id", "synthetic-target").put("api_version", "2025-01"));
      generate(2);
    });
    test("85 Target choice cannot exist only in internal JSON", () -> {
      prepare(); synthetic((i, an) -> o(i.get("target")).put("selected_operation", "productUpdate"));
      generate(2);
    });
    test("86 Raw chat answers are saved but absent from HTML", () -> {
      prepare(map("answers", map("Q06", "SYNTHETIC PRIVATE ANSWER SENTINEL")));
      synthetic();
      require(!generate(0).contains("SYNTHETIC PRIVATE ANSWER SENTINEL"), "No answer display or embedded answer copy");
      require(Mapping.read(m.p("Questions.txt")).contains("SYNTHETIC PRIVATE ANSWER SENTINEL"), "Chat persisted");
      eq(m.prepare(map(), null), 0);
      eq(o(m.load("input.json").get("answers")).get("Q06"), "SYNTHETIC PRIVATE ANSWER SENTINEL");
    });
    test("87 Changed reference clears old explicit operation and version", () -> {
      prepare(map("shopify_reference", "https://example.myshopify.com/admin/api/2026-07/graphql.json",
          "shopify_operation", "inventorySetQuantities", "shopify_version", "2026-07"));
      eq(m.prepare(map("shopify_reference", "https://shopify.dev/docs/api/admin-graphql/2026-10/mutations/productUpdate"), null), 0);
      eq(o(m.load("input.json").get("target")).get("selected_operation"), "productUpdate");
      eq(o(m.load("input.json").get("target")).get("api_version"), "2026-10");
    });
    test("88 REST reference requires explicit method and versioned official evidence", () -> {
      prepare(map("shopify_reference", "https://example.myshopify.com/admin/api/2026-07/products.json",
          "shopify_operation", "POST /products.json"));
      synthetic((i, an) -> Mapping.find(a(an.get("evidence")), "id", "synthetic-target").putAll(map(
          "url", "https://shopify.dev/docs/api/admin-rest/2026-07/resources/product",
          "operation", "POST /products.json")));
      generate(0);
    });
    test("89 WSDL metadata alone never passes payload check", () -> {
      prepare();
      Files.delete(m.p("Current/Source/availability-excerpt.xml"));
      Files.delete(m.p("Current/Source/availability-normalized.xml"));
      eq(m.prepare(map(), null), 2);
      require(a(m.load("preflight.json").get("usable_source_files")).isEmpty(), "No WSDL source fields");
    });
    test("90 Proposed attribute maps without analyst confirmation", () -> {
      prepare();
      synthetic((i, an) -> row(an, "AvailableQuantity").putAll(map(
          "status", "proposed", "target", "inventorySetQuantities.input.quantities[].quantity",
          "proposed_rule", "SYNTHETIC: preserve integer quantity; confirm stock basis before implementation.",
          "evidence_ids", list("source-sample", "synthetic-target"))));
      String html = generate(0);
      require(html.contains("Proposed mapping") && html.contains("input.quantities[].quantity"), "Actual destination is displayed");
      eq(a(m.load("input.json").get("known_rules")).size(), 0);
      exportScenario("proposed");
    });
    test("91 Proposed mapping without target evidence is rejected", () -> {
      prepare(); synthetic((i, an) -> row(an, "AvailableQuantity").putAll(map(
          "status", "proposed", "evidence_ids", list("source-sample"))));
      generate(2);
    });
    test("92 Agent discovery never becomes an analyst-supplied URL", () -> {
      m.setUnderstanding(map("Shopify endpoint/reference", ""));
      Mapping.save(m.fw.resolve("target-discovery.json"), map("requested_reference", "",
          "reference", "https://shopify.dev/docs/api/admin-graphql/2026-07/mutations/metafieldsSet",
          "operation", "metafieldsSet", "api_version", "2026-07", "reason", "SYNTHETIC discovery"));
      eq(m.prepare(map(), null), 0);
      eq(o(m.load("input.json").get("target")).get("selection_basis"), "agent_proposal");
      require(Mapping.unknown(m.understanding().get("shopify endpoint/reference")), "No invented analyst input");
    });
    test("93 Supplied API URL takes precedence over prior discovery", () -> {
      prepare(map("shopify_reference", "https://shopify.dev/docs/api/admin-graphql/2026-07/mutations/productUpdate"));
      eq(o(m.load("input.json").get("target")).get("selected_operation"), "productUpdate");
      eq(o(m.load("input.json").get("target")).get("selection_basis"), "analyst_reference");
    });
    test("94 Simple XSD business contract works without XML", () -> {
      sourceMissing();
      Mapping.write(m.p("Current/Source/order.xsd"), "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>"
          + "<xs:element name='Order'><xs:complexType><xs:sequence><xs:element name='SKU' type='xs:string'/>"
          + "<xs:element name='Quantity' type='xs:int'/></xs:sequence></xs:complexType></xs:element></xs:schema>");
      prepare();
      List<Object> fields = a(o(m.load("input.json").get("source")).get("fields"));
      eq(fields.size(), 2); eq(o(fields.get(0)).get("path"), "/Order/SKU");
    });
    test("95 WSDL requires relevant business element selection, never service metadata", () -> {
      sourceMissing();
      Mapping.write(m.p("Current/Source/order.wsdl"), "<w:definitions xmlns:w='http://schemas.xmlsoap.org/wsdl/' xmlns:xs='http://www.w3.org/2001/XMLSchema'>"
          + "<w:types><xs:schema targetNamespace='urn:order'><xs:element name='Order'><xs:complexType><xs:sequence>"
          + "<xs:element name='SKU' type='xs:string'/></xs:sequence></xs:complexType></xs:element></xs:schema></w:types>"
          + "<w:service name='UnconfirmedService'/></w:definitions>");
      Mapping.save(m.fw.resolve("contract-selection.json"), map("file", "Current/Source/order.wsdl", "namespace", "urn:order", "element", "Order"));
      prepare(); eq(a(o(m.load("input.json").get("source")).get("fields")).size(), 1);
    });
    test("96 Changed source reopens proposed mappings", () -> {
      prepare(); synthetic((i, an) -> row(an, "AvailableQuantity").putAll(map("status", "proposed",
          "evidence_ids", list("source-sample", "synthetic-target"))));
      generate(0);
      eq(m.prepare(map("answers", map("Q03", "SYNTHETIC: quantity means an adjustment delta.")), null), 0);
      eq(row(m.load("analysis.json"), "AvailableQuantity").get("status"), "needs_input");
    });
    if (System.getProperty("os.name").startsWith("Windows")) {
      test(
          "68 Windows CMD launcher with configured Java folder",
          () -> {
            Mapping.write(
                m.p("java-home.properties"),
                "java.home=" + System.getProperty("java.home") + "\r\n");
            prepare();
            synthetic();
            eq(process("cmd.exe", "/d", "/c", m.p("map.cmd").toString(), "generate"), 0);
          });
      test(
          "69 Windows missing Java configuration is actionable",
          () -> {
            Mapping.write(m.p("java-home.properties"), "java.home=\r\n");
            eq(process("cmd.exe", "/d", "/c", m.p("map.cmd").toString(), "prepare"), 2);
          });
      test(
          "70 Windows invalid Java folder fails",
          () -> {
            Mapping.write(m.p("java-home.properties"), "java.home=C:\\does-not-exist\\Java 8\r\n");
            eq(process("cmd.exe", "/d", "/c", m.p("map.cmd").toString(), "prepare"), 2);
          });
      test(
          "71 Windows locked report recovers",
          () -> {
            prepare();
            synthetic();
            generate(0);
            byte[] saved = Files.readAllBytes(m.p("Last-review.html"));
            try (java.nio.channels.FileChannel c =
                    java.nio.channels.FileChannel.open(
                        m.p("Report.html"), StandardOpenOption.WRITE);
                java.nio.channels.FileLock lock = c.lock()) {
              eq(m.generate(), 2);
            }
            require(
                Arrays.equals(saved, Files.readAllBytes(m.p("Last-review.html"))),
                "Locked-report recovery");
            generate(0);
          });
      test(
          "72 Windows Unicode Java-home restriction is actionable",
          () -> {
            Path home = temp.resolve("Java 8 \u03a9 & runtime !");
            copyRuntime(Paths.get(System.getProperty("java.home")), home);
            Mapping.write(m.p("java-home.properties"), "java.home=" + home + "\r\n");
            prepare();
            synthetic();
            eq(process("cmd.exe", "/d", "/c", m.p("map.cmd").toString(), "generate"), 2);
          });
      test(
          "75 Windows configured Java folder with spaces",
          () -> {
            Path home = temp.resolve("Java 8 runtime with spaces");
            copyRuntime(Paths.get(System.getProperty("java.home")), home);
            Mapping.write(m.p("java-home.properties"), "java.home=" + home + "\r\n");
            prepare();
            synthetic();
            eq(process("cmd.exe", "/d", "/c", m.p("map.cmd").toString(), "generate"), 0);
          });
    } else
      for (String name :
          Arrays.asList(
              "68 Windows CMD configured Java",
              "69 Windows missing Java configuration",
              "70 Windows invalid Java folder",
              "71 Windows locked report",
              "72 Windows Unicode Java-home restriction",
              "75 Windows configured Java folder with spaces"))
        skip(name, "Requires Windows; current host is " + System.getProperty("os.name"));
  }

  static void copyRuntime(Path source, Path target) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<Path>() {
          public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes a)
              throws IOException {
            Files.createDirectories(target.resolve(source.relativize(p)));
            return FileVisitResult.CONTINUE;
          }

          public FileVisitResult visitFile(Path p, BasicFileAttributes a) throws IOException {
            Files.copy(
                p, target.resolve(source.relativize(p)), StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  static final class PatternHolder {
    static final java.util.regex.Pattern EDIT =
        java.util.regex.Pattern.compile(
            "<(?:input|textarea|select)\\b|contenteditable\\s*=",
            java.util.regex.Pattern.CASE_INSENSITIVE);
  }

  static String javaExe() {
    return Paths.get(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
        .toString();
  }

  int invokeJar(String... args) throws Exception {
    List<String> command = new ArrayList<>();
    if (System.getProperty("os.name").startsWith("Windows")) {
      Mapping.write(
          m.p("java-home.properties"), "java.home=" + System.getProperty("java.home") + "\r\n");
      command.addAll(Arrays.asList("cmd.exe", "/d", "/c", m.p("map.cmd").toString()));
    } else command.addAll(Arrays.asList(javaExe(), "-jar", m.fw.resolve("mapping.jar").toString()));
    command.addAll(Arrays.asList(args));
    return process(command.toArray(new String[0]));
  }

  int process(String... command) throws Exception {

    ProcessBuilder builder = new ProcessBuilder(command);
    if (command[0].equals("cmd.exe")) {
      // An ASCII test wrapper supplies correctly quoted Unicode script and arguments
      // to CMD via environment variables, independent of the Java launcher's code page.
      Path wrapper = temp.resolve("invoke.cmd");
      StringBuilder script = new StringBuilder("@echo off\r\nsetlocal DisableDelayedExpansion\r\n");
      for (int n = 3; n < command.length; n++) {
        String key = "MAPPING_TEST_ARG_" + n;
        builder.environment().put(key, command[n]);
        if (n > 3) script.append(' ');
        script.append('"').append('%').append(key).append('%').append('"');
      }
      script.append("\r\nexit /b %ERRORLEVEL%\r\n");
      Mapping.write(wrapper, script.toString());
      builder.command("cmd.exe", "/d", "/c", wrapper.toString());
    }
    Path log = temp.resolve("child-process.log");
    Process p =
        builder
            .directory(temp.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    if (!p.waitFor(45, java.util.concurrent.TimeUnit.SECONDS)) {
      p.destroyForcibly();
      throw new AssertionError("Child command timed out after 45 seconds: " + command[0]);
    }
    int code = p.exitValue();
    if (code != 0) System.out.println(Mapping.read(log));
    return code;
  }

  void report() throws Exception {
    Map<String, Object> report =
        map(
            "java_version",
            System.getProperty("java.version"),
            "java_vendor",
            System.getProperty("java.vendor"),
            "os",
            System.getProperty("os.name"),
            "os_version",
            System.getProperty("os.version"),
            "passed",
            passed,
            "failed",
            failed,
            "outstanding",
            skipped,
            "results",
            results,
            "synthetic",
            true);
    Mapping.save(output.resolve("results.json"), report);
    StringBuilder html =
        new StringBuilder(
            "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\""
                + " content=\"width=device-width,initial-scale=1\"><title>Java 8 mapping"
                + " tests</title><style>body{font:16px/1.6 system-ui;max-width:1040px;margin:40px"
                + " auto;padding:0"
                + " 24px;background:#f7f8f3;color:#173a30}td,th{text-align:left;padding:9px"
                + " 14px;border-bottom:1px solid"
                + " #d5dfda}table{width:100%;border-collapse:collapse}.note{padding:16px;background:#fff0cb}a{color:#145d4d}</style><h1>Java"
                + " 8 mapping tests</h1><p>"
                + passed
                + " passed; "
                + failed
                + " failed; "
                + skipped
                + " outstanding.</p><p>Executed on "
                + Mapping.escape(System.getProperty("os.name"))
                + " / Java "
                + Mapping.escape(System.getProperty("java.version"))
                + " ("
                + Mapping.escape(System.getProperty("java.vendor"))
                + ").</p><p class=\"note\">All test purposes, answers, decisions and analysis are"
                + " synthetic. These tests validate framework behavior, not Copilot reasoning or"
                + " business approval. Actual analyst Copilot execution and live Shopify validation"
                + " remain outstanding. WSDL provenance and the real interface purpose are"
                + " unconfirmed.</p><p>Generated scenarios: <a"
                + " href=\"scenarios/basic/Report.html\">Basic</a> · <a"
                + " href=\"scenarios/clarified/Report.html\">Clarified</a> · <a"
                + " href=\"scenarios/reopened/Report.html\">Reopened</a></p><table><thead><tr><th>Check</th><th>Result</th><th>Detail</th></tr></thead><tbody>");
    for (Object v : results) {
      Map<String, Object> r = o(v);
      html.append("<tr><td>")
          .append(Mapping.escape(r.get("name")))
          .append("</td><td>")
          .append(Mapping.escape(r.get("status")))
          .append("</td><td>")
          .append(Mapping.escape(r.get("detail")))
          .append("</td></tr>");
    }
    html.append("</tbody></table></html>");
    Mapping.write(output.resolve("Test-results.html"), html.toString());
  }

  static int run(Path root, Path output) throws Exception {
    Files.createDirectories(output);
    WorkflowTests suite = new WorkflowTests(root, output);
    suite.suite();
    suite.report();
    System.out.println(
        suite.passed
            + " passed, "
            + suite.failed
            + " failed, "
            + suite.skipped
            + " outstanding. "
            + output.resolve("Test-results.html"));
    return suite.failed == 0 ? 0 : 1;
  }
}
