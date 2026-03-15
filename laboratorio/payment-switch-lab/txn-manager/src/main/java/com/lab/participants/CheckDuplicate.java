package com.lab.participants;

import org.jpos.transaction.TransactionParticipant;
import java.io.Serializable;

/**
 * Verifica se a mensagem é duplicata.
 * Usa cache com TTL para deduplicação por chave composta.
 *
 * EXERCÍCIO SEMANA 16: Implementar com Caffeine cache.
 * Chave sugerida: STAN + Terminal ID + Amount + PAN(last4) + Processing Code
 */
public class CheckDuplicate implements TransactionParticipant {

    // TODO Semana 16:
    // 1. Montar chave de deduplicação
    // 2. Verificar no cache se já existe
    // 3. Se duplicata → colocar response cacheada no context, retornar ABORTED
    // 4. Se nova → retornar PREPARED
    // 5. No commit → armazenar response no cache

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("Implementar na Semana 16");
    }

    @Override public void commit(long id, Serializable context) {
        // TODO: Armazenar response no cache para dedup futura
    }

    @Override public void abort(long id, Serializable context) { }
}
