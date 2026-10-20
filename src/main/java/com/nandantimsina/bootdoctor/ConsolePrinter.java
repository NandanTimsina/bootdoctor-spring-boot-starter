package com.nandantimsina.bootdoctor;

public final class ConsolePrinter {

    private ConsolePrinter() {
    }

    public static void print(String title, String body) {
        String line = "=".repeat(70);
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(" ").append(title).append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        sb.append(body).append(System.lineSeparator());
        sb.append(line).append(System.lineSeparator());
        System.err.print(sb);
    }
}