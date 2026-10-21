package com.nandantimsina.bootdoctor;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class BootDoctorFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

    private final Environment environment;

    public BootDoctorFailureAnalyzer(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        try {
            if (!isEnabled()) {
                return null;
            }
            Throwable root = rootCause(cause);
            String summary = root.getClass().getName() + ": " + root.getMessage();
            ConsolePrinter.print("BootDoctor diagnosis (placeholder)",
                    "Root cause: " + summary);
        } catch (Throwable ignored) {
            // never let our own problem make the failing app fail worse
        }
        return null;
    }

    private boolean isEnabled() {
        return environment.getProperty("bootdoctor.enabled", Boolean.class, true);
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}