package com.lab.iso;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HexUtilsTest {

    @Test
    void shouldConvertBytesToHex() {
        assertThat(HexUtils.bytesToHex(new byte[]{0x4A, 0x3B})).isEqualTo("4A3B");
        assertThat(HexUtils.bytesToHex(new byte[]{(byte) 0xFF, 0x00})).isEqualTo("FF00");
        assertThat(HexUtils.bytesToHex(new byte[]{})).isEqualTo("");
    }

    @Test
    void shouldConvertHexToBytes() {
        assertThat(HexUtils.hexToBytes("4A3B")).isEqualTo(new byte[]{0x4A, 0x3B});
        assertThat(HexUtils.hexToBytes("FF00")).isEqualTo(new byte[]{(byte) 0xFF, 0x00});
        assertThat(HexUtils.hexToBytes("")).isEqualTo(new byte[]{});
    }

    @Test
    void shouldRejectOddLengthHex() {
        assertThatThrownBy(() -> HexUtils.hexToBytes("ABC"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldConvertToBinaryString() {
        assertThat(HexUtils.toBinaryString(new byte[]{(byte) 0xF2})).isEqualTo("11110010");
        assertThat(HexUtils.toBinaryString(new byte[]{0x00})).isEqualTo("00000000");
        assertThat(HexUtils.toBinaryString(new byte[]{(byte) 0xFF})).isEqualTo("11111111");
    }

    @Test
    void shouldRoundtripBytesHexBytes() {
        byte[] original = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
        assertThat(HexUtils.hexToBytes(HexUtils.bytesToHex(original))).isEqualTo(original);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> HexUtils.bytesToHex(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HexUtils.hexToBytes(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HexUtils.toBinaryString(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
