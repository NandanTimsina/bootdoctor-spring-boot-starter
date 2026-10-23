package com.nandantimsina.bootdoctor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCollectorTest {

    @Test
    void listsTheChainAndNamesTheRootWithoutMarkingTheLine() {
        Throwable failure = new RuntimeException("outer", new IllegalStateException("inner msg"));
        String out = ContextCollector.collect(failure);
        assertThat(out).contains("1. java.lang.RuntimeException: outer");
        assertThat(out).contains("2. java.lang.IllegalStateException: inner msg");
        assertThat(out).doesNotContain("<--");
        assertThat(out).contains("last item in the chain is the root cause");
    }

    @Test
    void circularCauseChainDoesNotLoopForever() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b");
        a.initCause(b);
        b.initCause(a);
        assertThat(ContextCollector.collect(a)).contains("a").contains("b");
    }

    @Test
    void missingMessageIsShownAsPlaceholder() {
        assertThat(ContextCollector.collect(new RuntimeException()))
                .contains("(no message)");
    }

    @Test
    void veryLongChainKeepsTheRootAndSaysHowManyWereOmitted() {
        Throwable t = new RuntimeException("level0");
        for (int i = 1; i < 20; i++) {
            t = new RuntimeException("level" + i, t);
        }
        String out = ContextCollector.collect(t);
        assertThat(out).contains("level0").contains("8 more causes omitted");
    }

    @Test
    void veryLongMessageIsCut() {
        String out = ContextCollector.collect(new RuntimeException("x".repeat(2000)));
        assertThat(out).contains("...[truncated]");
        assertThat(out.length()).isLessThan(ContextCollector.MAX_TOTAL_CHARS + 50);
    }
}