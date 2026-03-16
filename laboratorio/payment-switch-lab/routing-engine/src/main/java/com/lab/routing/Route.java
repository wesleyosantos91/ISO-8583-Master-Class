package com.lab.routing;

import java.util.Objects;

/**
 * Rota para um BIN específico.
 *
 * @param binPrefix   Prefixo do BIN (6 ou 8 dígitos)
 * @param issuerName  Nome do emissor (ex: "Itau", "Bradesco")
 * @param network     Bandeira (ex: "VISA", "MASTERCARD", "ELO")
 * @param onUs        true se é on-us (mesma instituição)
 * @param muxName     Nome do QMUX destino para envio
 * @param fallbackMux QMUX alternativo se o principal estiver down (pode ser null)
 */
public record Route(
    String binPrefix,
    String issuerName,
    String network,
    boolean onUs,
    String muxName,
    String fallbackMux
) {
    public Route {
        Objects.requireNonNull(binPrefix, "binPrefix não pode ser null");
        Objects.requireNonNull(issuerName, "issuerName não pode ser null");
        Objects.requireNonNull(network, "network não pode ser null");
        Objects.requireNonNull(muxName, "muxName não pode ser null");
    }

    public boolean isOnUs() {
        return onUs;
    }
}
