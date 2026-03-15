package com.lab.routing;

/**
 * Rota para um BIN específico.
 *
 * @param binPrefix   Prefixo do BIN (6 ou 8 dígitos)
 * @param issuerName  Nome do emissor (ex: "Itaú", "Bradesco")
 * @param network     Bandeira (ex: "VISA", "MASTERCARD", "ELO")
 * @param onUs        true se é on-us (mesma instituição)
 * @param muxName     Nome do QMUX destino para envio
 * @param fallbackMux QMUX alternativo se o principal estiver down
 */
public record Route(
    String binPrefix,
    String issuerName,
    String network,
    boolean onUs,
    String muxName,
    String fallbackMux
) {
    public boolean isOnUs() {
        return onUs;
    }
}
