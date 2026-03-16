package com.lab.iso;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BcdUtilsTest {

    @Test
    void shouldConvertStringToBcd() {
        assertThat(BcdUtils.stringToBcd("1234")).isEqualTo(new byte[]{0x12, 0x34});
        assertThat(BcdUtils.stringToBcd("0000")).isEqualTo(new byte[]{0x00, 0x00});
        assertThat(BcdUtils.stringToBcd("9999")).isEqualTo(new byte[]{(byte) 0x99, (byte) 0x99});
    }

    @Test
    void shouldHandleOddLengthWithPadding() {
        assertThat(BcdUtils.stringToBcd("123")).isEqualTo(new byte[]{0x01, 0x23});
    }

    @Test
    void shouldConvertBcdToString() {
        assertThat(BcdUtils.bcdToString(new byte[]{0x12, 0x34})).isEqualTo("1234");
        assertThat(BcdUtils.bcdToString(new byte[]{0x01, 0x23})).isEqualTo("0123");
    }

    @Test
    void shouldConvertBcdToStringTrimmed() {
        assertThat(BcdUtils.bcdToStringTrimmed(new byte[]{0x01, 0x23})).isEqualTo("123");
        assertThat(BcdUtils.bcdToStringTrimmed(new byte[]{0x00, 0x05})).isEqualTo("5");
    }

    @Test
    void shouldRoundtrip() {
        String original = "123456";
        assertThat(BcdUtils.bcdToString(BcdUtils.stringToBcd(original))).isEqualTo(original);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> BcdUtils.stringToBcd(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BcdUtils.bcdToString(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
