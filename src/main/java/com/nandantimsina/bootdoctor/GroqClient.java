package com.nandantimsina.bootdoctor;

import org.springframework.boot.json.JsonParserFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class GroqClient implements LlmClient {

    private final HttpClient http;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public GroqClient(String baseUrl, String apiKey, String model, int timeoutMs) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.endpoint = base + "/chat/completions";
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String complete(PromptBuilder.Prompt prompt) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(model, prompt)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmException("Groq returned HTTP " + response.statusCode()
                        + ": " + shorten(response.body(), 200));
            }
            return extractAnswer(response.body());
        } catch (IOException e) {
            throw new LlmException("Could not reach Groq (" + e.getClass().getSimpleName() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting for Groq", e);
        }
    }

    static String buildRequestBody(String model, PromptBuilder.Prompt prompt) {
        return "{"
                + "\"model\":" + quote(model) + ","
                + "\"temperature\":0.2,"
                + "\"response_format\":{\"type\":\"json_object\"},"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + quote(prompt.system()) + "},"
                + "{\"role\":\"user\",\"content\":" + quote(prompt.user()) + "}"
                + "]}";
    }

    @SuppressWarnings("unchecked")
    static String extractAnswer(String responseBody) {
        try {
            Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(responseBody);
            List<Object> choices = (List<Object>) root.get("choices");
            Map<String, Object> first = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            Object content = message.get("content");
            if (content == null || content.toString().isBlank()) {
                throw new LlmException("Groq returned an empty answer");
            }
            return content.toString();
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmException("Could not read Groq's response", e);
        }
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
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
        return sb.append('"').toString();
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "...";
    }
}