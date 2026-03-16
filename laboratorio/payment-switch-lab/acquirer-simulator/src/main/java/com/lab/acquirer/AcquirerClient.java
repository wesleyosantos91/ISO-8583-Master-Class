package com.lab.acquirer;

/**
 * Cliente adquirente que gera transações de teste para o switch.
 *
 * Modos de operação:
 * - --echo     : Envia 0800 (echo test) e espera 0810
 * - --purchase : Envia 0200 (compra) com --amount e --pan
 * - --reversal : Envia 0400 (reversal) com --stan referenciando transação original
 * - --load     : Modo de carga — envia N transações/segundo
 *
 * EXERCÍCIO SEMANA 10: Implementar envio de 0200 e recepção de 0210.
 * EXERCÍCIO SEMANA 15: Implementar envio de 0800 (echo/sign-on).
 * EXERCÍCIO SEMANA 33: Implementar modo de carga com RateLimiter.
 */
public class AcquirerClient {

    public static void main(String[] args) {
        // TODO Semana 10: Parsear argumentos e executar modo selecionado
        // 1. Conectar ao switch via NACChannel (host:8583)
        // 2. Montar ISOMsg conforme modo
        // 3. Enviar e aguardar resposta
        // 4. Imprimir resultado formatado

        System.out.println("acquirer-simulator — use: --echo | --purchase | --reversal | --load");
        System.out.println("Implementar na Semana 10");
    }
}
