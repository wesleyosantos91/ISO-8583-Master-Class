package com.lab.simulator;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/**
 * Simulador de emissor para testes de integração.
 *
 * Responde a mensagens ISO 8583 com regras configuráveis:
 * - PAN *0000 → DE39=00 (aprovado)
 * - PAN *5100 → DE39=51 (saldo insuficiente)
 * - PAN *5400 → DE39=54 (cartão expirado)
 * - PAN *1400 → DE39=14 (PAN inválido)
 * - PAN *0500 → DE39=05 (não honrar)
 * - Timeout configurável para simular emissor lento
 *
 * EXERCÍCIO SEMANA 10: Implementar o ISORequestListener.
 * EXERCÍCIO SEMANA 12: Adicionar simulação de timeout.
 * EXERCÍCIO SEMANA 13: Responder reversals (0400 → 0410).
 */
public class IssuerSimulator implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg msg) {
        // TODO Semana 10:
        // 1. Ler MTI e PAN
        // 2. Determinar response code pela regra do PAN (últimos 4 dígitos)
        // 3. Montar response (setResponseMTI, copiar campos, setar DE39)
        // 4. Gerar DE38 (authorization code) se aprovado
        // 5. Enviar via source.send(response)

        throw new UnsupportedOperationException("Implementar na Semana 10");
    }
}
