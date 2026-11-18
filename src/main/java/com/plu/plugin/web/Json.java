package com.plu.plugin.web;

import java.util.Collection;
import java.util.Map;

/** Minimal JSON serializer (no external deps). Supports String, Number, Boolean, Map, Collection, arrays. */
public final class Json {

    private Json() {}

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
