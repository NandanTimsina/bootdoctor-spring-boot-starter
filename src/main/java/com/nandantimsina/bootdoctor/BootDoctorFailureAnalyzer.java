package com.nandantimsina.bootdoctor;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class BootDoctorFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

    private final BootDoctorSettings settings;
    private final Diagnoser diagnoser;

    public BootDoctorFailureAnalyzer(Environment environment) {
        BootDoctorSettings s = new BootDoctorSettings(environment);
        this.settings = s;
        this.diagnoser = new Diagnoser(s, () -> new GroqClient(
                s.getBaseUrl(), s.getApiKey(), s.getModel(), s.getTimeoutMs()));
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        try {
            if (!settings.isEnabled()) {
                return null;
            }
            diagnoser.diagnose(rootFailure);
        } catch (Throwable ignored) {
            // never let our own problem make the failing app fail worse
        }
        return null;
    }
}