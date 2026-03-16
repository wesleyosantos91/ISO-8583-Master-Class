package com.lab.observability;

/**
 * Métricas centralizadas do switch (Semana 21).
 *
 * Métricas que devem ser registradas:
 * - txn_total (counter) — por MTI, response code, rota (on-us/off-us)
 * - txn_duration_ms (histogram) — latência por MTI e rota
 * - txn_active (gauge) — transações em processamento simultâneo
 * - duplicate_count (counter) — transações duplicatas detectadas
 * - timeout_count (counter) — timeouts por emissor/MUX
 * - reversal_count (counter) — reversals gerados automaticamente
 *
 * EXERCÍCIO SEMANA 21: Implementar com Micrometer + Prometheus exporter.
 */
public class SwitchMetrics {

    // TODO Semana 21:
    // 1. Inicializar MeterRegistry (PrometheusMeterRegistry)
    // 2. Criar counters, timers, gauges
    // 3. Expor endpoint HTTP /metrics para Prometheus scrape
    // 4. Integrar com AuditLog e ForwardToIssuer

    /**
     * Registra uma transação completada.
     * @param mti MTI da mensagem
     * @param responseCode DE39 do response
     * @param route rota utilizada (on-us, off-us, nome do mux)
     * @param durationMs latência total em milissegundos
     */
    public void recordTransaction(String mti, String responseCode, String route, long durationMs) {
        // TODO Semana 21: Implementar
        throw new UnsupportedOperationException("Implementar na Semana 21");
    }

    /**
     * Registra um timeout de emissor.
     * @param muxName nome do QMUX que deu timeout
     */
    public void recordTimeout(String muxName) {
        // TODO Semana 21: Implementar
        throw new UnsupportedOperationException("Implementar na Semana 21");
    }
}
