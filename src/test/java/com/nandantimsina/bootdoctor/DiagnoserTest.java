package com.nandantimsina.bootdoctor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnoserTest {

    private static final Throwable FAILURE = new IllegalStateException("startup failed",
            new RuntimeException("Could not resolve placeholder 'JWT_SECRET'"));

    private static final String GOOD_ANSWER =
            "{\"cause\":\"JWT_SECRET is not set\",\"fix\":\"Set JWT_SECRET\","
                    + "\"confidence\":\"high\",\"evidence\":[\"Could not resolve placeholder\"]}";

    private static BootDoctorSettings settings(String... keyValues) {
        MockEnvironment env = new MockEnvironment();
        for (int i = 0; i < keyValues.length; i += 2) {
            env.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return new BootDoctorSettings(env);
    }

    private static String capture(Runnable action) {
        PrintStream original = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void goodAnswerPrintsTheDiagnosis() {
        Diagnoser d = new Diagnoser(settings("bootdoctor.api-key", "k"), () -> prompt -> GOOD_ANSWER);
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("Likely cause").contains("JWT_SECRET is not set").contains("high");
    }

    @Test
    void llmFailureIsContainedAndShowsRootCause() {
        Diagnoser d = new Diagnoser(settings("bootdoctor.api-key", "k"), () -> prompt -> {
            throw new LlmException("Groq returned HTTP 429: rate limit");
        });
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("Could not get an AI explanation").contains("429")
                .contains("Root cause").contains("JWT_SECRET");
    }

    @Test
    void unexpectedExceptionIsAlsoContained() {
        Diagnoser d = new Diagnoser(settings("bootdoctor.api-key", "k"), () -> prompt -> {
            throw new IllegalStateException("boom");
        });
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("Could not get an AI explanation");
    }

    @Test
    void garbageAnswerStillPrints() {
        Diagnoser d = new Diagnoser(settings("bootdoctor.api-key", "k"), () -> prompt -> "lol");
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("did not return the expected format");
    }

    @Test
    void noKeyPrintsHintAndNeverCreatesTheClient() {
        Diagnoser d = new Diagnoser(settings(), () -> {
            throw new AssertionError("client must not be created without a key");
        });
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("No API key is set").contains("Root cause");
    }

    @Test
    void dryRunPrintsThePromptAndSendsNothing() {
        Diagnoser d = new Diagnoser(settings("bootdoctor.api-key", "k", "bootdoctor.dry-run", "true"), () -> {
            throw new AssertionError("dry-run must not call the model");
        });
        String out = capture(() -> d.diagnose(FAILURE));
        assertThat(out).contains("nothing was sent").contains("<failure_report>");
    }

    @Test
    void secretsAreMaskedInTheDryRunPrompt() {
        Throwable leaky = new IllegalStateException(
                "connect failed: jdbc:postgresql://admin:hunter2@localhost/db password=hunter2");
        Diagnoser d = new Diagnoser(settings("bootdoctor.dry-run", "true"), () -> prompt -> GOOD_ANSWER);
        String out = capture(() -> d.diagnose(leaky));
        assertThat(out).doesNotContain("hunter2");
    }
}