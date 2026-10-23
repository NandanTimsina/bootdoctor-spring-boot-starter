package com.nandantimsina.bootdoctor;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class SecretMasker {

    private static final String MASK = "***";

    // user:password@host inside any URL, for example jdbc:postgresql://admin:pass@localhost/db
    private static final Pattern URL_USERINFO = Pattern.compile(
            "(?i)([a-z][a-z0-9+.\\-]*://[^\\s:/@]+:)([^\\s@/]+)(@)");

    // password=abc, "token": "abc", spring.datasource.password=abc, jwtSecretKey=abc
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)((?:password|passwd|pwd|secret[-_.]?key|secret|access[-_.]?token|token"
                    + "|api[-_.]?key|access[-_.]?key|private[-_.]?key|credentials?)"
                    + "[\"']?\\s*[=:]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,;&)}\\]]+)");

    // Authorization: anything until the end of the line
    private static final Pattern AUTH_HEADER = Pattern.compile(
            "(?i)(authorization[\"']?\\s*[=:]\\s*)[^\\r\\n]+");

    // Bearer eyJhbGciOi...
    private static final Pattern BEARER = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=\\-]{8,}");

    // keys that have a recognisable shape
    private static final List<Pattern> TOKEN_FORMATS = List.of(
            Pattern.compile("\\bgsk_[A-Za-z0-9]{20,}"),
            Pattern.compile("\\bsk-[A-Za-z0-9_\\-]{20,}"),
            Pattern.compile("\\b(?:AKIA|ASIA)[0-9A-Z]{16}\\b"),
            Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{30,}"),
            Pattern.compile("\\beyJ[A-Za-z0-9_\\-]{5,}\\.[A-Za-z0-9_\\-]{5,}\\.[A-Za-z0-9_\\-]{5,}"));

    private SecretMasker() {
    }

    public static String mask(String text) {
        return mask(text, List.of());
    }

    public static String mask(String text, Collection<String> knownSecrets) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = URL_USERINFO.matcher(result).replaceAll("$1" + MASK + "$3");
        result = KEY_VALUE.matcher(result).replaceAll("$1" + MASK);
        result = AUTH_HEADER.matcher(result).replaceAll("$1" + MASK);
        result = BEARER.matcher(result).replaceAll("Bearer " + MASK);
        for (Pattern format : TOKEN_FORMATS) {
            result = format.matcher(result).replaceAll(MASK);
        }
        if (knownSecrets != null) {
            for (String secret : knownSecrets) {
                if (secret != null && secret.length() >= 4) {
                    result = result.replace(secret, MASK);
                }
            }
        }
        return result;
    }
}