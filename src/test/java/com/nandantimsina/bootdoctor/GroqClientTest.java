package com.nandantimsina.bootdoctor;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroqClientTest {

    private static final PromptBuilder.Prompt PROMPT = PromptBuilder.build("some crash text");

    private static HttpServer startServer(int status, String body, long delayMs) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException ignored) {
                // test helper only
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void returnsTheModelText() throws IOException {
        HttpServer server = startServer(200,
                "{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}", 0);
        try {
            GroqClient client = new GroqClient(baseUrl(server), "test-key", "some-model", 2000);
            assertThat(client.complete(PROMPT)).isEqualTo("hello");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rateLimitBecomesLlmException() throws IOException {
        HttpServer server = startServer(429, "{\"error\":\"rate limit\"}", 0);
        try {
            GroqClient client = new GroqClient(baseUrl(server), "test-key", "some-model", 2000);
            assertThatThrownBy(() -> client.complete(PROMPT))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("429");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void slowServerTimesOut() throws IOException {
        HttpServer server = startServer(200,
                "{\"choices\":[{\"message\":{\"content\":\"late\"}}]}", 2000);
        try {
            GroqClient client = new GroqClient(baseUrl(server), "test-key", "some-model", 300);
            assertThatThrownBy(() -> client.complete(PROMPT)).isInstanceOf(LlmException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void garbageResponseBecomesLlmException() {
        assertThatThrownBy(() -> GroqClient.extractAnswer("not json at all"))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void emptyAnswerBecomesLlmException() {
        assertThatThrownBy(() -> GroqClient.extractAnswer(
                "{\"choices\":[{\"message\":{\"content\":\"\"}}]}"))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void quoteEscapesQuotesAndNewlines() {
        assertThat(GroqClient.quote("a\"b\nc")).isEqualTo("\"a\\\"b\\nc\"");
    }

    @Test
    void requestBodyContainsModelAndJsonMode() {
        String body = GroqClient.buildRequestBody("some-model", PROMPT);
        assertThat(body).contains("\"model\":\"some-model\"");
        assertThat(body).contains("json_object");
    }
}