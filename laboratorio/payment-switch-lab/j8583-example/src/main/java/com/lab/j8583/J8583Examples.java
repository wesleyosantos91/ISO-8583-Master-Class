package com.lab.j8583;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;

import java.io.IOException;
import java.text.ParseException;

/**
 * Exemplos práticos de pack/unpack com j8583.
 *
 * <h2>Comparação j8583 vs jPOS</h2>
 * <pre>
 * ┌────────────────────┬──────────────────────────────┬──────────────────────────────┐
 * │ Operação           │ jPOS                         │ j8583                        │
 * ├────────────────────┼──────────────────────────────┼──────────────────────────────┤
 * │ Criar mensagem     │ new ISOMsg()                 │ mf.newMessage(0x0200)        │
 * │ Setar MTI          │ msg.setMTI("0200")           │ (definido no newMessage)     │
 * │ Setar campo        │ msg.set(2, "4111...")         │ msg.setValue(2, "4111...",    │
 * │                    │                              │   IsoType.LLVAR, 0)          │
 * │ Ler campo          │ msg.getString(2)             │ msg.getObjectValue(2)        │
 * │ Pack (serializar)  │ msg.pack()                   │ msg.writeData()              │
 * │ Unpack             │ msg.unpack(bytes)            │ mf.parseMessage(bytes, 0)    │
 * │ Config             │ GenericPackager XML           │ ConfigParser XML ou código   │
 * │ Licença            │ AGPL 3.0                     │ Apache 2.0                   │
 * └────────────────────┴──────────────────────────────┴──────────────────────────────┘
 * </pre>
 */
public final class J8583Examples {

    private J8583Examples() {}

    // ─── Autorização (0200) ───────────────────────────────────────────

    /**
     * Cria uma mensagem 0200 de autorização de compra.
     *
     * <p>Equivalente jPOS:</p>
     * <pre>
     *   ISOMsg msg = new ISOMsg();
     *   msg.setPackager(packager);
     *   msg.setMTI("0200");
     *   msg.set(2, pan);
     *   msg.set(3, "003000");
     *   msg.set(4, amount);
     *   msg.set(14, "2712");
     *   msg.set(22, "051");
     *   msg.set(41, terminalId);
     *   msg.set(42, merchantId);
     *   msg.set(49, "986");
     *   byte[] packed = msg.pack();
     * </pre>
     */
    public static IsoMessage buildAuthorization(MessageFactory<IsoMessage> mf,
                                                 String pan,
                                                 String amount,
                                                 String terminalId,
                                                 String merchantId) {
        // newMessage já aplica o template (campo 3=003000, campo 49=986)
        IsoMessage msg = mf.newMessage(0x0200);

        msg.setValue(2,  pan,        IsoType.LLVAR,   19);   // PAN
        msg.setValue(4,  amount,     IsoType.AMOUNT,  12);   // Valor
        msg.setValue(14, "2712",     IsoType.DATE_EXP, 4);   // Validade
        msg.setValue(22, "051",      IsoType.NUMERIC,  3);   // POS Entry Mode (chip)
        msg.setValue(25, "00",       IsoType.NUMERIC,  2);   // POS Condition Code
        msg.setValue(41, terminalId, IsoType.ALPHA,    8);   // Terminal ID
        msg.setValue(42, merchantId, IsoType.ALPHA,   15);   // Merchant ID
        msg.setValue(43, "LOJA EXEMPLO          SAO PAULO    BR",
                     IsoType.ALPHA, 40);                      // Merchant Name

        return msg;
    }

    /**
     * Serializa mensagem para bytes ASCII (pack).
     */
    public static byte[] pack(IsoMessage msg) {
        return msg.writeData();
    }

    /**
     * Desserializa bytes de volta para IsoMessage (unpack).
     */
    public static IsoMessage unpack(MessageFactory<IsoMessage> mf, byte[] data)
            throws ParseException, IOException {
        return mf.parseMessage(data, 0);
    }

    // ─── Response (0210) ──────────────────────────────────────────────

    /**
     * Cria response 0210 a partir de um request 0200.
     *
     * <p>Em j8583, {@code createResponse} copia os campos relevantes
     * automaticamente e muda o MTI de 0200 → 0210.</p>
     *
     * <p>Equivalente jPOS:</p>
     * <pre>
     *   ISOMsg resp = (ISOMsg) request.clone();
     *   resp.setResponseMTI();
     *   resp.set(38, authCode);
     *   resp.set(39, "00");
     * </pre>
     */
    public static IsoMessage buildResponse(IsoMessage request,
                                            String authCode,
                                            String responseCode) {
        IsoMessage resp = request.createResponse();
        resp.setValue(38, authCode,     IsoType.ALPHA, 6);
        resp.setValue(39, responseCode, IsoType.ALPHA, 2);
        return resp;
    }

    // ─── Network Management (0800) ───────────────────────────────────

    /**
     * Cria echo-test (0800 com campo 70 = 301).
     */
    public static IsoMessage buildEchoTest(MessageFactory<IsoMessage> mf) {
        IsoMessage msg = mf.newMessage(0x0800);
        msg.setValue(70, "301", IsoType.NUMERIC, 3);
        return msg;
    }

    // ─── Reversal (0400) ─────────────────────────────────────────────

    /**
     * Cria reversal 0400 a partir de uma autorização original.
     */
    public static IsoMessage buildReversal(MessageFactory<IsoMessage> mf,
                                            String pan,
                                            String amount,
                                            String terminalId,
                                            String merchantId,
                                            String originalStan) {
        IsoMessage msg = mf.newMessage(0x0400);

        msg.setValue(2,  pan,          IsoType.LLVAR,   19);
        msg.setValue(3,  "003000",     IsoType.NUMERIC,  6);
        msg.setValue(4,  amount,       IsoType.AMOUNT,  12);
        msg.setValue(22, "051",        IsoType.NUMERIC,  3);
        msg.setValue(25, "00",         IsoType.NUMERIC,  2);
        msg.setValue(41, terminalId,   IsoType.ALPHA,    8);
        msg.setValue(42, merchantId,   IsoType.ALPHA,   15);
        msg.setValue(49, "986",        IsoType.NUMERIC,  3);

        return msg;
    }

    // ─── Utilitários ─────────────────────────────────────────────────

    /**
     * Imprime mensagem legível no console (para debug).
     */
    public static String dump(IsoMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("MTI: ").append(String.format("0x%04X", msg.getType())).append('\n');

        for (int i = 2; i <= 128; i++) {
            if (msg.hasField(i)) {
                sb.append(String.format("  F%-3d [%-10s] = %s%n",
                        i,
                        msg.getField(i).getType(),
                        msg.getObjectValue(i)));
            }
        }
        return sb.toString();
    }
}
