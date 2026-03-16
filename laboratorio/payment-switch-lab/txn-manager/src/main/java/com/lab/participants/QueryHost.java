package com.lab.participants;

import org.jpos.transaction.GroupSelector;

import java.io.Serializable;

/**
 * Primeiro participant da pipeline.
 * Extrai a request do Space, coloca no Context e decide qual grupo executar.
 *
 * EXERCÍCIO SEMANA 7: Implemente o select() para rotear por MTI.
 */
public class QueryHost implements GroupSelector {

    @Override
    public String select(long id, Serializable context) {
        // TODO Semana 7: Ler o MTI e retornar o grupo correto
        // "0100", "0200" → "authorization"
        // "0400"         → "reversal"
        // "0800"         → "network-mgmt"
        // default        → null (pula groups)
        throw new UnsupportedOperationException("Implementar na Semana 7");
    }

    @Override
    public int prepare(long id, Serializable context) {
        // TODO Semana 7: Extrair ISOMsg do context e validar que existe
        // 1. Obter ISOMsg do context
        // 2. Se null → ABORTED com RESPONSE_CODE=20 (invalid response)
        // 3. Se ok → gravar START_TIME no context, retornar PREPARED
        throw new UnsupportedOperationException("Implementar na Semana 7");
    }

    @Override
    public void commit(long id, Serializable context) { }

    @Override
    public void abort(long id, Serializable context) { }
}
