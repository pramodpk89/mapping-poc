import java.math.BigDecimal;
import java.util.*;

/** Strict JSON subset parser/writer, with duplicate-key rejection. No external libraries. */
final class Json {
  private final String text;
  private int pos;

  private Json(String text) {
    this.text = text;
  }

  static Object parse(String text) {
    Json p = new Json(text);
    Object value = p.value(0);
    p.ws();
    if (p.pos != text.length()) throw p.error("Trailing data");
    return value;
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException("JSON at character " + pos + ": " + message);
  }

  private void ws() {
    while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++;
  }

  private boolean take(char c) {
    ws();
    if (pos < text.length() && text.charAt(pos) == c) {
      pos++;
      return true;
    }
    return false;
  }

  private void need(char c) {
    if (!take(c)) throw error("Expected " + c);
  }

  private Object value(int depth) {
    if (depth > 80) throw error("Nesting limit exceeded");
    ws();
    if (pos == text.length()) throw error("Missing value");
    char c = text.charAt(pos);
    if (c == '"') return string();
    if (c == '{') {
      pos++;
      Map<String, Object> map = new LinkedHashMap<>();
      if (take('}')) return map;
      do {
        ws();
        String key = string();
        need(':');
        if (map.containsKey(key)) throw error("Duplicate property " + key);
        map.put(key, value(depth + 1));
      } while (take(','));
      need('}');
      return map;
    }
    if (c == '[') {
      pos++;
      List<Object> list = new ArrayList<>();
      if (take(']')) return list;
      do {
        list.add(value(depth + 1));
      } while (take(','));
      need(']');
      return list;
    }
    for (String literal : Arrays.asList("true", "false", "null"))
      if (text.startsWith(literal, pos)) {
        pos += literal.length();
        return literal.equals("null") ? null : Boolean.valueOf(literal);
      }
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
            .matcher(text.substring(pos));
    if (!m.lookingAt()) throw error("Invalid value");
    String n = m.group();
    pos += n.length();
    try {
      if (n.indexOf('.') < 0 && n.indexOf('e') < 0 && n.indexOf('E') < 0) return Long.valueOf(n);
      return new BigDecimal(n);
    } catch (NumberFormatException e) {
      throw error("Invalid number");
    }
  }

  private String string() {
    if (pos >= text.length() || text.charAt(pos++) != '"') throw error("Expected string");
    StringBuilder b = new StringBuilder();
    while (pos < text.length()) {
      char c = text.charAt(pos++);
      if (c == '"') return b.toString();
      if (c < 32) throw error("Unescaped control character");
      if (c == '\\') {
        if (pos == text.length()) throw error("Incomplete escape");
        c = text.charAt(pos++);
        switch (c) {
          case '"':
          case '\\':
          case '/':
            b.append(c);
            break;
          case 'b':
            b.append('\b');
            break;
          case 'f':
            b.append('\f');
            break;
          case 'n':
            b.append('\n');
            break;
          case 'r':
            b.append('\r');
            break;
          case 't':
            b.append('\t');
            break;
          case 'u':
            if (pos + 4 > text.length()) throw error("Incomplete Unicode escape");
            try {
              b.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
            } catch (NumberFormatException e) {
              throw error("Invalid Unicode escape");
            }
            pos += 4;
            break;
          default:
            throw error("Invalid escape");
        }
      } else b.append(c);
    }
    throw error("Unterminated string");
  }

  static String stringify(Object value) {
    if (value == null) return "null";
    if (value instanceof String) {
      StringBuilder b = new StringBuilder("\"");
      for (char c : ((String) value).toCharArray())
        switch (c) {
          case '"':
            b.append("\\\"");
            break;
          case '\\':
            b.append("\\\\");
            break;
          case '\n':
            b.append("\\n");
            break;
          case '\r':
            b.append("\\r");
            break;
          case '\t':
            b.append("\\t");
            break;
          default:
            if (c < 32) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else b.append(c);
        }
      return b.append('"').toString();
    }
    if (value instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) value;
      List<String> keys = new ArrayList<>();
      for (Object key : map.keySet()) keys.add((String) key);
      Collections.sort(keys);
      List<String> parts = new ArrayList<>();
      for (String key : keys) parts.add(stringify(key) + ":" + stringify(map.get(key)));
      return "{" + String.join(",", parts) + "}";
    }
    if (value instanceof List) {
      List<String> parts = new ArrayList<>();
      for (Object v : (List<?>) value) parts.add(stringify(v));
      return "[" + String.join(",", parts) + "]";
    }
    if (value instanceof Boolean || value instanceof Number) return value.toString();
    throw new IllegalArgumentException("Unsupported JSON value " + value.getClass());
  }
}
