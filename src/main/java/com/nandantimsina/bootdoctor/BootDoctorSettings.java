package com.nandantimsina.bootdoctor;

import org.springframework.core.env.Environment;

public final class BootDoctorSettings {

    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "openai/gpt-oss-20b";
    private static final int DEFAULT_TIMEOUT_MS = 8000;

    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutMs;
    private final boolean dryRun;

    public BootDoctorSettings(Environment env) {
        this.enabled = env.getProperty("bootdoctor.enabled", Boolean.class, true);
        this.apiKey = firstNonBlank(
                env.getProperty("bootdoctor.api-key"),
                env.getProperty("GROQ_API_KEY"));
        this.baseUrl = env.getProperty("bootdoctor.base-url", DEFAULT_BASE_URL);
        this.model = env.getProperty("bootdoctor.model", DEFAULT_MODEL);
        this.timeoutMs = env.getProperty("bootdoctor.timeout-ms", Integer.class, DEFAULT_TIMEOUT_MS);
        this.dryRun = env.getProperty("bootdoctor.dry-run", Boolean.class, false);
    }

    public boolean isEnabled() { return enabled; }
    public String getApiKey() { return apiKey; }
    public boolean hasApiKey() { return apiKey != null; }
    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }
    public int getTimeoutMs() { return timeoutMs; }
    public boolean isDryRun() { return dryRun; }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    @Override
    public String toString() {
        return "BootDoctorSettings{enabled=" + enabled
                + ", apiKey=" + (hasApiKey() ? "***set***" : "not set")
                + ", baseUrl=" + baseUrl
                + ", model=" + model
                + ", timeoutMs=" + timeoutMs
                + ", dryRun=" + dryRun + "}";
    }
}