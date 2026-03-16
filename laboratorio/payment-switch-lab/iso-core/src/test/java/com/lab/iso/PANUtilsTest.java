package com.lab.iso;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PANUtilsTest {

    @Test
    void shouldValidateLuhn() {
        // PANs validos (Luhn check)
        assertThat(PANUtils.luhnCheck("4532015112830366")).isTrue();
        assertThat(PANUtils.luhnCheck("5425233430109903")).isTrue();
        assertThat(PANUtils.luhnCheck("4111111111111111")).isTrue();

        // PANs invalidos
        assertThat(PANUtils.luhnCheck("4532015112830367")).isFalse();
        assertThat(PANUtils.luhnCheck("1234567890123456")).isFalse();
    }

    @Test
    void shouldRejectInvalidInputForLuhn() {
        assertThat(PANUtils.luhnCheck(null)).isFalse();
        assertThat(PANUtils.luhnCheck("")).isFalse();
        assertThat(PANUtils.luhnCheck("ABCDEF")).isFalse();
    }

    @Test
    void shouldMaskPAN() {
        assertThat(PANUtils.mask("4532015112830366")).isEqualTo("453201******0366");
        assertThat(PANUtils.mask("5425233430109903")).isEqualTo("542523******9903");
        assertThat(PANUtils.mask("4111111111111111")).isEqualTo("411111******1111");
    }

    @Test
    void shouldRejectShortPANForMask() {
        assertThatThrownBy(() -> PANUtils.mask("12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExtractBIN8() {
        assertThat(PANUtils.extractBIN("4532015112830366")).isEqualTo("45320151");
    }

    @Test
    void shouldExtractBIN6() {
        assertThat(PANUtils.extractBINLegacy("4532015112830366")).isEqualTo("453201");
    }
}
