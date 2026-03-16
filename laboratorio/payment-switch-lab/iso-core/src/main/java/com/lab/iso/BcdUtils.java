package com.lab.iso;

/**
 * Utilitarios para Binary Coded Decimal (BCD).
 *
 * BCD codifica 2 digitos decimais por byte, compactando campos numericos.
 * Muito usado em ISO 8583 para campos como PAN, Amount, STAN.
 *
 * Referencia: Semana 3 — Encoding e Formatos
 */
public class BcdUtils {

    private BcdUtils() { }

    /**
     * Converte string de digitos decimais para bytes BCD.
     * Se o numero de digitos for impar, adiciona padding '0' a esquerda.
     * Ex: "1234" -> {0x12, 0x34}
     * Ex: "123"  -> {0x01, 0x23}
     */
    public static byte[] stringToBcd(String digits) {
        if (digits == null) {
            throw new IllegalArgumentException("digits nao pode ser null");
        }
        // Padding para tamanho par
        if (digits.length() % 2 != 0) {
            digits = "0" + digits;
        }
        byte[] bcd = new byte[digits.length() / 2];
        for (int i = 0; i < bcd.length; i++) {
            int hi = Character.digit(digits.charAt(i * 2), 10);
            int lo = Character.digit(digits.charAt(i * 2 + 1), 10);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Caractere nao-decimal encontrado: " + digits);
            }
            bcd[i] = (byte) ((hi << 4) | lo);
        }
        return bcd;
    }

    /**
     * Converte bytes BCD para string de digitos decimais.
     * Ex: {0x12, 0x34} -> "1234"
     */
    public static String bcdToString(byte[] bcd) {
        if (bcd == null) {
            throw new IllegalArgumentException("bcd nao pode ser null");
        }
        StringBuilder sb = new StringBuilder(bcd.length * 2);
        for (byte b : bcd) {
            sb.append((b >> 4) & 0x0F);
            sb.append(b & 0x0F);
        }
        return sb.toString();
    }

    /**
     * Converte bytes BCD para string, removendo padding '0' a esquerda.
     * Ex: {0x01, 0x23} -> "123"
     */
    public static String bcdToStringTrimmed(byte[] bcd) {
        String result = bcdToString(bcd);
        int start = 0;
        while (start < result.length() - 1 && result.charAt(start) == '0') {
            start++;
        }
        return result.substring(start);
    }
}
