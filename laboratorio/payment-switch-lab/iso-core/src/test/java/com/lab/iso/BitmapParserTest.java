package com.lab.iso;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class BitmapParserTest {

    @Test
    void shouldParsePrimaryBitmap() {
        // Bitmap: 0x80 00 00 00 00 00 00 00 = bit 1 set (secondary bitmap indicator)
        byte[] bitmap = HexUtils.hexToBytes("8000000000000000");
        List<Integer> fields = BitmapParser.parse(bitmap);
        assertThat(fields).containsExactly(1);
    }

    @Test
    void shouldParseMultipleFields() {
        // Bitmap: 0xF0 = 11110000 -> bits 1,2,3,4
        byte[] bitmap = HexUtils.hexToBytes("F000000000000000");
        List<Integer> fields = BitmapParser.parse(bitmap);
        assertThat(fields).containsExactly(1, 2, 3, 4);
    }

    @Test
    void shouldParseHexBitmap() {
        // Typical auth bitmap: DE 2,3,4,7,11,22,41,42,49
        // Hex: 7234050128C08000
        List<Integer> fields = BitmapParser.parseHex("7234050128C08000");
        assertThat(fields).contains(2, 3, 4, 7, 11, 22, 41, 42, 49);
    }

    @Test
    void shouldDetectSecondaryBitmap() {
        byte[] withSecondary = HexUtils.hexToBytes("8000000000000000");
        byte[] withoutSecondary = HexUtils.hexToBytes("7000000000000000");
        assertThat(BitmapParser.hasSecondaryBitmap(withSecondary)).isTrue();
        assertThat(BitmapParser.hasSecondaryBitmap(withoutSecondary)).isFalse();
    }

    @Test
    void shouldCheckFieldPresence() {
        byte[] bitmap = HexUtils.hexToBytes("F000000000000000");
        assertThat(BitmapParser.isFieldPresent(bitmap, 1)).isTrue();
        assertThat(BitmapParser.isFieldPresent(bitmap, 5)).isFalse();
    }

    @Test
    void shouldRejectInvalidBitmapSize() {
        assertThatThrownBy(() -> BitmapParser.parse(new byte[]{0x00}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
