package com.nandantimsina.bootdoctor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseParserTest {

    private static final String GOOD = """
            {"cause":"JWT_SECRET is not set","fix":"Set the JWT_SECRET environment variable","confidence":"high","evidence":["Could not resolve placeholder 'JWT_SECRET'"]}""";

    @Test
    void parsesGoodJson() {
        ResponseParser.Diagnosis d = ResponseParser.parse(GOOD);
        assertThat(d.structured()).isTrue();
        assertThat(d.cause()).isEqualTo("JWT_SECRET is not set");
        assertThat(d.fix()).contains("JWT_SECRET");
        assertThat(d.confidence()).isEqualTo("high");
        assertThat(d.evidence()).hasSize(1);
    }

    @Test
    void parsesJsonInsideMarkdownFence() {
        ResponseParser.Diagnosis d = ResponseParser.parse("```json\n" + GOOD + "\n```");
        assertThat(d.structured()).isTrue();
        assertThat(d.cause()).isEqualTo("JWT_SECRET is not set");
    }

    @Test
    void parsesJsonWithTextAround() {
        ResponseParser.Diagnosis d = ResponseParser.parse("Here is the answer: " + GOOD + " Hope that helps.");
        assertThat(d.structured()).isTrue();
    }

    @Test
    void brokenJsonFallsBackToRawText() {
        ResponseParser.Diagnosis d = ResponseParser.parse("{\"cause\": \"oops\", \"fix\": ");
        assertThat(d.structured()).isFalse();
        assertThat(d.cause()).contains("oops");
    }

    @Test
    void plainTextFallsBackToRawText() {
        ResponseParser.Diagnosis d = ResponseParser.parse("I think the secret is missing");
        assertThat(d.structured()).isFalse();
        assertThat(d.cause()).contains("secret is missing");
    }

    @Test
    void missingCauseFallsBack() {
        ResponseParser.Diagnosis d = ResponseParser.parse("{\"fix\":\"x\",\"confidence\":\"high\"}");
        assertThat(d.structured()).isFalse();
    }

    @Test
    void unknownConfidenceBecomesUnknown() {
        ResponseParser.Diagnosis d = ResponseParser.parse(
                "{\"cause\":\"c\",\"fix\":\"f\",\"confidence\":\"certain\"}");
        assertThat(d.structured()).isTrue();
        assertThat(d.confidence()).isEqualTo("unknown");
    }

    @Test
    void evidenceIsLimitedToThreeItems() {
        ResponseParser.Diagnosis d = ResponseParser.parse(
                "{\"cause\":\"c\",\"fix\":\"f\",\"confidence\":\"low\",\"evidence\":[\"1\",\"2\",\"3\",\"4\",\"5\"]}");
        assertThat(d.evidence()).hasSize(3);
    }

    @Test
    void nullAndBlankDoNotCrash() {
        assertThat(ResponseParser.parse(null).structured()).isFalse();
        assertThat(ResponseParser.parse("   ").structured()).isFalse();
    }

    @Test
    void veryLongCauseIsCut() {
        String json = "{\"cause\":\"" + "a".repeat(2000) + "\"}";
        ResponseParser.Diagnosis d = ResponseParser.parse(json);
        assertThat(d.cause().length()).isLessThanOrEqualTo(603);
    }
}