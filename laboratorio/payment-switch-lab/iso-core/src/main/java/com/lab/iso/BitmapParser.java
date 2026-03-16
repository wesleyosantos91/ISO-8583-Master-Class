package com.lab.iso;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser de bitmap ISO 8583.
 *
 * O bitmap indica quais Data Elements estao presentes na mensagem.
 * - Primary Bitmap (8 bytes / 64 bits): DE 1 a DE 64
 * - Secondary Bitmap (8 bytes adicionais): DE 65 a DE 128 (presente quando bit 1 = 1)
 *
 * Referencia: Semana 2 — Anatomia ISO 8583
 */
public class BitmapParser {

    private BitmapParser() { }

    /**
     * Dado um bitmap em bytes (8 ou 16 bytes), retorna lista de DEs presentes.
     *
     * @param bitmap array de 8 bytes (primary) ou 16 bytes (primary + secondary)
     * @return lista ordenada de numeros de DE presentes
     */
    public static List<Integer> parse(byte[] bitmap) {
        if (bitmap == null || (bitmap.length != 8 && bitmap.length != 16)) {
            throw new IllegalArgumentException("Bitmap deve ter 8 ou 16 bytes, recebeu: " +
                    (bitmap == null ? "null" : bitmap.length));
        }
        List<Integer> fields = new ArrayList<>();
        for (int i = 0; i < bitmap.length * 8; i++) {
            int byteIndex = i / 8;
            int bitIndex = 7 - (i % 8);
            if (((bitmap[byteIndex] >> bitIndex) & 1) == 1) {
                fields.add(i + 1); // DEs sao 1-indexed
            }
        }
        return fields;
    }

    /**
     * Dado um bitmap em hexadecimal (16 ou 32 chars), retorna lista de DEs presentes.
     *
     * @param hexBitmap string hexadecimal representando o bitmap
     * @return lista ordenada de numeros de DE presentes
     */
    public static List<Integer> parseHex(String hexBitmap) {
        if (hexBitmap == null) {
            throw new IllegalArgumentException("hexBitmap nao pode ser null");
        }
        return parse(HexUtils.hexToBytes(hexBitmap));
    }

    /**
     * Verifica se um bitmap contem secondary bitmap (bit 1 = 1).
     */
    public static boolean hasSecondaryBitmap(byte[] bitmap) {
        if (bitmap == null || bitmap.length < 1) {
            return false;
        }
        return (bitmap[0] & 0x80) != 0;
    }

    /**
     * Verifica se um DE especifico esta presente no bitmap.
     *
     * @param bitmap bytes do bitmap
     * @param de numero do Data Element (1-128)
     * @return true se o DE esta presente
     */
    public static boolean isFieldPresent(byte[] bitmap, int de) {
        if (de < 1 || de > bitmap.length * 8) {
            return false;
        }
        int index = de - 1;
        int byteIndex = index / 8;
        int bitIndex = 7 - (index % 8);
        return ((bitmap[byteIndex] >> bitIndex) & 1) == 1;
    }
}
