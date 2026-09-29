import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/** Java 8, standard library only. Helpers validate/render; Copilot performs mapping reasoning. */
public final class Mapping {
  final Path root, fw;
  static final String[] LABELS = {
    "Interface name",
    "What this interface does",
    "Current flow",
    "Target flow",
    "Payload origin",
    "Reviewer",
    "Additional context"
  };
  static final String PROVENANCE =
      "WSDL provenance and role remain unconfirmed. Folder placement and a parseable schema do not"
          + " establish the source of this interface.";

  Mapping(Path root) {
    this.root = root.toAbsolutePath().normalize();
    this.fw = this.root.resolve(".framework");
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> obj(Object x) {
    if (!(x instanceof Map)) throw new IllegalArgumentException("Expected a JSON object");
    return (Map<String, Object>) x;
  }

  @SuppressWarnings("unchecked")
  static List<Object> arr(Object x) {
    if (!(x instanceof List)) throw new IllegalArgumentException("Expected a JSON array");
    return (List<Object>) x;
  }

  static Map<String, Object> map(Object... pairs) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int n = 0; n < pairs.length; n += 2) result.put((String) pairs[n], pairs[n + 1]);
    return result;
  }

  static List<Object> list(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }

  static String str(Object x) {
    return x == null ? "" : x.toString();
  }

  static List<Object> optionalList(Map<String, Object> m, String key) {
    return m.containsKey(key) ? arr(m.get(key)) : list();
  }

  static boolean unknown(Object text) {
    String s = str(text).trim().toLowerCase(Locale.ROOT).replaceAll("^[ .!?]+|[ .!?]+$", "");
    return s.isEmpty()
        || s.startsWith("[")
        || Arrays.asList(
                "unknown",
                "tbd",
                "todo",
                "not sure",
                "n/a",
                "na",
                "none",
                "i don't know",
                "i do not know",
                "not known")
            .contains(s);
  }

  static String read(Path path) throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    int offset = 0;
    Charset charset = StandardCharsets.UTF_8;
    if (bytes.length >= 2 && bytes[0] == (byte) 255 && bytes[1] == (byte) 254) {
      charset = StandardCharsets.UTF_16LE;
      offset = 2;
    } else if (bytes.length >= 2 && bytes[0] == (byte) 254 && bytes[1] == (byte) 255) {
      charset = StandardCharsets.UTF_16BE;
      offset = 2;
    } else if (bytes.length >= 3
        && bytes[0] == (byte) 239
        && bytes[1] == (byte) 187
        && bytes[2] == (byte) 191) offset = 3;
    try {
      return charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
          .toString();
    } catch (CharacterCodingException e) {
      throw new IOException(
          path.getFileName() + ": save as UTF-8 or BOM-marked UTF-16 and retry.", e);
    }
  }

  static void write(Path path, String text) throws IOException {
    Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), ".mapping-", ".tmp");
    try {
      Files.write(temp, text.getBytes(StandardCharsets.UTF_8));
      Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      throw new IOException(
          "This folder does not support atomic saves. Use a local writable folder; the previous"
              + " file was preserved.",
          e);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  static Object json(Path path) throws IOException {
    return Json.parse(read(path));
  }

  static void save(Path path, Object value) throws IOException {
    write(path, Json.stringify(value) + "\n");
  }

  static Object copy(Object value) {
    return Json.parse(Json.stringify(value));
  }

  static String hash(byte[] bytes) {
    try {
      StringBuilder b = new StringBuilder();
      for (byte v : MessageDigest.getInstance("SHA-256").digest(bytes))
        b.append(String.format(Locale.ROOT, "%02x", v & 255));
      return b.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  static String fingerprint(Object value) {
    return hash(Json.stringify(value).getBytes(StandardCharsets.UTF_8));
  }

  Path p(String relative) {
    return root.resolve(relative);
  }

  Map<String, Object> load(String name) throws IOException {
    return obj(json(fw.resolve(name)));
  }

  static List<MatcherData> marks(String text, String regex) {
    List<MatcherData> result = new ArrayList<>();
    Matcher m = Pattern.compile(regex).matcher(text);
    while (m.find())
      result.add(new MatcherData(m.start(), m.end(), m.groupCount() > 0 ? m.group(1) : ""));
    return result;
  }

  static final class MatcherData {
    final int start, end;
    final String key;

    MatcherData(int s, int e, String k) {
      start = s;
      end = e;
      key = k;
    }
  }

  Map<String, Object> understanding() throws IOException {
    Map<String, Object> result = map();
    if (!Files.exists(p("Understanding.txt"))) return result;
    String text = read(p("Understanding.txt"));
    List<MatcherData> marks =
        marks(text, "(?im)^[ \\t]*(" + String.join("|", LABELS) + ")[ \\t]*:[ \\t]*");
    for (int n = 0; n < marks.size(); n++) {
      MatcherData m = marks.get(n);
      String key = m.key.toLowerCase(Locale.ROOT);
      if (result.containsKey(key))
        throw new IllegalArgumentException("Understanding.txt contains duplicate label " + key);
      result.put(
          key,
          text.substring(m.end, n + 1 < marks.size() ? marks.get(n + 1).start : text.length())
              .trim());
    }
    return result;
  }

  void setUnderstanding(Map<String, Object> updates) throws IOException {
    Map<String, Object> data = understanding();
    for (String k : updates.keySet()) data.put(k.toLowerCase(Locale.ROOT), updates.get(k));
    List<String> blocks = new ArrayList<>();
    for (String label : LABELS)
      blocks.add((label + ": " + str(data.get(label.toLowerCase(Locale.ROOT)))).trim());
    write(p("Understanding.txt"), String.join("\n\n", blocks) + "\n");
  }

  Map<String, Object> questions() throws IOException {
    Map<String, Object> result = map();
    if (!Files.exists(p("Questions.txt"))) return result;
    String text = read(p("Questions.txt"));
    List<MatcherData> marks = marks(text, "(?m)^[ \\t]*(Q\\d+)[ \\t]*:");
    for (int n = 0; n < marks.size(); n++) {
      MatcherData m = marks.get(n);
      if (result.containsKey(m.key))
        throw new IllegalArgumentException("Questions.txt contains duplicate question " + m.key);
      String block =
          text.substring(m.end, n + 1 < marks.size() ? marks.get(n + 1).start : text.length());
      Matcher a = Pattern.compile("(?im)^[ \\t]*Answer[ \\t]*:[ \\t]*").matcher(block);
      result.put(m.key, a.find() ? block.substring(a.end()).trim() : "");
    }
    return result;
  }

  void setAnswers(Map<String, Object> updates, List<Object> definitions) throws IOException {
    questions();
    String text =
        Files.exists(p("Questions.txt"))
            ? read(p("Questions.txt"))
            : "QUESTIONS FOR THE ANALYST\n\n";
    for (String id : updates.keySet()) {
      List<MatcherData> marks = marks(text, "(?m)^[ \\t]*(Q\\d+)[ \\t]*:");
      boolean found = false;
      for (int n = 0; n < marks.size(); n++) {
        MatcherData m = marks.get(n);
        if (!m.key.equals(id)) continue;
        int end = n + 1 < marks.size() ? marks.get(n + 1).start : text.length();
        String block = text.substring(m.end, end);
        Matcher a = Pattern.compile("(?im)^[ \\t]*Answer[ \\t]*:").matcher(block);
        if (a.find()) block = block.substring(0, a.start());
        text =
            text.substring(0, m.end)
                + block.replaceAll("\\s+$", "")
                + "\nAnswer: "
                + updates.get(id)
                + "\n\n"
                + text.substring(end);
        found = true;
        break;
      }
      if (!found) {
        Map<String, Object> q = find(definitions, "id", id);
        if (q == null) throw new IllegalArgumentException("Unknown question " + id);
        text +=
            "\n"
                + id
                + ": "
                + q.get("question")
                + "\n"
                + q.get("hint")
                + "\nAnswer: "
                + updates.get(id)
                + "\n";
      }
    }
    write(p("Questions.txt"), text.replaceAll("\\s+$", "") + "\n");
  }

  /** CSV state machine: quoted multiline cells, Excel delimiters, and strict cell counts. */
  static List<List<String>> csv(String text, char delimiter) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false, closed = false;
    for (int i = 0; i <= text.length(); i++) {
      char c = i == text.length() ? '\n' : text.charAt(i);
      if (quoted) {
        if (i == text.length())
          throw new IllegalArgumentException("Decisions.csv: unterminated quoted cell");
        if (c == '"') {
          if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
            cell.append('"');
            i++;
          } else {
            quoted = false;
            closed = true;
          }
        } else cell.append(c);
        continue;
      }
      if (c == delimiter || c == '\r' || c == '\n') {
        row.add(cell.toString().trim());
        cell.setLength(0);
        closed = false;
        if (c != delimiter) {
          if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
          if (row.stream().anyMatch(s -> !s.isEmpty())) rows.add(row);
          row = new ArrayList<>();
        }
        continue;
      }
      if (closed) {
        if (c == ' ' || c == '\t') continue;
        throw new IllegalArgumentException("Decisions.csv: unexpected text after quote");
      }
      if (c == '"') {
        if (cell.length() != 0)
          throw new IllegalArgumentException("Decisions.csv: quote text containing delimiters");
        quoted = true;
      } else cell.append(c);
    }
    return rows;
  }

  List<Object> decisions() throws IOException {
    if (!Files.exists(p("Decisions.csv"))) return list();
    String text = read(p("Decisions.csv"));
    String first = text.split("\\r?\\n", 2)[0];
    char delimiter = first.indexOf(';') >= 0 ? ';' : first.indexOf('\t') >= 0 ? '\t' : ',';
    List<List<String>> cells = csv(text, delimiter);
    if (cells.isEmpty()) throw new IllegalArgumentException("Decisions.csv needs column headers");
    List<String> headers = cells.get(0);
    String[] required = {"Attribute", "Decision", "Explanation", "Confirmed by"};
    Map<String, Integer> indices = new HashMap<>();
    for (String h : required) {
      for (int n = 0; n < headers.size(); n++)
        if (h.equalsIgnoreCase(headers.get(n))) {
          if (indices.containsKey(h))
            throw new IllegalArgumentException("Decisions.csv has duplicate columns");
          indices.put(h, n);
        }
      if (!indices.containsKey(h))
        throw new IllegalArgumentException(
            "Decisions.csv needs columns Attribute, Decision, Explanation and Confirmed by.");
    }
    List<Object> result = list();
    for (List<String> row : cells.subList(1, cells.size())) {
      if (row.size() != headers.size())
        throw new IllegalArgumentException(
            "Decisions.csv has missing or extra cells. Quote text containing commas.");
      Map<String, Object> data = map();
      for (String h : required) data.put(h, row.get(indices.get(h)));
      result.add(data);
    }
    return result;
  }

  void writeDecisions(List<Object> rows) throws IOException {
    String[] headers = {"Attribute", "Decision", "Explanation", "Confirmed by"};
    StringBuilder b = new StringBuilder(String.join(",", headers) + "\r\n");
    for (Object value : rows) {
      Map<String, Object> row = obj(value);
      List<String> cells = new ArrayList<>();
      for (String h : headers) cells.add("\"" + str(row.get(h)).replace("\"", "\"\"") + "\"");
      b.append(String.join(",", cells)).append("\r\n");
    }
    write(p("Decisions.csv"), b.toString());
  }

  static Map<String, Object> confirmed(List<Object> rows) {
    Map<String, List<Map<String, Object>>> groups = new TreeMap<>();
    for (Object v : rows) {
      Map<String, Object> r = obj(v);
      if (!str(r.get("Decision")).isEmpty())
        groups.computeIfAbsent(str(r.get("Attribute")), k -> new ArrayList<>()).add(r);
    }
    List<Object> rules = list(), warnings = list();
    for (String attr : groups.keySet()) {
      List<Map<String, Object>> entries = groups.get(attr);
      Set<Object> statements = new HashSet<>();
      for (Map<String, Object> r : entries) statements.add(r.get("Decision"));
      if (statements.size() > 1) {
        warnings.add(
            "Conflicting decisions for "
                + attr
                + ". That field needs clarification; no confirmation was carried forward.");
        continue;
      }
      Map<String, Object> r = entries.get(entries.size() - 1);
      if (!unknown(r.get("Confirmed by")) && !unknown(r.get("Decision")))
        rules.add(
            map(
                "id",
                "decision:" + attr,
                "attribute",
                attr,
                "statement",
                r.get("Decision"),
                "confirmed_by",
                r.get("Confirmed by")));
    }
    return map("rules", rules, "warnings", warnings);
  }

  void local(Path path) throws IOException {
    Path full = path.toAbsolutePath().normalize();
    if (!full.startsWith(root))
      throw new IOException("Evidence path escapes the interface folder.");
    for (Path part = full; part != null && part.startsWith(root); part = part.getParent())
      if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)) {
        BasicFileAttributes a =
            Files.readAttributes(part, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (a.isSymbolicLink() || a.isOther())
          throw new IOException(
              "Linked files or junctions are not supported. Copy evidence into this folder.");
        if (!part.toRealPath().startsWith(root.toRealPath()))
          throw new IOException("An input points outside this folder.");
      }
  }

  List<Path> files(Path folder) throws IOException {
    List<Path> result = new ArrayList<>();
    if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return result;
    local(folder);
    try (DirectoryStream<Path> children = Files.newDirectoryStream(folder)) {
      for (Path child : children) {
        local(child);
        if (Files.isDirectory(child)) result.addAll(files(child));
        else result.add(child);
      }
    }
    Collections.sort(result);
    return result;
  }

  static Document xml(Path path) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setFeature("http://xml.org/sax/features/external-general-entities", false);
    f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    f.setXIncludeAware(false);
    f.setExpandEntityReferences(false);
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    DocumentBuilder b = f.newDocumentBuilder();
    b.setErrorHandler(
        new org.xml.sax.helpers.DefaultHandler() {
          public void error(SAXParseException e) throws SAXException {
            throw e;
          }

          public void fatalError(SAXParseException e) throws SAXException {
            throw e;
          }
        });
    return b.parse(path.toFile());
  }

  static String escape(Object s) {
    return str(s)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  void notice(String title, List<Object> items) throws IOException {
    StringBuilder b =
        new StringBuilder(
            "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\""
                + " content=\"width=device-width,initial-scale=1\"><title>Mapping input"
                + " check</title><style>body{max-width:800px;margin:60px auto;padding:0"
                + " 24px;background:#f7f8f3;color:#173a30;font:17px/1.65 system-ui}li{margin:14px"
                + " 0}.notice{padding:18px;background:#fff0cb}</style><body><p>INTERFACE MAPPING /"
                + " INPUT CHECK</p><h1>"
                + escape(title)
                + "</h1><div class=\"notice\"><ul>");
    for (Object s : items) b.append("<li>").append(escape(s)).append("</li>");
    b.append(
        "</ul></div><p>Tell Copilot the missing details in chat, then run"
            + " <strong>/map-interface</strong> again. Copilot saves your clarification. You can"
            + " also edit the indicated files.</p><p>No new mapping analysis was generated.</p>");
    if (Files.exists(p("Last-review.html")))
      b.append(
          "<p><a href=\"Last-review.html\">Open the last completed review (may be"
              + " outdated)</a></p>");
    write(p("Report.html"), b.append("</body></html>").toString());
  }

  Map<String, Object> preflight() throws Exception {
    Map<String, Object> cfg = load("Required-inputs.json");
    for (String k :
        Arrays.asList(
            "require_description",
            "require_parseable_source_contract_or_payload",
            "require_official_shopify_reference"))
      if (!(cfg.get(k) instanceof Boolean))
        throw new IllegalArgumentException("Framework configuration needs a boolean for " + k);
    List<Path> files = files(p("Current"));
    files.addAll(files(p("Shopify")));
    for (String n :
        Arrays.asList(
            "Understanding.txt",
            "Questions.txt",
            "Decisions.csv",
            ".framework/Required-inputs.json")) {
      local(p(n));
      if (Files.isRegularFile(p(n))) files.add(p(n));
    }
    List<Object> missing = list(), warnings = list(PROVENANCE), usable = list();
    Map<String, Object> u = understanding();
    if (Boolean.TRUE.equals(cfg.get("require_description"))
        && unknown(u.get("what this interface does")))
      missing.add(
          "Understanding.txt: provide one sentence describing the business purpose after What this"
              + " interface does:.");
    if (unknown(u.get("interface name")))
      missing.add("Understanding.txt: provide the interface name.");
    for (Path file : files) {
      String relative = root.relativize(file).toString().replace('\\', '/'),
          name = file.getFileName().toString();
      if (!relative.startsWith("Current/Source/") || name.equals("availability-normalized.xml"))
        continue;
      try {
        if (name.toLowerCase(Locale.ROOT).matches(".*\\.(xml|wsdl|xsd)")) {
          Document d = xml(file);
          Element e = d.getDocumentElement();
          if (e.getLocalName().equals("definitions")
              && d.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element").getLength()
                  == 0) {
            warnings.add(name + ": imports external schemas; not used as sole parseable contract.");
            continue;
          }
          if (!e.hasChildNodes() && e.getTextContent().trim().isEmpty()) {
            warnings.add(name + ": empty document is not source evidence.");
            continue;
          }
          usable.add(relative);
        } else if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
          Object v = json(file);
          if (v instanceof Map && !obj(v).isEmpty() || v instanceof List && !arr(v).isEmpty())
            usable.add(relative);
        }
      } catch (Exception e) {
        warnings.add(
            name
                + ": incomplete, invalid or DTD-bearing structured file; kept as supplementary"
                + " evidence.");
      }
    }
    if (Boolean.TRUE.equals(cfg.get("require_parseable_source_contract_or_payload"))
        && usable.isEmpty())
      missing.add(
          "Current/Source: add a readable WSDL with embedded schemas, XSD, complete XML payload or"
              + " nonempty JSON payload. Notes or the repaired sample alone do not satisfy this"
              + " check.");
    boolean good = false;
    try {
      Map<String, Object> t = obj(json(p("Shopify/Target-reference.json")));
      boolean official = false;
      for (Object v : arr(t.get("evidence"))) {
        URI uri = new URI(str(obj(v).get("url")));
        if ("https".equals(uri.getScheme()) && "shopify.dev".equals(uri.getHost())) official = true;
      }
      good =
          "Shopify".equals(t.get("platform"))
              && !unknown(t.get("api_version"))
              && !arr(t.get("candidates")).isEmpty()
              && official;
    } catch (Exception e) {
      good = false;
    }
    if (Boolean.TRUE.equals(cfg.get("require_official_shopify_reference")) && !good)
      missing.add(
          "Shopify/Target-reference.json: obtain official Shopify target documentation with an API"
              + " version and candidate operations before analysis.");
    try {
      warnings.addAll(arr(confirmed(decisions()).get("warnings")));
      questions();
    } catch (Exception e) {
      missing.add(e.getMessage());
    }
    Map<String, Object> hashes = map();
    for (Path file : files)
      hashes.put(
          root.relativize(file).toString().replace('\\', '/'), hash(Files.readAllBytes(file)));
    Map<String, Object> result =
        map(
            "status",
            missing.isEmpty() ? "pass" : "blocked",
            "checked_at",
            Instant.now().toString(),
            "missing",
            missing,
            "warnings",
            warnings,
            "usable_source_files",
            usable,
            "human_inputs_sha256",
            fingerprint(hashes));
    save(fw.resolve("preflight.json"), result);
    return result;
  }

  Map<String, Object> assemble(
      Map<String, Object> previous, Map<String, Object> analysis, Map<String, Object> checked)
      throws Exception {
    Map<String, Object> data = obj(copy(previous)),
        u = understanding(),
        in = obj(data.get("interface"));
    String[] labels = {
      "interface name",
      "name",
      "what this interface does",
      "description",
      "current flow",
      "current_flow",
      "target flow",
      "target_flow",
      "payload origin",
      "source_origin",
      "reviewer",
      "reviewer"
    };
    for (int n = 0; n < labels.length; n += 2) in.put(labels[n + 1], str(u.get(labels[n])));
    data.put("additional_context", str(u.get("additional context")));
    Map<String, Object> answers = map();
    for (Object v : arr(analysis.get("questions"))) answers.put(str(obj(v).get("id")), "");
    answers.putAll(questions());
    data.put("answers", answers);
    data.put("known_rules", confirmed(decisions()).get("rules"));
    data.put("human_inputs_sha256", checked.get("human_inputs_sha256"));
    List<Object> matching = list();
    for (Path file : files(p("Current/Source"))) {
      if (!file.toString().toLowerCase(Locale.ROOT).endsWith(".wsdl")) continue;
      Document d;
      try {
        d = xml(file);
      } catch (Exception e) {
        continue;
      }
      NodeList types = d.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "complexType");
      for (int n = 0; n < types.getLength(); n++) {
        Element t = (Element) types.item(n);
        if (!"WebAvailabilityItem".equals(t.getAttribute("name"))) continue;
        List<Object> fields = list();
        NodeList seqs = t.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "sequence");
        for (int s = 0; s < seqs.getLength(); s++) {
          NodeList nodes = seqs.item(s).getChildNodes();
          for (int k = 0; k < nodes.getLength(); k++) {
            Node node = nodes.item(k);
            if (!(node instanceof Element) || !"element".equals(node.getLocalName())) continue;
            Element e = (Element) node;
            fields.add(
                map(
                    "name",
                    e.getAttribute("name"),
                    "type",
                    e.getAttribute("type"),
                    "optional",
                    "0".equals(e.getAttribute("minOccurs")),
                    "nullable",
                    "true".equals(e.getAttribute("nillable"))));
          }
        }
        if (!fields.isEmpty()) matching.add(fields);
      }
    }
    if (matching.isEmpty())
      throw new IllegalArgumentException(
          "No embedded WebAvailabilityItem field inventory is available. This POC needs a reviewed"
              + " contract adapter for other schemas; old fields cannot be reused.");
    for (Object fields : matching)
      if (!fields.equals(matching.get(0)))
        throw new IllegalArgumentException(
            "Source contracts disagree about WebAvailabilityItem. Clarify which contract applies.");
    obj(data.get("source")).put("fields", matching.get(0));
    return data;
  }

  static void schema(Object value, Map<String, Object> s, String at) {
    if (s.containsKey("type")) {
      List<Object> types = s.get("type") instanceof List ? arr(s.get("type")) : list(s.get("type"));
      boolean ok = false;
      for (Object t : types)
        switch (str(t)) {
          case "object":
            ok |= value instanceof Map;
            break;
          case "array":
            ok |= value instanceof List;
            break;
          case "string":
            ok |= value instanceof String;
            break;
          case "integer":
            ok |= value instanceof Long || value instanceof Integer;
            break;
          case "boolean":
            ok |= value instanceof Boolean;
            break;
          case "null":
            ok |= value == null;
            break;
          default:
            throw new IllegalArgumentException("Unsupported schema type " + t);
        }
      if (!ok) throw new IllegalArgumentException(at + ": expected " + types);
    }
    if (s.containsKey("enum") && !arr(s.get("enum")).contains(value))
      throw new IllegalArgumentException(at + ": unexpected value");
    if (s.containsKey("const") && !Objects.equals(value, s.get("const")))
      throw new IllegalArgumentException(at + ": unexpected constant");
    if (s.containsKey("minimum")
        && ((Number) value).doubleValue() < ((Number) s.get("minimum")).doubleValue())
      throw new IllegalArgumentException(at + ": below minimum");
    if (value instanceof Map) {
      Map<String, Object> m = obj(value),
          props = s.containsKey("properties") ? obj(s.get("properties")) : map();
      for (Object k : optionalList(s, "required"))
        if (!m.containsKey(k)) throw new IllegalArgumentException(at + ": missing " + k);
      for (String k : m.keySet()) {
        if (props.containsKey(k)) schema(m.get(k), obj(props.get(k)), at + "." + k);
        else if (Boolean.FALSE.equals(s.get("additionalProperties")))
          throw new IllegalArgumentException(at + ": unexpected property " + k);
        else if (s.get("additionalProperties") instanceof Map)
          schema(m.get(k), obj(s.get("additionalProperties")), at + "." + k);
      }
    }
    if (value instanceof List && s.containsKey("items")) {
      List<Object> a = arr(value);
      for (int n = 0; n < a.size(); n++) schema(a.get(n), obj(s.get("items")), at + "[" + n + "]");
    }
  }

  static Set<String> ids(List<Object> items, String key, String label) {
    Set<String> seen = new HashSet<>();
    for (Object v : items)
      if (!seen.add(str(obj(v).get(key)))) throw new IllegalArgumentException("Duplicate " + label);
    return seen;
  }

  static Map<String, Object> find(List<Object> items, String key, Object value) {
    for (Object v : items) if (Objects.equals(obj(v).get(key), value)) return obj(v);
    return null;
  }

  void validate(Map<String, Object> in, Map<String, Object> a) throws Exception {
    schema(in, obj(json(fw.resolve("schemas/input.schema.json"))), "input");
    schema(a, obj(json(fw.resolve("schemas/analysis.schema.json"))), "analysis");
    if (!obj(in.get("interface")).get("id").equals(a.get("interface_id")))
      throw new IllegalArgumentException("Input and analysis refer to different interfaces");
    List<Object> fields = arr(obj(in.get("source")).get("fields"));
    Set<String> names = ids(fields, "name", "source field");
    if (names.isEmpty()) throw new IllegalArgumentException("No source fields were identified");
    if (!names.equals(ids(arr(a.get("mappings")), "source_field", "mapping field")))
      throw new IllegalArgumentException("Mapping coverage mismatch");
    Set<String> qids = ids(arr(a.get("questions")), "id", "question ID"),
        eids = ids(arr(a.get("evidence")), "id", "evidence ID");
    ids(arr(a.get("interface_gates")), "id", "gate ID");
    Map<String, Object> answers = obj(in.get("answers"));
    if (!qids.containsAll(answers.keySet()))
      throw new IllegalArgumentException("Answers contain unknown question IDs");
    for (Object v : arr(a.get("questions")))
      for (Object f : arr(obj(v).get("affected_fields")))
        if (!"*".equals(f) && !names.contains(f))
          throw new IllegalArgumentException("Unknown affected field");
    for (String c :
        Arrays.asList(
            "mappings",
            "target_requirements",
            "target_candidates",
            "observations",
            "interface_gates"))
      for (Object v : arr(a.get(c))) {
        Map<String, Object> row = obj(v);
        if (!qids.containsAll(optionalList(row, "question_ids")))
          throw new IllegalArgumentException(c + ": unknown question reference");
        if (!eids.containsAll(optionalList(row, "evidence_ids")))
          throw new IllegalArgumentException(c + ": unknown evidence reference");
      }
    for (Object v : arr(a.get("mappings"))) {
      Map<String, Object> row = obj(v);
      String f = str(row.get("source_field"));
      if (!Objects.equals(row.get("source_type"), find(fields, "name", f).get("type")))
        throw new IllegalArgumentException(f + ": source type does not match the contract");
      if (Arrays.asList("ready", "excluded").contains(row.get("status"))) {
        if (!"confirmed".equals(row.get("decision_basis")) || unknown(row.get("confirmed_by")))
          throw new IllegalArgumentException(
              f + ": ready/excluded needs a confirmed decision and reviewer");
        for (Object id : arr(row.get("question_ids")))
          if (unknown(answers.get(id)))
            throw new IllegalArgumentException(f + ": unanswered mapping dependency");
        Map<String, Object> rule = find(arr(in.get("known_rules")), "attribute", f);
        if (rule == null
            || !Objects.equals(rule.get("confirmed_by"), row.get("confirmed_by"))
            || unknown(rule.get("statement"))
            || !arr(row.get("evidence_ids")).contains(rule.get("id")))
          throw new IllegalArgumentException(
              f + ": confirmation must cite its current attributed decision from Decisions.csv");
        Map<String, Object> e = find(arr(a.get("evidence")), "id", rule.get("id"));
        if (e == null
            || !"Decisions.csv".equals(e.get("url"))
            || !Objects.equals(e.get("note"), rule.get("statement")))
          throw new IllegalArgumentException(
              f + ": decision evidence must quote the current statement and link Decisions.csv");
        if ("ready".equals(row.get("status"))
            && unknown(obj(in.get("target")).get("selected_operation")))
          throw new IllegalArgumentException(
              f + ": select the target operation before confirming the mapping");
      }
    }
    for (Object v : arr(a.get("questions"))) {
      Map<String, Object> q = obj(v);
      if ("resolved".equals(q.get("review_status"))
          && (unknown(answers.get(q.get("id"))) || unknown(q.get("resolution_note"))))
        throw new IllegalArgumentException(
            q.get("id") + ": resolved question needs an answer and a review note");
    }
  }

  static Set<String> affected(
      Map<String, Object> old, Map<String, Object> now, Map<String, Object> analysis) {
    Set<String> result = new HashSet<>();
    List<Object> mappings = arr(analysis.get("mappings"));
    for (String key : Arrays.asList("interface", "source", "target", "additional_context"))
      if (!Objects.equals(old.get(key), now.get(key)))
        for (Object v : mappings) result.add(str(obj(v).get("source_field")));
    Map<String, Object> before = obj(old.get("answers")), after = obj(now.get("answers"));
    for (Object v : mappings) {
      Map<String, Object> m = obj(v);
      String f = str(m.get("source_field"));
      if (!Objects.equals(
          find(arr(old.get("known_rules")), "attribute", f),
          find(arr(now.get("known_rules")), "attribute", f))) result.add(f);
      for (Object q : arr(m.get("question_ids")))
        if (!Objects.equals(before.get(q), after.get(q))) result.add(f);
    }
    for (Object v : arr(analysis.get("questions"))) {
      Map<String, Object> q = obj(v);
      if (!Objects.equals(before.get(q.get("id")), after.get(q.get("id"))))
        for (Object m : mappings)
          if (arr(q.get("affected_fields")).contains("*")
              || arr(q.get("affected_fields")).contains(obj(m).get("source_field")))
            result.add(str(obj(m).get("source_field")));
    }
    return result;
  }

  static void reopen(
      Map<String, Object> old, Map<String, Object> now, Map<String, Object> analysis) {
    Set<String> changed = affected(old, now, analysis);
    for (Object v : arr(analysis.get("mappings"))) {
      Map<String, Object> m = obj(v);
      if (changed.contains(m.get("source_field"))
          && Arrays.asList("ready", "excluded").contains(m.get("status"))) {
        m.putAll(
            map(
                "status",
                "needs_input",
                "decision_basis",
                "proposal",
                "confirmed_by",
                null,
                "reason",
                "The supporting inputs changed. Copilot must review the current decision and"
                    + " dependencies.",
                "target",
                "Awaiting renewed review",
                "proposed_rule",
                "Reassess the prior proposal against the saved clarification before confirming."));
        arr(m.get("evidence_ids")).removeIf(id -> str(id).startsWith("decision:"));
      }
    }
    for (Object v : arr(analysis.get("questions"))) {
      Map<String, Object> q = obj(v);
      List<Object> fields = arr(q.get("affected_fields"));
      if (!Objects.equals(
              obj(old.get("answers")).get(q.get("id")), obj(now.get("answers")).get(q.get("id")))
          || fields.stream().anyMatch(changed::contains)
          || fields.contains("*") && !changed.isEmpty()) {
        q.put("review_status", "needs_clarification");
        q.put("resolution_note", "");
      }
    }
    // Do not refresh the analysis hash. Copilot must inspect and review the imported change.
  }

  final class RunLock implements AutoCloseable {
    final FileChannel channel;
    final FileLock lock;

    RunLock() throws IOException {
      channel =
          FileChannel.open(
              fw.resolve("run.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      FileLock acquired = null;
      try {
        acquired = channel.tryLock();
        if (acquired == null)
          throw new IOException("Another run is using this folder. Wait and retry.");
        lock = acquired;
      } catch (Exception e) {
        channel.close();
        throw new IOException(
            "Another run is using this folder, or the folder is not writable. Wait and retry.", e);
      }
    }

    public void close() throws IOException {
      lock.release();
      channel.close();
    }
  }

  Path archive(String group) throws IOException {
    Path path =
        fw.resolve("history")
            .resolve(group)
            .resolve(
                Instant.now().toString().replace(":", "")
                    + "-"
                    + UUID.randomUUID().toString().substring(0, 8));
    return Files.createDirectories(path);
  }

  int prepare(Map<String, Object> edits, Path imported) {
    try (RunLock ignored = new RunLock()) {
      try {
        Map<String, Object> old = load("input.json"),
            a = load("analysis.json"),
            updates = map(),
            answers = map();
        List<Object> rows = decisions();
        Set<String> known = ids(arr(a.get("questions")), "id", "question ID");
        for (String key : edits.keySet())
          if (!Arrays.asList(
                  "purpose", "source_origin", "reviewer", "context", "answers", "decisions")
              .contains(key))
            throw new IllegalArgumentException("Unknown clarification property " + key);
        if (imported != null) {
          if (!edits.isEmpty())
            throw new IllegalArgumentException("Import saved answers separately from other edits.");
          Map<String, Object> value = obj(json(imported));
          schema(value, obj(json(fw.resolve("schemas/input.schema.json"))), "answers");
          if (!Objects.equals(
              obj(value.get("interface")).get("id"), obj(old.get("interface")).get("id")))
            throw new IllegalArgumentException("These saved answers belong to another interface.");
          if (!Objects.equals(
              value.get("human_inputs_sha256"), preflight().get("human_inputs_sha256")))
            throw new IllegalArgumentException(
                "Files changed since these answers were saved. Reconcile with current notes; no"
                    + " files were overwritten.");
          for (String key : Arrays.asList("source", "target", "known_rules"))
            if (!Objects.equals(value.get(key), old.get(key)))
              throw new IllegalArgumentException(
                  "The answer file changes source, target or confirmed rules. Review those changes"
                      + " separately.");
          Map<String, Object> vi = obj(value.get("interface"));
          updates =
              map(
                  "What this interface does",
                  vi.get("description"),
                  "Payload origin",
                  vi.get("source_origin"),
                  "Reviewer",
                  vi.get("reviewer"),
                  "Additional context",
                  value.get("additional_context"));
          answers = obj(value.get("answers"));
        }
        String[] labels = {
          "purpose",
          "What this interface does",
          "source_origin",
          "Payload origin",
          "reviewer",
          "Reviewer"
        };
        for (int n = 0; n < labels.length; n += 2)
          if (edits.containsKey(labels[n])) updates.put(labels[n + 1], edits.get(labels[n]));
        if (edits.containsKey("context")) {
          if (!(edits.get("context") instanceof String))
            throw new IllegalArgumentException("Context must be text");
          updates.put(
              "Additional context",
              (str(understanding().get("additional context")) + "\n" + edits.get("context"))
                  .trim());
        }
        if (edits.containsKey("answers")) answers = obj(edits.get("answers"));
        for (String id : answers.keySet()) {
          if (!known.contains(id)) throw new IllegalArgumentException("Unknown question ID: " + id);
          if (!(answers.get(id) instanceof String)
              || Pattern.compile("(?m)^[ \\t]*Q\\d+[ \\t]*:").matcher(str(answers.get(id))).find())
            throw new IllegalArgumentException(
                "Answers must be text without embedded question headings.");
        }
        for (Object value : updates.values())
          if (!(value instanceof String)
              || Pattern.compile("(?im)^[ \\t]*(" + String.join("|", LABELS) + ")[ \\t]*:")
                  .matcher(str(value))
                  .find())
            throw new IllegalArgumentException(
                "Clarifications must be text without embedded Understanding.txt headings.");
        Set<String> editedAttrs = new HashSet<>();
        for (Object v : optionalList(edits, "decisions")) {
          Map<String, Object> d = obj(v);
          schema(
              d,
              map(
                  "type",
                  "object",
                  "required",
                  list("attribute", "decision", "explanation", "confirmed_by"),
                  "additionalProperties",
                  map("type", "string")),
              "decision");
          String attr = str(d.get("attribute"));
          if (!editedAttrs.add(attr))
            throw new IllegalArgumentException("Conflicting duplicate decision edits for " + attr);
          if (find(arr(obj(old.get("source")).get("fields")), "name", attr) == null)
            throw new IllegalArgumentException("Unknown source attribute " + attr);
          rows.removeIf(r -> attr.equals(obj(r).get("Attribute")));
          rows.add(
              map(
                  "Attribute",
                  attr,
                  "Decision",
                  d.get("decision"),
                  "Explanation",
                  d.get("explanation"),
                  "Confirmed by",
                  d.get("confirmed_by")));
        }
        if (!updates.isEmpty() || !answers.isEmpty() || !editedAttrs.isEmpty()) {
          // Back up before edits, roll back partial saves on I/O failure. Valid but incomplete
          // answers stay saved.
          Path backup = archive("input-edits");
          String[] names = {"Understanding.txt", "Questions.txt", "Decisions.csv"};
          for (String name : names)
            if (Files.exists(p(name))) Files.copy(p(name), backup.resolve(name));
          try {
            if (!updates.isEmpty()) setUnderstanding(updates);
            if (!answers.isEmpty()) setAnswers(answers, arr(a.get("questions")));
            if (!editedAttrs.isEmpty()) writeDecisions(rows);
          } catch (Exception e) {
            for (String name : names)
              if (Files.exists(backup.resolve(name)))
                Files.copy(backup.resolve(name), p(name), StandardCopyOption.REPLACE_EXISTING);
              else Files.deleteIfExists(p(name));
            throw e;
          }
        }
        Map<String, Object> checked = preflight();
        if (!arr(checked.get("missing")).isEmpty()) {
          notice("Add these details before we run", arr(checked.get("missing")));
          return 2;
        }
        Map<String, Object> data = assemble(old, a, checked);
        reopen(old, data, a);
        save(fw.resolve("analysis.json"), a);
        save(fw.resolve("input.json"), data);
        return 0;
      } catch (Exception e) {
        notice("Input needs attention", list(e.getMessage()));
        System.err.println(e.getMessage());
        return 2;
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return 2;
    }
  }

  String html(Map<String, Object> in, Map<String, Object> a, List<Object> warnings)
      throws Exception {
    validate(in, a);
    if (!fingerprint(in).equals(a.get("input_sha256")))
      throw new IllegalArgumentException("Input changed; Copilot must review it.");
    for (Object v : arr(a.get("evidence"))) {
      Map<String, Object> e = obj(v);
      if (str(e.get("url")).startsWith("Current/") && e.containsKey("sha256")) {
        Path file = p(str(e.get("url")));
        local(file);
        if (!Files.isRegularFile(file)
            || !Objects.equals(hash(Files.readAllBytes(file)), e.get("sha256")))
          throw new IllegalArgumentException("Source evidence changed. Copilot must review it.");
      }
    }
    String payload =
        Json.stringify(map("input", in, "analysis", a, "stale", false, "input_warnings", warnings))
            .replace("<", "\\u003c")
            .replace(">", "\\u003e")
            .replace("&", "\\u0026");
    return read(fw.resolve("report-template.html")).replace("__PACK_DATA__", payload);
  }

  List<Path> completedHistory() throws IOException {
    List<Path> result = new ArrayList<>();
    Path history = fw.resolve("history");
    if (Files.isDirectory(history))
      try (DirectoryStream<Path> dirs = Files.newDirectoryStream(history)) {
        for (Path dir : dirs)
          if (Files.isRegularFile(dir.resolve("completed.json"))) result.add(dir);
      }
    Collections.sort(result);
    return result;
  }

  int generate() {
    try (RunLock ignored = new RunLock()) {
      String status = "blocked";
      byte[] lastBackup = null;
      boolean lastExisted = Files.exists(p("Last-review.html")), lastTouched = false;
      try {
        Map<String, Object> checked = preflight();
        if (!arr(checked.get("missing")).isEmpty()) {
          notice("Required input is missing", arr(checked.get("missing")));
          return 2;
        }
        Map<String, Object> in = load("input.json"), a = load("analysis.json");
        if (!Objects.equals(in.get("human_inputs_sha256"), checked.get("human_inputs_sha256"))
            || !fingerprint(in).equals(a.get("input_sha256"))) {
          status = "needs_analysis";
          notice(
              "Copilot needs to analyse the updated files",
              list(
                  "Required inputs passed, but the analysis does not correspond to these files."
                      + " Continue RUN.md and review the new input."));
          return 3;
        }
        Map<String, Object> assembled = assemble(in, a, checked);
        for (String key :
            Arrays.asList("interface", "answers", "known_rules", "additional_context"))
          if (!Objects.equals(in.get(key), assembled.get(key)))
            throw new IllegalArgumentException(
                "Analyst "
                    + key
                    + " changed or was not imported. Run prepare and review the input.");
        if (!Objects.equals(
            obj(in.get("source")).get("fields"), obj(assembled.get("source")).get("fields")))
          throw new IllegalArgumentException(
              "Source fields changed; prepare and analyse the current contract.");
        validate(in, a);
        List<Path> history = completedHistory();
        if (!history.isEmpty()) {
          Path dir = history.get(history.size() - 1);
          Map<String, Object> prior = obj(json(dir.resolve("analysis.json"))),
              priorInput = obj(json(dir.resolve("input.json")));
          Set<String> changed = affected(priorInput, in, prior);
          for (Object v : arr(a.get("mappings"))) {
            Map<String, Object> row = obj(v),
                old = find(arr(prior.get("mappings")), "source_field", row.get("source_field"));
            if (old != null
                && (!Objects.equals(old.get("status"), row.get("status"))
                    || changed.contains(row.get("source_field"))
                        && Arrays.asList("ready", "excluded").contains(old.get("status")))
                && str(old.get("reason")).trim().equals(str(row.get("reason")).trim()))
              throw new IllegalArgumentException(
                  row.get("source_field")
                      + ": status or supporting decision changed but the explanation is unchanged."
                      + " Review and update the reason.");
          }
        }
        String html = html(in, a, arr(checked.get("warnings")));
        Path archive = archive("");
        save(archive.resolve("input.json"), in);
        save(archive.resolve("analysis.json"), a);
        write(archive.resolve("Report.html"), html);
        if (lastExisted) lastBackup = Files.readAllBytes(p("Last-review.html"));
        write(
            p("Last-review.html"),
            html.replace(
                "<body><main>",
                "<body><main><div class=\"notice\">Saved review - this may predate your latest"
                    + " clarifications. Run /map-interface for the current result.</div>"));
        lastTouched = true;
        write(p("Report.html"), html);
        save(archive.resolve("completed.json"), map("completed_at", Instant.now().toString()));
        status = "generated";
        return 0;
      } catch (Exception e) {
        if (lastTouched) {
          if (lastExisted) Files.write(p("Last-review.html"), lastBackup);
          else Files.deleteIfExists(p("Last-review.html"));
        }
        notice(
            "The report needs a correction",
            list(
                e.getMessage(),
                "Copilot should correct the analysis or configuration and rerun the checked"
                    + " generator."));
        System.err.println(e.getMessage());
        return 2;
      } finally {
        try {
          Map<String, Object> log =
              map(
                  "time",
                  Instant.now().toString(),
                  "status",
                  status,
                  "configured_execution_host",
                  "github_copilot_vscode",
                  "actual_execution_host",
                  "unknown",
                  "actual_model",
                  "unknown",
                  "java_version",
                  System.getProperty("java.version"),
                  "os",
                  System.getProperty("os.name"));
          Files.write(
              fw.resolve("run-history.jsonl"),
              (Json.stringify(log) + "\n").getBytes(StandardCharsets.UTF_8),
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND);
        } catch (IOException e) {
          System.err.println("Could not append run history; inspect folder write access.");
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return 2;
    }
  }

  static Path defaultRoot() throws Exception {
    return Paths.get(Mapping.class.getProtectionDomain().getCodeSource().getLocation().toURI())
        .getParent()
        .getParent();
  }

  static int run(String[] args) throws Exception {
    if (!"1.8".equals(System.getProperty("java.specification.version"))) {
      System.err.println(
          "This pack requires Java 1.8. Set java.home in java-home.properties to your approved Java"
              + " 8 folder.");
      return 2;
    }
    if (args.length == 0) {
      System.err.println(
          "Usage: map.cmd prepare [--edits FILE | --import-answers FILE] | fingerprint | generate |"
              + " test [--output DIR]");
      return 2;
    }
    if (args.length == 1 && args[0].equals("--launcher")) {
      List<String> launch = new ArrayList<>();
      launch.add(str(System.getenv("MAPPING_ACTION")));
      String[] names = {
        "MAPPING_ROOT",
        "--root",
        "MAPPING_EDITS",
        "--edits",
        "MAPPING_IMPORT",
        "--import-answers",
        "MAPPING_OUTPUT",
        "--output"
      };
      for (int n = 0; n < names.length; n += 2) {
        String value = System.getenv(names[n]);
        if (value != null && !value.isEmpty()) {
          launch.add(names[n + 1]);
          launch.add(value);
        }
      }
      return run(launch.toArray(new String[0]));
    }
    String action = args[0];

    Map<String, String> options = new HashMap<>();
    for (int n = 1; n < args.length; n += 2) {
      if (n + 1 >= args.length
          || !Arrays.asList("--root", "--edits", "--import-answers", "--output").contains(args[n])
          || options.put(args[n], args[n + 1]) != null)
        throw new IllegalArgumentException("Invalid or duplicate option: " + args[n]);
    }
    Set<String> allowed = new HashSet<>(Arrays.asList("--root"));
    if (action.equals("prepare")) allowed.addAll(Arrays.asList("--edits", "--import-answers"));
    if (action.equals("test")) allowed.add("--output");
    if (!allowed.containsAll(options.keySet()))
      throw new IllegalArgumentException("Option is not valid for " + action);
    Mapping m =
        new Mapping(
            options.containsKey("--root") ? Paths.get(options.get("--root")) : defaultRoot());
    switch (action) {
      case "prepare":
        Map<String, Object> edits;
        try {
          edits =
              options.containsKey("--edits") ? obj(json(Paths.get(options.get("--edits")))) : map();
        } catch (Exception e) {
          // A malformed chat data file is a failed run too, not a reason to display the
          // previous report as current. Take the same lock before replacing its notice.
          try (RunLock ignored = m.new RunLock()) {
            m.notice("Input needs attention", list(e.getMessage()));
          }
          System.err.println(e.getMessage());
          return 2;
        }
        return m.prepare(
            edits,
            options.containsKey("--import-answers")
                ? Paths.get(options.get("--import-answers"))
                : null);
      case "fingerprint":
        System.out.println(fingerprint(m.load("input.json")));
        return 0;
      case "generate":
        return m.generate();
      case "test":
        return WorkflowTests.run(
            m.root,
            options.containsKey("--output")
                ? Paths.get(options.get("--output"))
                : m.p("Test-results"));
      default:
        throw new IllegalArgumentException("Unknown action " + action);
    }
  }

  public static void main(String[] args) {
    int code;
    try {
      code = run(args);
    } catch (Exception e) {
      System.err.println("Mapping needs attention: " + e.getMessage());
      code = 2;
    }
    System.exit(code);
  }
}
