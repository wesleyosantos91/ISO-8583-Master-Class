package com.lab.participants;

import org.jpos.transaction.TransactionParticipant;
import java.io.Serializable;

/**
 * Roteamento por BIN: decide on-us vs off-us e seleciona o QMUX destino.
 *
 * EXERCÍCIO SEMANA 11: Implementar com BINTable.
 */
public class RouteByBIN implements TransactionParticipant {

    // TODO Semana 11:
    // 1. Extrair PAN do context
    // 2. Extrair BIN (8 dígitos, fallback 6)
    // 3. Consultar BINTable
    // 4. Se não encontrar → ABORTED com DE39=92
    // 5. Colocar no context: IS_ON_US, DESTINATION_MUX, NETWORK

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("Implementar na Semana 11");
    }

    @Override public void commit(long id, Serializable context) { }
    @Override public void abort(long id, Serializable context) { }
}
