package com.nemouw.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void normalizesAnEmailByTrimmingAndLowercasing() {
        assertThat(TextNormalizer.normalizeEmail("  Amira.Haddad@ClinicMail.org \n")).isEqualTo("amira.haddad@clinicmail.org");
    }

    @Test
    void leavesAnAlreadyNormalEmailUnchanged() {
        assertThat(TextNormalizer.normalizeEmail("layla.khoury@mail.example.com")).isEqualTo("layla.khoury@mail.example.com");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "amira.haddad@clinicmail.org | amira.haddad",
            "layla+news@mail.example.com | layla+news",
            "omar@a@b.org                | omar",
            "@clinicmail.org             | NULL",
            "no-at-sign                  | NULL",
            "NULL                        | NULL"
    })
    void extractsTheLocalPartBeforeTheFirstAtSignOrNothingWhenThereIsNone(String email, String expected) {
        assertThat(TextNormalizer.emailLocalPart(email)).isEqualTo(expected);
    }

    @Test
    void composesADecomposedLetterWithNfc() {
        assertThat(TextNormalizer.nfc("cafe\u0301")).isEqualTo("caf\u00E9");
    }

    @Test
    void leavesFullWidthLettersAloneWithNfcButFoldsThemWithNfkc() {
        assertThat(TextNormalizer.nfc("\uFF21\uFF22\uFF23")).isEqualTo("\uFF21\uFF22\uFF23");
        assertThat(TextNormalizer.nfkc("\uFF21\uFF22\uFF23")).isEqualTo("ABC");
    }

    @Test
    void expandsALigatureWithNfkc() {
        assertThat(TextNormalizer.nfkc("\uFB01nance")).isEqualTo("finance");
    }
}
