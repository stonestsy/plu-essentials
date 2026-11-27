package com.plu.plugin.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON serializer/parser (no external deps). Supports String, Number, Boolean, Map, Collection, arrays. */
public final class Json {

    private Json() {}

    /** Parses JSON into Map / List / String / Long / Double / Boolean / null. */
    public static Object parse(String text) {
        return new Parser(text).parse();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s;
            if (!this.s.isEmpty() && this.s.charAt(0) == '\uFEFF') this.i = 1;
        }

        Object parse() {
            skip();
            Object value = value();
            skip();
            return value;
        }

        private Object value() {
            skip();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> { expect("true"); yield Boolean.TRUE; }
                case 'f' -> { expect("false"); yield Boolean.FALSE; }
                case 'n' -> { expect("null"); yield null; }
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skip();
            if (peek() == '}') { i++; return map; }
            while (i < s.length()) {
                skip();
                String key = string();
                skip();
                if (peek() == ':') i++;
                Object val = value();
                map.put(key, val);
                skip();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; break; }
                break;
            }
            return map;
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skip();
            if (peek() == ']') { i++; return list; }
            while (i < s.length()) {
                list.add(value());
                skip();
                char c = peek();
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; break; }
                break;
            }
            return list;
        }

        private String string() {
            skip();
            if (peek() != '"') return "";
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\' && i < s.length()) {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (i + 4 <= s.length()) {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                            }
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object number() {
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') i++;
                else break;
            }
            String raw = s.substring(start, i);
            if (raw.isEmpty()) return null;
            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) return Double.parseDouble(raw);
                return Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }

        private void expect(String word) {
            if (s.startsWith(word, i)) i += word.length();
        }

        private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }

    public static String stringify(Object obj) {
        StringBuilder sb = new StringBuilder();
        write(sb, obj);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object obj) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String s) {
            writeString(sb, s);
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
        } else if (obj instanceof Map<?, ?> map) {
            writeMap(sb, map);
        } else if (obj instanceof Collection<?> col) {
            writeArray(sb, col);
        } else if (obj.getClass().isArray()) {
            writeArrayObj(sb, (Object[]) obj);
        } else {
            writeString(sb, obj.toString());
        }
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            write(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Collection<?> col) {
        sb.append('[');
        boolean first = true;
        for (Object o : col) {
            if (!first) sb.append(',');
            first = false;
            write(sb, o);
        }
        sb.append(']');
    }

    private static void writeArrayObj(StringBuilder sb, Object[] arr) {
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            write(sb, arr[i]);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
