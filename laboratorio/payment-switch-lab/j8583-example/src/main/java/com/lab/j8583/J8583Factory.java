package com.lab.j8583;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

import java.io.IOException;
import java.text.ParseException;

/**
 * Fábrica de mensagens j8583 pré-configurada.
 *
 * <p>Equivalência com jPOS:</p>
 * <pre>
 *   jPOS:  GenericPackager packager = new GenericPackager("iso87ascii.xml");
 *          ISOMsg msg = new ISOMsg();
 *          msg.setPackager(packager);
 *
 *   j8583: MessageFactory&lt;IsoMessage&gt; mf = J8583Factory.create();
 *          IsoMessage msg = mf.newMessage(0x0200);
 * </pre>
 */
public final class J8583Factory {

    private J8583Factory() {}

    /**
     * Cria MessageFactory a partir do j8583-config.xml no classpath.
     */
    public static MessageFactory<IsoMessage> create() throws IOException {
        MessageFactory<IsoMessage> mf = ConfigParser.createFromClasspathConfig("j8583-config.xml");
        mf.setUseBinaryMessages(false);   // ASCII (padrão)
        mf.setAssignDate(true);           // Seta campo 7 automaticamente
        mf.setTraceNumberGenerator(new SequentialTraceGenerator());
        return mf;
    }

    /**
     * Cria MessageFactory sem XML — configuração 100% programática.
     * Útil para entender como j8583 funciona "por baixo".
     */
    public static MessageFactory<IsoMessage> createProgrammatic() {
        MessageFactory<IsoMessage> mf = new MessageFactory<>();
        mf.setUseBinaryMessages(false);
        mf.setAssignDate(true);
        mf.setTraceNumberGenerator(new SequentialTraceGenerator());

        // Definir headers por tipo de mensagem
        mf.addMessageTemplate(buildAuthorizationTemplate(mf));

        return mf;
    }

    private static IsoMessage buildAuthorizationTemplate(MessageFactory<IsoMessage> mf) {
        IsoMessage tpl = new IsoMessage();
        tpl.setType(0x0200);
        tpl.setIsoHeader("ISO");
        // Campo 3 — Processing Code default "003000" (compra)
        tpl.setValue(3, "003000", IsoType.NUMERIC, 6);
        // Campo 49 — Currency Code default "986" (BRL)
        tpl.setValue(49, "986", IsoType.NUMERIC, 3);
        return tpl;
    }

    /**
     * Faz parse de bytes recebidos de volta para IsoMessage.
     */
    public static IsoMessage parse(MessageFactory<IsoMessage> mf, byte[] data)
            throws ParseException, IOException {
        return mf.parseMessage(data, 0);
    }
}
