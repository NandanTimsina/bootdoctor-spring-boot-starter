package com.nandantimsina.bootdoctor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolePrinterTest {

    @Test
    void wrapKeepsLinesShortAndIndentsContinuation() {
        String text = "one two three four five six seven eight nine ten eleven twelve";
        String wrapped = ConsolePrinter.wrap("Label: ", text, 30);
        for (String line : wrapped.split("\\R")) {
            assertThat(line.length()).isLessThanOrEqualTo(30);
        }
        assertThat(wrapped.split("\\R")[1]).startsWith("       ");
    }

    @Test
    void formatDiagnosisShowsAllFields() {
        ResponseParser.Diagnosis d = new ResponseParser.Diagnosis(
                "the cause", "the fix", "high", List.of("a log line"), true);
        String out = ConsolePrinter.formatDiagnosis(d, "some-model", 2400);
        assertThat(out).contains("the cause", "the fix", "high", "a log line", "some-model", "2.4 s");
    }
}