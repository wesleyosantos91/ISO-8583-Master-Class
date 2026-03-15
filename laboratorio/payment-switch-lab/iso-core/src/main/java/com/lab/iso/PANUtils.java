package com.lab.iso;

/**
 * Validação e mascaramento de PAN.
 *
 * EXERCÍCIO SEMANA 9/18: Implemente e garanta cobertura de testes.
 */
public class PANUtils {

    /**
     * Algoritmo de Luhn — valida dígito verificador do PAN.
     * @return true se o PAN é válido
     */
    public static boolean luhnCheck(String pan) {
        // TODO: Implementar na Semana 9
        throw new UnsupportedOperationException("Implementar");
    }

    /**
     * Mascara o PAN para log seguro (PCI compliant).
     * Mostra first 6 + last 4, mascara o resto.
     * Ex: "4532015112830366" → "453201****0366"
     */
    public static String mask(String pan) {
        // TODO: Implementar na Semana 18
        throw new UnsupportedOperationException("Implementar");
    }

    /**
     * Extrai BIN (8 dígitos) do PAN.
     */
    public static String extractBIN(String pan) {
        if (pan == null || pan.length() < 8) {
            throw new IllegalArgumentException("PAN inválido");
        }
        return pan.substring(0, 8);
    }

    /**
     * Extrai BIN legado (6 dígitos) do PAN.
     */
    public static String extractBINLegacy(String pan) {
        if (pan == null || pan.length() < 6) {
            throw new IllegalArgumentException("PAN inválido");
        }
        return pan.substring(0, 6);
    }
}
