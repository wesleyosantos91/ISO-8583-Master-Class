package com.lab.participants;

import org.jpos.transaction.TransactionParticipant;
import java.io.Serializable;

/**
 * Encaminha a mensagem para o emissor via QMUX e aguarda resposta.
 *
 * EXERCÍCIO SEMANA 10: Implementar com QMUX.
 */
public class ForwardToIssuer implements TransactionParticipant {

    private long timeout = 30_000; // 30 segundos

    // TODO Semana 10:
    // 1. Ler DESTINATION_MUX do context
    // 2. Enviar request via QMUX
    // 3. Se response != null → colocar no context, PREPARED
    // 4. Se response == null (timeout) → NEEDS_REVERSAL=true, ABORTED

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("Implementar na Semana 10");
    }

    @Override public void commit(long id, Serializable context) { }
    @Override public void abort(long id, Serializable context) {
        // TODO Semana 13: Se NEEDS_REVERSAL, disparar auto-reversal
    }
}
