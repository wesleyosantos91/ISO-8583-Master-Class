package com.lab.participants;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;

/**
 * Valida formato, campos obrigatórios e regras básicas.
 *
 * EXERCÍCIO SEMANA 7/9:
 *   Semana 7 — Estrutura básica (prepare/commit/abort)
 *   Semana 9 — Validações por campo (Luhn, amount, POS Entry Mode)
 */
public class ValidateMessage implements TransactionParticipant {

    private static final int[] REQUIRED_AUTH = {2, 3, 4, 7, 11, 22, 41, 42, 49};
    private static final int[] REQUIRED_REVERSAL = {2, 3, 4, 7, 11, 41, 90};
    private static final int[] REQUIRED_ECHO = {7, 11, 70};

    @Override
    public int prepare(long id, Serializable context) {
        // TODO Semana 7:
        // 1. Extrair ISOMsg do context
        // 2. Verificar MTI é conhecido
        // 3. Verificar campos obrigatórios por MTI
        // 4. Retornar PREPARED ou ABORTED (com RESPONSE_CODE no context)

        // TODO Semana 9 (expandir):
        // 5. Validar PAN com Luhn (DE2)
        // 6. Validar Amount > 0 (DE4)
        // 7. Validar POS Entry Mode conhecido (DE22)
        // 8. Validar Processing Code conhecido (DE3)

        throw new UnsupportedOperationException("Implementar");
    }

    @Override
    public void commit(long id, Serializable context) { }

    @Override
    public void abort(long id, Serializable context) { }
}
