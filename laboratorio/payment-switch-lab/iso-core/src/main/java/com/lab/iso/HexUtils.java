package com.lab.iso;

/**
 * Utilitarios para manipulacao hexadecimal.
 *
 * Referencia: Semana 3 — Encoding e Formatos
 */
public class HexUtils {

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private HexUtils() { }

    /**
     * Converte array de bytes para string hexadecimal.
     * Ex: {0x4A, 0x3B} -> "4A3B"
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes nao pode ser null");
        }
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

    /**
     * Converte string hexadecimal para array de bytes.
     * Ex: "4A3B" -> {0x4A, 0x3B}
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex nao pode ser null");
        }
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("String hex deve ter tamanho par: " + hex.length());
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Caractere hex invalido na posicao " + (i * 2));
            }
            bytes[i] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }

    /**
     * Converte bytes para representacao binaria legivel.
     * Ex: {0xF2} -> "11110010"
     */
    public static String toBinaryString(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes nao pode ser null");
        }
        StringBuilder sb = new StringBuilder(bytes.length * 8);
        for (byte b : bytes) {
            for (int bit = 7; bit >= 0; bit--) {
                sb.append((b >> bit) & 1);
            }
        }
        return sb.toString();
    }
}
