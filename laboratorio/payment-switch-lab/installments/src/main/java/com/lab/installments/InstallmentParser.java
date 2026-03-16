package com.lab.installments;

/**
 * Parser e logica de parcelamento (Semana 11).
 *
 * Tipos de parcelamento no Brasil:
 * - Lojista (merchant installments) -> emissor recebe a vista, adquirente parcela
 * - Emissor (issuer installments) -> emissor parcela na fatura do portador
 *
 * Convencao DE48 usada neste lab:
 * - Formato: "TTNN" onde TT = tipo (03=lojista, 05=emissor), NN = numero de parcelas
 * - Exemplos: "0303" = 3x lojista, "0512" = 12x emissor
 * - Ausencia de DE48 ou valor "0301" = a vista
 *
 * Referencia: Semana 11 — Roteamento e Parcelamento
 */
public class InstallmentParser {

    /** Codigo de parcelamento lojista no DE48 */
    public static final String TYPE_MERCHANT = "03";
    /** Codigo de parcelamento emissor no DE48 */
    public static final String TYPE_ISSUER = "05";

    private InstallmentParser() { }

    /**
     * Extrai o numero de parcelas de DE48.
     * @param de48 conteudo do Data Element 48
     * @return numero de parcelas (1 = a vista)
     */
    public static int parseInstallmentCount(String de48) {
        if (de48 == null || de48.length() < 4) {
            return 1; // a vista
        }
        try {
            int count = Integer.parseInt(de48.substring(2, 4));
            return count > 0 ? count : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Determina se e parcelamento lojista ou emissor.
     * @param de48 conteudo do Data Element 48
     * @return "MERCHANT", "ISSUER" ou "CASH" (a vista)
     */
    public static String parseInstallmentType(String de48) {
        if (de48 == null || de48.length() < 2) {
            return "CASH";
        }
        String typeCode = de48.substring(0, 2);
        return switch (typeCode) {
            case TYPE_MERCHANT -> "MERCHANT";
            case TYPE_ISSUER -> "ISSUER";
            default -> "CASH";
        };
    }

    /**
     * Verifica se a transacao e parcelada.
     * @param de48 conteudo do Data Element 48
     * @return true se parcelado (mais de 1 parcela)
     */
    public static boolean isInstallment(String de48) {
        return parseInstallmentCount(de48) > 1;
    }
}
