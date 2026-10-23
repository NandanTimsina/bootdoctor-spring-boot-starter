package com.nandantimsina.bootdoctor;

import org.springframework.boot.SpringBootVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ContextCollector {

    static final int MAX_CHAIN_SHOWN = 12;
    static final int MAX_MESSAGE_CHARS = 300;
    static final int MAX_ROOT_MESSAGE_CHARS = 800;
    static final int MAX_FRAMES = 8;
    static final int MAX_TOTAL_CHARS = 6000;

    private ContextCollector() {
    }

    public static String collect(Throwable failure) {
        StringBuilder sb = new StringBuilder();
        sb.append("Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("Spring Boot version: ").append(SpringBootVersion.getVersion()).append('\n');
        sb.append('\n').append("Exception chain (outermost first):").append('\n');

        List<Throwable> chain = chainOf(failure);
        Throwable root = chain.get(chain.size() - 1);

        List<Throwable> shown = chain;
        int omitted = 0;
        if (chain.size() > MAX_CHAIN_SHOWN) {
            shown = new ArrayList<>(chain.subList(0, MAX_CHAIN_SHOWN - 1));
            shown.add(root);
            omitted = chain.size() - MAX_CHAIN_SHOWN;
        }

        for (int i = 0; i < shown.size(); i++) {
            Throwable t = shown.get(i);
            boolean isRoot = (t == root);
            int limit = isRoot ? MAX_ROOT_MESSAGE_CHARS : MAX_MESSAGE_CHARS;
            if (isRoot && omitted > 0) {
                sb.append("   (").append(omitted).append(" more causes omitted)").append('\n');
            }
            sb.append(i + 1).append(". ")
                    .append(t.getClass().getName()).append(": ")
                    .append(shorten(t.getMessage(), limit))
                    .append('\n');
        }
        sb.append("(The last item in the chain is the root cause.)").append('\n');

        sb.append('\n').append("Top stack frames of the root cause:").append('\n');
        StackTraceElement[] frames = root.getStackTrace();
        for (int i = 0; i < Math.min(frames.length, MAX_FRAMES); i++) {
            sb.append("  at ").append(frames[i]).append('\n');
        }

        String result = sb.toString();
        if (result.length() > MAX_TOTAL_CHARS) {
            result = result.substring(0, MAX_TOTAL_CHARS) + "\n...[truncated]";
        }
        return result;
    }

    private static List<Throwable> chainOf(Throwable failure) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        while (current != null && seen.add(current) && chain.size() < 50) {
            chain.add(current);
            current = current.getCause();
        }
        return chain;
    }

    static String shorten(String text, int max) {
        if (text == null) {
            return "(no message)";
        }
        String oneLine = text.replace('\r', ' ').replace('\n', ' ');
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max) + "...[truncated]";
    }
}