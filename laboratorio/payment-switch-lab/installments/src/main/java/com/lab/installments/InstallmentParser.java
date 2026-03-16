package com.lab.installments;

/**
 * Parser e lógica de parcelamento (Semana 11).
 *
 * Tipos de parcelamento no Brasil:
 * - Lojista (merchant installments) → emissor recebe à vista, adquirente parcela
 * - Emissor (issuer installments) → emissor parcela na fatura do portador
 *
 * Campos ISO envolvidos:
 * - DE3  (Processing Code) → identifica compra a crédito parcelado
 * - DE48 (Additional Data) → número de parcelas e tipo
 *
 * EXERCÍCIO SEMANA 11: Implementar parsing de DE48 para parcelamento.
 */
public class InstallmentParser {

    /**
     * Extrai o número de parcelas de DE48.
     * @param de48 conteúdo do Data Element 48
     * @return número de parcelas (1 = à vista)
     */
    public static int parseInstallmentCount(String de48) {
        // TODO Semana 11: Implementar parsing
        throw new UnsupportedOperationException("Implementar na Semana 11");
    }

    /**
     * Determina se é parcelamento lojista ou emissor.
     * @param de48 conteúdo do Data Element 48
     * @return "MERCHANT" ou "ISSUER"
     */
    public static String parseInstallmentType(String de48) {
        // TODO Semana 11: Implementar parsing
        throw new UnsupportedOperationException("Implementar na Semana 11");
    }
}
