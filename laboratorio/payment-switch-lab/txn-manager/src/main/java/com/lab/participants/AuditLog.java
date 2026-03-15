package com.lab.participants;

import org.jpos.transaction.TransactionParticipant;
import java.io.Serializable;

/**
 * Registra cada transação para auditoria.
 * Usa NO_JOIN pois não precisa de commit/abort.
 *
 * EXERCÍCIO SEMANA 7: Implementar log estruturado.
 * EXERCÍCIO SEMANA 18: Garantir mascaramento PCI.
 * EXERCÍCIO SEMANA 21: Adicionar métricas.
 */
public class AuditLog implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        // TODO Semana 7:
        // Log: MTI, STAN, PAN (mascarado!), Amount, ResponseCode, Route, Latency
        //
        // Formato sugerido:
        // txn.completed mti=0200 stan=123456 pan=4532****0366 amt=000000015000
        //               rc=00 route=ON_US latency_ms=145 duplicate=false

        // TODO Semana 21: Registrar métricas (SwitchMetrics)

        return NO_JOIN; // Não precisa de commit/abort
    }

    @Override public void commit(long id, Serializable context) { }
    @Override public void abort(long id, Serializable context) { }
}
