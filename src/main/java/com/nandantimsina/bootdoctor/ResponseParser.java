package com.nandantimsina.bootdoctor;

import org.springframework.boot.json.JsonParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResponseParser {

    private static final int MAX_CAUSE = 600;
    private static final int MAX_FIX = 800;
    private static final int MAX_EVIDENCE_ITEM = 300;
    private static final int MAX_EVIDENCE = 3;
    private static final int MAX_RAW = 800;
    private static final Set<String> CONFIDENCES = Set.of("high", "medium", "low");

    private ResponseParser() {
    }

    public record Diagnosis(String cause, String fix, String confidence,
                            List<String> evidence, boolean structured) {
    }

    public static Diagnosis parse(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            return rawFallback("(empty answer)");
        }
        try {
            String json = extractJsonObject(modelText);
            if (json != null) {
                Map<String, Object> map = JsonParserFactory.getJsonParser().parseMap(json);
                String cause = text(map.get("cause"));
                if (cause != null) {
                    String fix = text(map.get("fix"));
                    return new Diagnosis(
                            limit(cause, MAX_CAUSE),
                            limit(fix != null ? fix : "(no fix suggested)", MAX_FIX),
                            normaliseConfidence(text(map.get("confidence"))),
                            evidenceOf(map.get("evidence")),
                            true);
                }
            }
        } catch (RuntimeException ignored) {
            // bad JSON: fall through to the raw fallback
        }
        return rawFallback(modelText);
    }

    private static Diagnosis rawFallback(String raw) {
        return new Diagnosis(limit(raw, MAX_RAW),
                "(the model did not return the expected format)",
                "unknown", List.of(), false);
    }

    // takes the text from the first { to the last }, which drops ```json fences and chatter
    private static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private static String text(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return s.strip();
        }
        return null;
    }

    private static String normaliseConfidence(String value) {
        if (value == null) {
            return "unknown";
        }
        String lower = value.toLowerCase();
        return CONFIDENCES.contains(lower) ? lower : "unknown";
    }

    private static List<String> evidenceOf(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String s = text(item);
                if (s != null) {
                    result.add(limit(s, MAX_EVIDENCE_ITEM));
                }
                if (result.size() == MAX_EVIDENCE) {
                    break;
                }
            }
        }
        return result;
    }

    private static String limit(String s, int max) {
        String stripped = s.strip();
        return stripped.length() <= max ? stripped : stripped.substring(0, max) + "...";
    }
}