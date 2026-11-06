// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Small SCIM 2.0 filter parser/evaluator (RFC 7644 §3.4.2.2) for the in-memory provisioning lists.
 *
 * <p>Supports {@code and}/{@code or}/{@code not}, parentheses, the comparison operators {@code eq ne co sw ew gt ge lt le}
 * and {@code pr}, nested attribute paths ({@code emails.value}) and values applied to lists (any element matches).
 * Complex joins beyond that, or extension-schema attributes, are rejected as {@code invalidFilter}.
 */
final class ScimFilter {

    private static final Pattern TOKEN = Pattern.compile(
            "(\\(|\\))|(?i)\\b(and|or|not|eq|ne|co|sw|ew|pr|gt|ge|lt|le)\\b|\"((?:[^\"\\\\]|\\\\.)*)\"|([^\\s()]+)");

    private ScimFilter() {}

    static Predicate<Map<String, Object>> parse(String filter) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(filter);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2).toLowerCase(Locale.ROOT));
            } else if (matcher.group(3) != null) {
                tokens.add("\"" + matcher.group(3) + "\"");
            } else if (matcher.group(4) != null) {
                tokens.add(matcher.group(4));
            }
        }
        Parser parser = new Parser(tokens);
        Predicate<Map<String, Object>> predicate = parser.parseOr();
        if (parser.hasMore()) {
            throw invalid();
        }
        return predicate;
    }

    private static ScimException invalid() {
        return new ScimException(HttpStatus.BAD_REQUEST, "Unsupported or malformed filter expression", "invalidFilter");
    }

    /** Recursive-descent parser over the token list. */
    private static final class Parser {
        private final List<String> tokens;
        private int index;

        Parser(List<String> tokens) {
            this.tokens = tokens;
        }

        boolean hasMore() {
            return index < tokens.size();
        }

        private String peek() {
            return hasMore() ? tokens.get(index) : null;
        }

        private String next() {
            if (!hasMore()) {
                throw invalid();
            }
            return tokens.get(index++);
        }

        private boolean keywords(String token, String... words) {
            if (token == null) {
                return false;
            }
            for (String word : words) {
                if (token.equalsIgnoreCase(word)) {
                    return true;
                }
            }
            return false;
        }

        Predicate<Map<String, Object>> parseOr() {
            Predicate<Map<String, Object>> left = parseAnd();
            while (keywords(peek(), "or")) {
                next();
                Predicate<Map<String, Object>> right = parseAnd();
                left = left.or(right);
            }
            return left;
        }

        private Predicate<Map<String, Object>> parseAnd() {
            Predicate<Map<String, Object>> left = parseUnary();
            while (keywords(peek(), "and")) {
                next();
                Predicate<Map<String, Object>> right = parseUnary();
                left = left.and(right);
            }
            return left;
        }

        private Predicate<Map<String, Object>> parseUnary() {
            if ("(".equals(peek())) {
                next();
                Predicate<Map<String, Object>> inner = parseOr();
                if (!")".equals(peek())) {
                    throw invalid();
                }
                next();
                return inner;
            }
            if (keywords(peek(), "not")) {
                next();
                return parseUnary().negate();
            }
            return parseExpression();
        }

        private Predicate<Map<String, Object>> parseExpression() {
            String attribute = next();
            if (keywords(attribute, "and", "or", "not", "eq", "ne", "co", "sw", "ew", "pr", "gt", "ge", "lt", "le")) {
                throw invalid();
            }
            String operator = next().toLowerCase(Locale.ROOT);
            if ("pr".equals(operator)) {
                return resource -> !resolve(resource, attribute).isEmpty();
            }
            if (!List.of("eq", "ne", "co", "sw", "ew", "gt", "ge", "lt", "le").contains(operator)) {
                throw invalid();
            }
            String raw = next();
            return resource -> compare(resolve(resource, attribute), operator, raw);
        }
    }

    private static List<Object> resolve(Map<String, Object> resource, String path) {
        List<Object> current = new ArrayList<>();
        current.add(resource);
        for (String part : path.split("\\.")) {
            List<Object> next = new ArrayList<>();
            for (Object value : current) {
                collect(value, part, next);
            }
            current = next;
        }
        return current;
    }

    private static void collect(Object value, String part, List<Object> out) {
        if (value instanceof Map<?, ?> map) {
            Object found = map.get(part);
            if (found instanceof Collection<?> collection) {
                out.addAll(collection);
            } else if (found != null) {
                out.add(found);
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collect(item, part, out);
            }
        }
    }

    private static boolean compare(List<Object> values, String operator, String raw) {
        String parsed = raw;
        boolean quotedFlag = false;
        if (parsed.length() >= 2 && parsed.startsWith("\"") && parsed.endsWith("\"")) {
            parsed = parsed.substring(1, parsed.length() - 1);
            quotedFlag = true;
        }
        final String expected = parsed;
        final boolean quoted = quotedFlag;
        switch (operator) {
            case "eq":
                return values.stream().anyMatch(value -> equalsValue(value, expected, quoted));
            case "ne":
                return values.stream().noneMatch(value -> equalsValue(value, expected, quoted));
            case "co":
                return values.stream()
                        .anyMatch(value ->
                                stringOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)));
            case "sw":
                return values.stream()
                        .anyMatch(value ->
                                stringOf(value).toLowerCase(Locale.ROOT).startsWith(expected.toLowerCase(Locale.ROOT)));
            case "ew":
                return values.stream()
                        .anyMatch(value ->
                                stringOf(value).toLowerCase(Locale.ROOT).endsWith(expected.toLowerCase(Locale.ROOT)));
            case "gt":
                return values.stream().anyMatch(value -> numeric(value, expected) > 0);
            case "ge":
                return values.stream().anyMatch(value -> numeric(value, expected) >= 0);
            case "lt":
                return values.stream().anyMatch(value -> numeric(value, expected) < 0);
            case "le":
                return values.stream().anyMatch(value -> numeric(value, expected) <= 0);
            default:
                throw invalid();
        }
    }

    private static boolean equalsValue(Object value, String expected, boolean quoted) {
        if (value instanceof Boolean || value instanceof Number) {
            if (value instanceof Boolean && !quoted) {
                return value.equals(Boolean.parseBoolean(expected));
            }
            try {
                return Double.compare(Double.parseDouble(String.valueOf(value)), Double.parseDouble(expected)) == 0;
            } catch (NumberFormatException exception) {
                return stringOf(value).equalsIgnoreCase(expected);
            }
        }
        return stringOf(value).equalsIgnoreCase(expected);
    }

    private static int numeric(Object value, String expected) {
        try {
            return Double.compare(Double.parseDouble(String.valueOf(value)), Double.parseDouble(expected));
        } catch (NumberFormatException exception) {
            return stringOf(value).compareToIgnoreCase(expected);
        }
    }

    private static String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
