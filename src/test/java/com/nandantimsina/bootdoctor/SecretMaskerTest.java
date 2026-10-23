package com.nandantimsina.bootdoctor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecretMaskerTest {

    @Test
    void masksPasswordProperty() {
        String out = SecretMasker.mask("spring.datasource.password=devpassword");
        assertThat(out).doesNotContain("devpassword");
        assertThat(out).contains("spring.datasource.password=");
    }

    @Test
    void masksPasswordInsideJdbcUrl() {
        String out = SecretMasker.mask("failed: jdbc:postgresql://admin:s3cret@localhost:5432/db");
        assertThat(out).doesNotContain("s3cret");
        assertThat(out).contains("localhost:5432");
    }

    @Test
    void masksAuthorizationHeader() {
        String out = SecretMasker.mask("Authorization: Bearer abcdefghijklmnop12345");
        assertThat(out).doesNotContain("abcdefghijklmnop12345");
    }

    @Test
    void masksBareBearerToken() {
        String out = SecretMasker.mask("header was Bearer abcdefghijklmnop12345 here");
        assertThat(out).doesNotContain("abcdefghijklmnop12345");
    }

    @Test
    void masksGroqKeyFormat() {
        String out = SecretMasker.mask("the key gsk_abcdefghijklmnopqrstuvwxyz123456 leaked");
        assertThat(out).doesNotContain("gsk_abcdefghijklmnopqrstuvwxyz123456");
    }

    @Test
    void masksKnownSecretAnywhere() {
        String out = SecretMasker.mask("connected with abcd1234xyz ok", List.of("abcd1234xyz"));
        assertThat(out).doesNotContain("abcd1234xyz");
    }

    @Test
    void leavesNormalCrashTextUntouched() {
        String text = "Error creating bean with name 'authTokenFilter': Unsatisfied dependency "
                + "expressed through field 'jwtService'. Could not resolve placeholder 'JWT_SECRET' "
                + "in value \"${JWT_SECRET}\" <-- \"${spring.app.jwtSecretKey}\"";
        assertThat(SecretMasker.mask(text)).isEqualTo(text);
    }

    @Test
    void nullBecomesEmptyString() {
        assertThat(SecretMasker.mask(null)).isEmpty();
    }
}