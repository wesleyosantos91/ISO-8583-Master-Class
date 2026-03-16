package com.lab.iso;

/**
 * Validacao e mascaramento de PAN.
 *
 * Referencia: Semana 9 (Luhn) e Semana 18 (mascaramento PCI).
 */
public class PANUtils {

    private PANUtils() { }

    /**
     * Algoritmo de Luhn — valida digito verificador do PAN.
     * @return true se o PAN e valido
     */
    public static boolean luhnCheck(String pan) {
        if (pan == null || pan.isEmpty()) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = pan.charAt(i) - '0';
            if (n < 0 || n > 9) {
                return false;
            }
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * Mascara o PAN para log seguro (PCI compliant).
     * Mostra first 6 + last 4, mascara o resto.
     * Ex: "4532015112830366" -> "453201******0366"
     */
    public static String mask(String pan) {
        if (pan == null || pan.length() < 13) {
            throw new IllegalArgumentException("PAN invalido: tamanho minimo 13 digitos");
        }
        int maskLen = pan.length() - 10; // 6 first + 4 last = 10 visible
        return pan.substring(0, 6) + "*".repeat(maskLen) + pan.substring(pan.length() - 4);
    }

    /**
     * Extrai BIN (8 digitos) do PAN.
     */
    public static String extractBIN(String pan) {
        if (pan == null || pan.length() < 8) {
            throw new IllegalArgumentException("PAN invalido");
        }
        return pan.substring(0, 8);
    }

    /**
     * Extrai BIN legado (6 digitos) do PAN.
     */
    public static String extractBINLegacy(String pan) {
        if (pan == null || pan.length() < 6) {
            throw new IllegalArgumentException("PAN invalido");
        }
        return pan.substring(0, 6);
    }
}
