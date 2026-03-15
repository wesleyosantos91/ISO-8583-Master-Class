package com.lab.participants;

import org.jpos.transaction.TransactionParticipant;
import java.io.Serializable;

/**
 * Monta a response final para enviar de volta ao adquirente.
 *
 * EXERCÍCIO SEMANA 7: Implementar.
 *   - Se RESPONSE existe no context → usar
 *   - Se RESPONSE_CODE existe mas sem RESPONSE → montar resposta de erro
 *   - Sempre copiar campos obrigatórios do request (STAN, Terminal, etc.)
 */
public class BuildResponse implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        // TODO Semana 7:
        // 1. Se já tem RESPONSE no context (veio do emissor) → ajustar e enviar
        // 2. Se não tem RESPONSE mas tem RESPONSE_CODE (erro interno) → montar response
        // 3. Copiar: DE 2, 3, 4, 7, 11, 41, 42, 49 do request
        // 4. Setar MTI response (request.setResponseMTI())
        // 5. Setar DE 39 com o response code
        // 6. Enviar via ISOSource

        throw new UnsupportedOperationException("Implementar na Semana 7");
    }

    @Override public void commit(long id, Serializable context) { }
    @Override public void abort(long id, Serializable context) {
        // Mesmo no abort, precisamos enviar response (com código de erro)
        // TODO: Montar response de erro e enviar
    }
}
