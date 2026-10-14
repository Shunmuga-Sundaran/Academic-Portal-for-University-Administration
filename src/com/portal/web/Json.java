package com.portal.web;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.Map;

/** Tiny JSON writer so the project needs no external library. Handles Map, lists, records, numbers, dates. */
public final class Json {

    private Json() { }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    private static void append(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            sb.append(Double.isFinite(d) ? v.toString() : "null");
        } else if (v instanceof BigDecimal d) {
            sb.append(d.toPlainString());
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v);
        } else if (v instanceof CharSequence || v instanceof Enum<?> || v instanceof Temporal) {
            quote(sb, v.toString());
        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                append(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> items) {
            sb.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                append(sb, item);
            }
            sb.append(']');
        } else if (v instanceof Record) {
            sb.append('{');
            RecordComponent[] components = v.getClass().getRecordComponents();
            for (int i = 0; i < components.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                quote(sb, components[i].getName());
                sb.append(':');
                try {
                    append(sb, components[i].getAccessor().invoke(v));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot convert " + v.getClass().getSimpleName() + " to JSON", e);
                }
            }
            sb.append('}');
        } else {
            quote(sb, v.toString());
        }
    }

    private static void quote(StringBuilder sb, String s) {
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
