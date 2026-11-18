package com.plu.plugin.util;

/**
 * 宽松类型转换：兼容数据库/YAML 的返回差异。
 * 例如 MySQL Connector/J 对 TINYINT(1) 默认返回 Boolean，
 * YAML 配置可能返回 Integer/String，直接强转 Number 会抛 ClassCastException。
 */
public final class Values {

    private Values() {}

    public static int asInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        if (o instanceof CharSequence s) {
            try { return Integer.parseInt(s.toString().trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    public static long asLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof Boolean b) return b ? 1L : 0L;
        if (o instanceof CharSequence s) {
            try { return Long.parseLong(s.toString().trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    public static double asDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof CharSequence s) {
            try { return Double.parseDouble(s.toString().trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    public static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        if (o instanceof CharSequence s) {
            String v = s.toString().trim();
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        }
        return def;
    }
}
