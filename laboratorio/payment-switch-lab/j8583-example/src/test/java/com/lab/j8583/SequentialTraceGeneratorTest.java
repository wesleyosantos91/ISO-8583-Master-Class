package com.lab.j8583;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SequentialTraceGenerator — STAN (campo 11)")
class SequentialTraceGeneratorTest {

    @Test
    @DisplayName("Deve gerar números sequenciais a partir de 1")
    void shouldGenerateSequentially() {
        var gen = new SequentialTraceGenerator();

        assertThat(gen.nextTrace()).isEqualTo(1);
        assertThat(gen.nextTrace()).isEqualTo(2);
        assertThat(gen.nextTrace()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve iniciar de valor customizado")
    void shouldStartFromCustomValue() {
        var gen = new SequentialTraceGenerator(100);

        assertThat(gen.getLastTraceNumber()).isEqualTo(100);
        assertThat(gen.nextTrace()).isEqualTo(101);
    }

    @Test
    @DisplayName("Deve resetar para 1 ao ultrapassar 999999")
    void shouldWrapAround() {
        var gen = new SequentialTraceGenerator(999_998);

        assertThat(gen.nextTrace()).isEqualTo(999_999);
        assertThat(gen.nextTrace()).isEqualTo(1);  // wrap-around
    }
}
