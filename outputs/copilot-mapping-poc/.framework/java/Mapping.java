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
    "Shopify endpoint/reference",
    "Shopify operation",
    "Shopify API version",
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
            "require_source_evidence",
            "require_shopify_endpoint_reference"))
      if (!(cfg.get(k) instanceof Boolean))
        throw new IllegalArgumentException("Framework configuration needs a boolean for " + k);
    List<Path> files = files(p("Current"));
    files.addAll(files(p("Shopify")));
    for (String n :
        Arrays.asList(
            "Understanding.txt",
            "Questions.txt",
            "Decisions.csv",
            ".framework/Required-inputs.json", ".framework/target-discovery.json", ".framework/contract-selection.json")) {
      local(p(n));
      if (Files.isRegularFile(p(n))) files.add(p(n));
    }
    List<Object> missing = list(), warnings = list(PROVENANCE), usable = list();
    Map<String, Object> u = understanding();
    if (unknown(u.get("what this interface does")))
      warnings.add("Business purpose has not been confirmed. Agent-inferred mappings are proposals; this does not block useful field mapping.");
    try {
      List<Object> fields = payloadFields(obj(load("input.json").get("source")), warnings);
      for (Object field : fields)
        for (Object file : arr(obj(field).get("evidence_files")))
          if (!usable.contains(file)) usable.add(file);
    } catch (Exception e) {
      missing.add(e.getMessage());
    }
    try {
      targetInput(u);
    } catch (Exception e) {
      missing.add(e.getMessage());
    }
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
            "source_evidence_sha256",
            sourceFingerprint(),
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
    obj(data.get("source")).put("fields", payloadFields(obj(data.get("source")), list()));
    data.put("target", targetInput(u));
    return data;
  }

  static final String XML_TYPE = "XML text (sample; contract type unconfirmed)";

  static String xmlName(Node node) {
    String ns = node.getNamespaceURI();
    return (ns == null || ns.isEmpty() ? "" : "{" + ns + "}") + node.getLocalName();
  }

  static boolean metadataNamespace(String ns) {
    return ns != null && (ns.equals("http://www.w3.org/2001/XMLSchema")
        || ns.startsWith("http://schemas.xmlsoap.org/wsdl")
        || ns.equals("http://www.w3.org/ns/wsdl"));
  }

  void collectPayload(Element e, String parent, String file, Map<String, Object> fields) {
    String ns = e.getNamespaceURI(), name = e.getLocalName();
    if (metadataNamespace(ns)) return;
    boolean soap = "http://schemas.xmlsoap.org/soap/envelope/".equals(ns)
        || "http://www.w3.org/2003/05/soap-envelope".equals(ns);
    if (soap && !"Envelope".equals(name) && !"Body".equals(name)) return;
    String path = parent + "/" + xmlName(e);
    boolean children = false;
    NodeList nodes = e.getChildNodes();
    for (int n = 0; n < nodes.getLength(); n++) {
      if (nodes.item(n) instanceof Element) {
        children = true;
        collectPayload((Element) nodes.item(n), path, file, fields);
      }
    }
    if (soap) return;
    NamedNodeMap attrs = e.getAttributes();
    for (int n = 0; n < attrs.getLength(); n++) {
      Node attr = attrs.item(n);
      String ans = attr.getNamespaceURI();
      if (ans != null && (ans.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)
          || ans.equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
          || ans.equals(XMLConstants.XML_NS_URI) || metadataNamespace(ans))) continue;
      payloadField(fields, path + "/@" + xmlName(attr), "@" + attr.getLocalName(), file);
    }
    // A lone empty root is not meaningful input; empty leaf elements inside a payload are fields.
    if (!children && (!parent.isEmpty() || !e.getTextContent().trim().isEmpty()))
      payloadField(fields, path, name, file);
  }

  void payloadField(Map<String, Object> fields, String path, String name, String file) {
    Map<String, Object> field = fields.containsKey(path) ? obj(fields.get(path))
        : map("name", name, "path", path, "type", XML_TYPE, "optional", null,
            "nullable", null, "evidence_files", list());
    if (!arr(field.get("evidence_files")).contains(file)) arr(field.get("evidence_files")).add(file);
    fields.put(path, field);
  }

  List<Object> payloadFields(Map<String, Object> source, List<Object> warnings) throws Exception {
    Map<String, Object> fields = map();
    String sample = str(source.get("sample")), normalized = str(source.get("normalized_sample"));
    for (Path file : files(p("Current/Source"))) {
      String relative = root.relativize(file).toString().replace('\\', '/');
      if (!relative.toLowerCase(Locale.ROOT).endsWith(".xml") || relative.equals(normalized)) continue;
      Document doc;
      try {
        doc = xml(file);
      } catch (Exception error) {
        if (relative.equals(sample) && !normalized.isEmpty() && Files.isRegularFile(p(normalized))) {
          local(p(normalized));
          if (!normalized.startsWith("Current/Source/") || normalized.equals(sample))
            throw new IllegalArgumentException("Normalized payload must be a separate local source file.");
          String original = read(file).trim(), repaired = read(p(normalized)).trim();
          if (!repaired.startsWith(original)
              || !repaired.substring(original.length()).matches("(?:\\s*</[A-Za-z_][A-Za-z0-9_.:-]*>)+"))
            throw new IllegalArgumentException("Normalized XML differs beyond appended closing tags; review the original payload.");
          doc = xml(p(normalized));
          warnings.add(relative + ": incomplete excerpt; extracted via " + normalized
              + " after verifying only closing tags were appended. Values and business rules remain unconfirmed.");
        } else {
          throw new IllegalArgumentException(relative + ": unreadable/incomplete XML or DTD; supply a complete business payload.");
        }
      }
      Element rootElement = doc.getDocumentElement();
      if (metadataNamespace(rootElement.getNamespaceURI())
          || Arrays.asList("definitions", "description", "schema").contains(rootElement.getLocalName())) {
        warnings.add(relative + ": service/schema metadata retained as reference, not mapped.");
        continue;
      }
      collectPayload(rootElement, "", relative, fields);
    }
    if (fields.isEmpty()) fields.putAll(contractFields(warnings));
    if (fields.isEmpty()) throw new IllegalArgumentException(
        "Current/Source: supply readable XML or a business XSD/WSDL contract. Service metadata and an orphaned normalized copy are not business fields.");
    Map<String, Integer> counts = new HashMap<>();
    for (Object value : fields.values()) {
      String name = str(obj(value).get("name"));
      counts.put(name, counts.containsKey(name) ? counts.get(name) + 1 : 1);
    }
    for (Object value : fields.values()) {
      Map<String, Object> field = obj(value);
      if (counts.get(str(field.get("name"))) > 1) field.put("name", field.get("path"));
    }
    return new ArrayList<>(fields.values());
  }

  static final String XSD = "http://www.w3.org/2001/XMLSchema";

  String qname(Element e, String value) {
    int colon = value.indexOf(':');
    String ns = e.lookupNamespaceURI(colon < 0 ? null : value.substring(0, colon));
    return "{" + str(ns) + "}" + (colon < 0 ? value : value.substring(colon + 1));
  }

  Map<String, Object> contractFields(List<Object> warnings) throws Exception {
    Map<String, Object> result = map(), selection = Files.isRegularFile(fw.resolve("contract-selection.json"))
        ? load("contract-selection.json") : map();
    List<Element> roots = new ArrayList<>();
    List<String> origins = new ArrayList<>();
    Map<String, Element> declarations = new HashMap<>();
    for (Path file : files(p("Current/Source"))) {
      String relative = root.relativize(file).toString().replace('\\', '/');
      if (!relative.matches("(?i).*\\.(xsd|wsdl)$")) continue;
      if (!selection.isEmpty() && !relative.equals(selection.get("file"))) continue;
      Document doc = xml(file);
      NodeList schemas = doc.getElementsByTagNameNS(XSD, "schema");
      for (int n = 0; n < schemas.getLength(); n++) {
        Element schema = (Element) schemas.item(n);
        NodeList children = schema.getChildNodes();
        for (int k = 0; k < children.getLength(); k++) {
          if (!(children.item(k) instanceof Element)) continue;
          Element child = (Element) children.item(k);
          if (!XSD.equals(child.getNamespaceURI()) || !child.hasAttribute("name")) continue;
          String key = "{" + schema.getAttribute("targetNamespace") + "}" + child.getAttribute("name");
          declarations.put(child.getLocalName() + ":" + key, child);
          if ("element".equals(child.getLocalName()) && (!relative.endsWith(".wsdl") || !selection.isEmpty())) {
            if (selection.isEmpty() || (child.getAttribute("name").equals(selection.get("element"))
                && schema.getAttribute("targetNamespace").equals(str(selection.get("namespace"))))) {
              roots.add(child); origins.add(relative);
            }
          }
        }
      }
    }
    if (roots.isEmpty()) return result;
    if (roots.size() != 1) throw new IllegalArgumentException(
        "The contract has multiple business messages. Copilot must identify the relevant message from context and save contract-selection.json; ask only if its purpose is ambiguous.");
    contractElement(roots.get(0), "", origins.get(0), declarations, result, new HashSet<String>());
    warnings.add("Fields extracted from business schema declarations in " + origins.get(0)
        + "; service metadata is excluded. Contract role/provenance is not a business confirmation.");
    return result;
  }

  void contractElement(Element e, String parent, String file, Map<String, Element> declarations,
      Map<String, Object> fields, Set<String> active) {
    if (e.hasAttribute("ref")) {
      Element ref = declarations.get("element:" + qname(e, e.getAttribute("ref")));
      if (ref == null) throw new IllegalArgumentException("Contract references an unavailable business element; Copilot must inspect the supplied contract dependencies.");
      e = ref;
    }
    String name = e.getAttribute("name");
    if (name.isEmpty()) return;
    String path = parent + "/" + name;
    Element structure = null;
    NodeList children = e.getChildNodes();
    for (int n = 0; n < children.getLength(); n++)
      if (children.item(n) instanceof Element && "complexType".equals(children.item(n).getLocalName())) structure = (Element) children.item(n);
    String type = e.getAttribute("type"), typeKey = qname(e, type);
    if (structure == null && !type.isEmpty()) structure = declarations.get("complexType:" + typeKey);
    if (structure != null) {
      String token = file + ":" + typeKey + ":" + System.identityHashCode(structure);
      if (!active.add(token)) throw new IllegalArgumentException("Recursive contract needs a bounded business message selection; no truncated inventory was produced.");
      contractMembers(structure, path, file, declarations, fields, active);
      active.remove(token);
    } else {
      if (!type.isEmpty() && !typeKey.startsWith("{" + XSD + "}")
          && !declarations.containsKey("simpleType:" + typeKey))
        throw new IllegalArgumentException("Unresolved contract type " + type + "; supply its referenced schema.");
      payloadField(fields, path, name, file);
      Map<String, Object> field = obj(fields.get(path));
      field.put("type", "Contract declaration: " + (type.isEmpty() ? "text" : type));
      field.put("optional", "0".equals(e.getAttribute("minOccurs")));
      field.put("nullable", "true".equals(e.getAttribute("nillable")));
    }
  }

  void contractMembers(Element e, String parent, String file, Map<String, Element> declarations,
      Map<String, Object> fields, Set<String> active) {
    NodeList children = e.getChildNodes();
    for (int n = 0; n < children.getLength(); n++) {
      if (!(children.item(n) instanceof Element)) continue;
      Element child = (Element) children.item(n);
      if (!XSD.equals(child.getNamespaceURI())) continue;
      String kind = child.getLocalName();
      if ("element".equals(kind)) contractElement(child, parent, file, declarations, fields, active);
      else if ("attribute".equals(kind) && child.hasAttribute("name")) {
        String path = parent + "/@" + child.getAttribute("name");
        payloadField(fields, path, "@" + child.getAttribute("name"), file);
        obj(fields.get(path)).put("type", "Contract declaration: " + child.getAttribute("type"));
      } else if (Arrays.asList("sequence", "all", "choice", "complexContent", "simpleContent").contains(kind))
        contractMembers(child, parent, file, declarations, fields, active);
      else if (Arrays.asList("extension", "restriction", "group", "any", "attributeGroup").contains(kind))
        throw new IllegalArgumentException("This contract uses " + kind + "; Copilot must resolve its business structure or request an XML example. No guessed fields were emitted.");
    }
  }

  String sourceFingerprint() throws Exception {
    Map<String, Object> hashes = map();
    for (Path file : files(p("Current/Source")))
      hashes.put(root.relativize(file).toString().replace(File.separatorChar, (char)47), hash(Files.readAllBytes(file)));
    return fingerprint(hashes);
  }

  Map<String, Object> targetInput(Map<String, Object> u) throws Exception {

    String requested = str(u.get("shopify endpoint/reference")).trim(), reference = requested;
    Map<String, Object> discovered = map();
    if (Files.isRegularFile(fw.resolve("target-discovery.json"))) {
      Map<String, Object> value = load("target-discovery.json");
      if (requested.equals(str(value.get("requested_reference")))
          && sourceFingerprint().equals(value.get("source_sha256"))) discovered = value;
    }
    if (unknown(reference)) reference = str(discovered.get("reference"));
    if (unknown(reference)) return map("platform", "Shopify", "endpoint_reference", "",
        "api_version", "", "research_method", "agent_discovery_pending", "selected_operation", null,
        "selection_basis", "agent_proposal");
    URI uri;
    try { uri = new URI(reference); }
    catch (Exception e) { throw new IllegalArgumentException("Share the Shopify API endpoint/reference in Copilot chat."); }
    if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
        || uri.getQuery() != null || unknown(uri.getPath()))
      throw new IllegalArgumentException("Share the Shopify API endpoint/reference in Copilot chat (HTTPS URL without credentials or query parameters).");
    String operation = str(u.get("shopify operation")).trim();
    if (unknown(operation)) operation = str(discovered.get("operation"));
    Matcher op = Pattern.compile("/(?:mutations|queries)/([^/]+)/*$").matcher(uri.getPath());
    if (op.find()) {
      if (!unknown(operation) && !operation.equals(op.group(1)))
        throw new IllegalArgumentException("Shopify operation conflicts with the supplied reference; clarify the intended operation.");
      operation = op.group(1);
    }
    String version = str(u.get("shopify api version")).trim();
    if (unknown(version)) version = str(discovered.get("api_version"));
    Matcher ver = Pattern.compile("/(20[0-9]{2}-(?:01|04|07|10))(?:/|$)").matcher(uri.getPath());
    if (ver.find()) {
      if (!unknown(version) && !version.equals(ver.group(1)))
        throw new IllegalArgumentException("Shopify API version conflicts with the supplied endpoint/reference.");
      version = ver.group(1);
    }
    // Intake permits unresolved operation/version; the agent researches them before generation.
    return map("platform", "Shopify", "endpoint_reference", reference, "api_version", version,
        "research_method", "agent_inspects_official_documentation", "selected_operation", unknown(operation) ? null : operation,
        "selection_basis", unknown(requested) ? "agent_proposal" : "analyst_reference");
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
    Map<String, Object> target = obj(in.get("target"));
    String operation = str(target.get("selected_operation")), version = str(target.get("api_version"));
    if (unknown(operation) || !version.matches("20[0-9]{2}-(?:01|04|07|10)"))
      throw new IllegalArgumentException("Copilot must discover the relevant Shopify operation/version from the source and official API references before generating mappings.");
    Map<String, Object> selected = find(arr(a.get("target_candidates")), "operation", operation);

    if (selected == null || !"selected".equals(selected.get("state")))
      throw new IllegalArgumentException("Analysis must inspect the supplied Shopify operation and mark it selected.");
    boolean official = false;
    for (Object id : arr(selected.get("evidence_ids"))) {
      Map<String, Object> evidence = find(arr(a.get("evidence")), "id", id);
      if (evidence == null) continue;
      URI uri = new URI(str(evidence.get("url")));
      if ("https".equals(uri.getScheme()) && "shopify.dev".equals(uri.getHost())
          && version.equals(evidence.get("api_version")) && !unknown(evidence.get("retrieved_on"))
          && !unknown(evidence.get("note")) && uri.getPath().contains("/" + version + "/")
          && uri.getPath().startsWith("/docs/api/")
          && (uri.getPath().endsWith("/" + operation)
              || operation.matches("(?:GET|POST|PUT|PATCH|DELETE) /[^\\s]+")
                  && operation.equals(evidence.get("operation"))
                  && uri.getPath().contains("/admin-rest/"))) official = true;

    }
    if (!official) throw new IllegalArgumentException(
        "Copilot must inspect and cite versioned official Shopify documentation for the selected operation before generation.");
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
        throw new IllegalArgumentException(f + ": source type does not match the XML payload inventory");
      if ("proposed".equals(row.get("status"))) {
        if (!"proposal".equals(row.get("decision_basis")) || !unknown(row.get("confirmed_by"))
            || unknown(row.get("target")) || str(row.get("target")).startsWith("Pending")
            || unknown(row.get("proposed_rule")))
          throw new IllegalArgumentException(f + ": a proposal needs a concrete destination and rule, without invented confirmation.");
        boolean sourceEvidence = false, targetEvidence = false;
        for (Object id : arr(row.get("evidence_ids"))) {
          Map<String, Object> e = find(arr(a.get("evidence")), "id", id);
          if (e == null) continue;
          if (arr(find(fields, "name", f).get("evidence_files")).contains(e.get("url"))
              && e.containsKey("sha256")) sourceEvidence = true;
          URI url = new URI(str(e.get("url")));
          if ("https".equals(url.getScheme()) && "shopify.dev".equals(url.getHost())
              && version.equals(e.get("api_version")) && !unknown(e.get("note"))) targetEvidence = true;
        }
        if (!sourceEvidence || !targetEvidence)
          throw new IllegalArgumentException(f + ": proposed mapping must cite its source file and inspected Shopify API evidence.");
      }
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
          && Arrays.asList("ready", "excluded", "proposed").contains(m.get("status"))) {
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
                  "purpose", "source_origin", "reviewer", "context", "answers", "decisions",
                  "shopify_reference", "shopify_operation", "shopify_version")
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
          "Reviewer",
          "shopify_reference",
          "Shopify endpoint/reference",
          "shopify_operation",
          "Shopify operation",
          "shopify_version",
          "Shopify API version"
        };
        for (int n = 0; n < labels.length; n += 2)
          if (edits.containsKey(labels[n])) updates.put(labels[n + 1], edits.get(labels[n]));
        if (edits.containsKey("shopify_reference") && !Objects.equals(
            str(understanding().get("shopify endpoint/reference")), edits.get("shopify_reference"))) {
          if (!edits.containsKey("shopify_operation")) updates.put("Shopify operation", "");
          if (!edits.containsKey("shopify_version")) updates.put("Shopify API version", "");
        }
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
        if (!fingerprint(data).equals(a.get("input_sha256"))) {
          notice(
              "Copilot needs to analyse the updated files",
              list(
                  "Your clarifications are saved. Copilot must review them before generating the"
                      + " next mapping report."));
        }
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
    Map<String, Object> reportInput = obj(copy(in));
    reportInput.remove("answers");
    String payload =
        Json.stringify(map("input", reportInput, "analysis", a, "stale", false, "input_warnings", warnings))
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
            Arrays.asList("interface", "answers", "known_rules", "additional_context", "target"))
          if (!Objects.equals(in.get(key), assembled.get(key)))
            throw new IllegalArgumentException(
                "Analyst "
                    + key
                    + " changed or was not imported. Run prepare and review the input.");
        if (!Objects.equals(
            obj(in.get("source")).get("fields"), obj(assembled.get("source")).get("fields")))
          throw new IllegalArgumentException(
              "Source fields changed; prepare and analyse the current XML payload.");
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
                        && Arrays.asList("ready", "excluded", "proposed").contains(old.get("status")))
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
