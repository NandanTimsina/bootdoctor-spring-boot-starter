package com.nandantimsina.bootdoctor;

import java.util.List;
import java.util.function.Supplier;

public final class Diagnoser {

    private final BootDoctorSettings settings;
    private final Supplier<LlmClient> clientFactory;

    public Diagnoser(BootDoctorSettings settings, Supplier<LlmClient> clientFactory) {
        this.settings = settings;
        this.clientFactory = clientFactory;
    }

    public void diagnose(Throwable failure) {
        List<String> secrets = settings.hasApiKey() ? List.of(settings.getApiKey()) : List.of();
        String safeReport = SecretMasker.mask(ContextCollector.collect(failure), secrets);
        PromptBuilder.Prompt prompt = PromptBuilder.build(safeReport);

        if (settings.isDryRun()) {
            ConsolePrinter.print("BootDoctor dry run: nothing was sent", dryRunText(prompt));
            return;
        }
        if (!settings.hasApiKey()) {
            ConsolePrinter.print("BootDoctor", noKeyText(failure, secrets));
            return;
        }

        System.err.println("BootDoctor: asking " + settings.getModel()
                + " to explain this failure (waiting up to " + settings.getTimeoutMs() / 1000.0 + " s)...");
        long start = System.nanoTime();
        try {
            String answer = clientFactory.get().complete(prompt);
            ResponseParser.Diagnosis diagnosis = ResponseParser.parse(answer);
            long millis = (System.nanoTime() - start) / 1_000_000;
            ConsolePrinter.print("BootDoctor diagnosis",
                    ConsolePrinter.formatDiagnosis(diagnosis, settings.getModel(), millis));
        } catch (RuntimeException e) {
            String reason = (e instanceof LlmException)
                    ? e.getMessage()
                    : "unexpected error (" + e.getClass().getSimpleName() + ")";
            ConsolePrinter.print("BootDoctor",
                    "Could not get an AI explanation: " + SecretMasker.mask(reason, secrets)
                            + System.lineSeparator() + rootCauseLine(failure, secrets));
        }
    }

    private static String dryRunText(PromptBuilder.Prompt prompt) {
        String nl = System.lineSeparator();
        return "SYSTEM MESSAGE:" + nl + prompt.system() + nl + nl
                + "USER MESSAGE:" + nl + prompt.user();
    }

    private static String noKeyText(Throwable failure, List<String> secrets) {
        String nl = System.lineSeparator();
        return "No API key is set, so no AI explanation was requested." + nl
                + "Set GROQ_API_KEY (or bootdoctor.api-key) to enable it," + nl
                + "or bootdoctor.dry-run=true to preview what would be sent." + nl
                + rootCauseLine(failure, secrets);
    }

    private static String rootCauseLine(Throwable failure, List<String> secrets) {
        Throwable root = failure;
        for (int i = 0; i < 50 && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        String message = SecretMasker.mask(root.getMessage(), secrets)
                .replace('\n', ' ').replace('\r', ' ');
        if (message.length() > 300) {
            message = message.substring(0, 300) + "...";
        }
        return "Root cause: " + root.getClass().getName() + ": " + message;
    }
}