package com.nandantimsina.bootdoctor;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class BootDoctorFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

    private final BootDoctorSettings settings;

    public BootDoctorFailureAnalyzer(Environment environment) {
        this.settings = new BootDoctorSettings(environment);
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        try {
            if (!settings.isEnabled()) {
                return null;
            }
            String context = ContextCollector.collect(rootFailure);
            ConsolePrinter.print("BootDoctor diagnosis (placeholder)", context);
        } catch (Throwable ignored) {
            // never let our own problem make the failing app fail worse
        }
        return null;
    }
}