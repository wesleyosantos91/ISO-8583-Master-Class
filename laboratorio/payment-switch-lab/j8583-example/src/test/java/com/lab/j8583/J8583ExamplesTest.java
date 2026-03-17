package com.lab.j8583;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do módulo j8583-example.
 *
 * <p>Cada teste demonstra uma operação ISO 8583 usando a biblioteca j8583
 * e documenta a equivalência com jPOS nos comentários.</p>
 */
class J8583ExamplesTest {

    private static final String PAN         = "4111111111111111";
    private static final String AMOUNT      = "000000015000";  // R$ 150,00
    private static final String TERMINAL_ID = "TERM0001";
    private static final String MERCHANT_ID = "MERCHANT0000001";

    private MessageFactory<IsoMessage> mf;

    @BeforeEach
    void setUp() throws IOException {
        mf = J8583Factory.create();
    }

    // ─── Autorização ──────────────────────────────────────────────

    @Nested
    @DisplayName("0200 — Autorização")
    class AuthorizationTest {

        @Test
        @DisplayName("Deve criar mensagem 0200 com todos os campos obrigatórios")
        void shouldCreateAuthorization() {
            IsoMessage msg = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

            assertThat(msg.getType()).isEqualTo(0x0200);
            assertThat(msg.getObjectValue(2)).isEqualTo(PAN);
            assertThat(msg.getObjectValue(3)).isEqualTo("003000");     // Processing code do template
            assertThat(msg.getObjectValue(4).toString()).contains("150");
            assertThat(msg.getObjectValue(41)).isEqualTo(TERMINAL_ID);
            assertThat(msg.getObjectValue(42)).isEqualTo(MERCHANT_ID);
            assertThat(msg.getObjectValue(49)).isEqualTo("986");       // BRL do template
        }

        @Test
        @DisplayName("Deve serializar (pack) e desserializar (unpack) mantendo campos intactos")
        void shouldPackAndUnpack() throws ParseException, IOException {
            IsoMessage original = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

            // Pack → bytes
            byte[] packed = J8583Examples.pack(original);
            assertThat(packed).isNotEmpty();

            // Unpack → mensagem reconstruída
            IsoMessage parsed = J8583Examples.unpack(mf, packed);
            assertThat(parsed.getType()).isEqualTo(0x0200);
            assertThat(parsed.getObjectValue(2)).isEqualTo(PAN);
            assertThat(parsed.getObjectValue(41)).isEqualTo(TERMINAL_ID);
            assertThat(parsed.getObjectValue(42)).isEqualTo(MERCHANT_ID);
        }

        @Test
        @DisplayName("STAN (campo 11) deve ser preenchido automaticamente pelo TraceGenerator")
        void shouldAutoAssignStan() {
            IsoMessage msg1 = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);
            IsoMessage msg2 = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

            assertThat(msg1.hasField(11)).isTrue();
            assertThat(msg2.hasField(11)).isTrue();

            int stan1 = Integer.parseInt(msg1.getObjectValue(11).toString());
            int stan2 = Integer.parseInt(msg2.getObjectValue(11).toString());
            assertThat(stan2).isGreaterThan(stan1);
        }
    }

    // ─── Response ─────────────────────────────────────────────────

    @Nested
    @DisplayName("0210 — Response")
    class ResponseTest {

        @Test
        @DisplayName("Deve criar response 0210 a partir de request 0200")
        void shouldCreateResponse() {
            IsoMessage request = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

            IsoMessage response = J8583Examples.buildResponse(request, "ABC123", "00");

            assertThat(response.getType()).isEqualTo(0x0210);
            assertThat(response.getObjectValue(2)).isEqualTo(PAN);      // Copiado do request
            assertThat(response.getObjectValue(38)).isEqualTo("ABC123"); // Auth code
            assertThat(response.getObjectValue(39)).isEqualTo("00");     // Approved
        }

        @Test
        @DisplayName("Response deve manter campos do request original")
        void shouldPreserveRequestFields() {
            IsoMessage request = J8583Examples.buildAuthorization(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

            IsoMessage response = J8583Examples.buildResponse(request, "XYZ789", "00");

            assertThat(response.getObjectValue(41)).isEqualTo(TERMINAL_ID);
            assertThat(response.getObjectValue(42)).isEqualTo(MERCHANT_ID);
            assertThat(response.getObjectValue(49)).isEqualTo("986");
        }
    }

    // ─── Network Management ──────────────────────────────────────

    @Nested
    @DisplayName("0800 — Network Management")
    class EchoTest {

        @Test
        @DisplayName("Deve criar echo-test 0800 com campo 70=301")
        void shouldCreateEchoTest() {
            IsoMessage msg = J8583Examples.buildEchoTest(mf);

            assertThat(msg.getType()).isEqualTo(0x0800);
            assertThat(msg.getObjectValue(70).toString()).contains("301");
        }

        @Test
        @DisplayName("Echo-test deve ser serializável")
        void shouldPackEchoTest() {
            IsoMessage msg = J8583Examples.buildEchoTest(mf);
            byte[] packed = J8583Examples.pack(msg);
            assertThat(packed).isNotEmpty();
        }
    }

    // ─── Reversal ────────────────────────────────────────────────

    @Nested
    @DisplayName("0400 — Reversal")
    class ReversalTest {

        @Test
        @DisplayName("Deve criar reversal 0400 com dados da transação original")
        void shouldCreateReversal() {
            IsoMessage msg = J8583Examples.buildReversal(
                    mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID, "000001");

            assertThat(msg.getType()).isEqualTo(0x0400);
            assertThat(msg.getObjectValue(2)).isEqualTo(PAN);
            assertThat(msg.getObjectValue(3)).isEqualTo("003000");
            assertThat(msg.getObjectValue(41)).isEqualTo(TERMINAL_ID);
        }
    }

    // ─── Factory ─────────────────────────────────────────────────

    @Nested
    @DisplayName("J8583Factory")
    class FactoryTest {

        @Test
        @DisplayName("Deve criar factory a partir do XML do classpath")
        void shouldCreateFromXml() throws IOException {
            MessageFactory<IsoMessage> factory = J8583Factory.create();
            assertThat(factory).isNotNull();
        }

        @Test
        @DisplayName("Deve criar factory programaticamente (sem XML)")
        void shouldCreateProgrammatic() {
            MessageFactory<IsoMessage> factory = J8583Factory.createProgrammatic();
            assertThat(factory).isNotNull();

            IsoMessage msg = factory.newMessage(0x0200);
            assertThat(msg.getType()).isEqualTo(0x0200);
            // Template programático deve ter campo 3 e 49
            assertThat(msg.getObjectValue(3)).isEqualTo("003000");
            assertThat(msg.getObjectValue(49)).isEqualTo("986");
        }
    }

    // ─── Dump / Debug ────────────────────────────────────────────

    @Test
    @DisplayName("dump() deve gerar representação legível da mensagem")
    void shouldDumpMessage() {
        IsoMessage msg = J8583Examples.buildAuthorization(
                mf, PAN, AMOUNT, TERMINAL_ID, MERCHANT_ID);

        String dump = J8583Examples.dump(msg);

        assertThat(dump)
                .contains("MTI: 0x0200")
                .contains("F2")
                .contains(PAN)
                .contains("F41")
                .contains(TERMINAL_ID);
    }
}
