package com.nandantimsina.bootdoctor;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class BootDoctorFailureAnalyzer extends AbstractFailureAnalyzer<Throwable>
        implements EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        try {
            if (!isEnabled()) {
                return null;
            }
            String summary = cause.getClass().getName() + ": " + cause.getMessage();
            ConsolePrinter.print("BootDoctor diagnosis (placeholder)",
                    "Failure seen: " + summary);
        } catch (Throwable ignored) {
            // never let our own problem make the failing app fail worse
        }
        return null;
    }

    private boolean isEnabled() {
        if (environment == null) {
            return true;
        }
        return environment.getProperty("bootdoctor.enabled", Boolean.class, true);
    }
}