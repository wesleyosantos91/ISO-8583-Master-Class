package com.lab.reconciliation;

/**
 * Engine de reconciliação auth vs clearing (Semana 23).
 *
 * Compara transações autorizadas com registros de clearing para detectar:
 * - Transações autorizadas sem clearing (missing capture)
 * - Clearing sem autorização (phantom transactions)
 * - Mismatches de valor (amount differences)
 * - Mismatches de moeda
 *
 * EXERCÍCIO SEMANA 20: Implementar reconciliação básica (auth vs clearing).
 * EXERCÍCIO SEMANA 23: Implementar detecção de mismatches e relatório de exceções.
 */
public class ReconciliationEngine {

    /**
     * Executa reconciliação entre arquivo de autorizações e arquivo de clearing.
     * @param authFilePath caminho do arquivo de autorizações
     * @param clearingFilePath caminho do arquivo de clearing
     */
    public void reconcile(String authFilePath, String clearingFilePath) {
        // TODO Semana 23:
        // 1. Carregar autorizações (por STAN + Terminal + Data)
        // 2. Carregar clearing records
        // 3. Match por chave composta
        // 4. Identificar: matched, auth-only, clearing-only, mismatched
        // 5. Gerar relatório de exceções

        throw new UnsupportedOperationException("Implementar na Semana 23");
    }
}
