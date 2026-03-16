package com.lab.participants;

/**
 * Constantes para chaves do Context compartilhado entre participants.
 *
 * Centraliza todas as chaves usadas no TransactionManager Context
 * para evitar erros de digitação e garantir consistência.
 *
 * Uso: context.put(ContextKeys.RESPONSE, isoMsg);
 */
public final class ContextKeys {

    private ContextKeys() { }

    /** ISOMsg original recebido do adquirente */
    public static final String REQUEST = "REQUEST";

    /** ISOMsg de resposta (do emissor ou montada internamente) */
    public static final String RESPONSE = "RESPONSE";

    /** Código de resposta (DE39) — usado quando não há ISOMsg de response */
    public static final String RESPONSE_CODE = "RESPONSE_CODE";

    /** ISOSource para enviar a resposta de volta ao adquirente */
    public static final String SOURCE = "SOURCE";

    /** Nome do QMUX destino selecionado pelo RouteByBIN */
    public static final String DESTINATION_MUX = "DESTINATION_MUX";

    /** Nome do QMUX fallback caso o destino principal esteja down */
    public static final String FALLBACK_MUX = "FALLBACK_MUX";

    /** Boolean — true se a transação é on-us (mesma instituição) */
    public static final String IS_ON_US = "IS_ON_US";

    /** Bandeira da transação (VISA, MASTERCARD, ELO, HIPER) */
    public static final String NETWORK = "NETWORK";

    /** Boolean — true se a transação precisa de auto-reversal (timeout do emissor) */
    public static final String NEEDS_REVERSAL = "NEEDS_REVERSAL";

    /** Boolean — true se a transação é duplicata detectada pelo CheckDuplicate */
    public static final String IS_DUPLICATE = "IS_DUPLICATE";

    /** Timestamp (long) de início do processamento — para cálculo de latência */
    public static final String START_TIME = "START_TIME";

    /** Route selecionada pelo RouteByBIN */
    public static final String ROUTE = "ROUTE";
}
