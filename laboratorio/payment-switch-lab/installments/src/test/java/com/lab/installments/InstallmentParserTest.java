package com.lab.installments;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InstallmentParserTest {

    @Test
    void shouldParseInstallmentCount() {
        assertThat(InstallmentParser.parseInstallmentCount("0303")).isEqualTo(3);
        assertThat(InstallmentParser.parseInstallmentCount("0512")).isEqualTo(12);
        assertThat(InstallmentParser.parseInstallmentCount("0301")).isEqualTo(1);
    }

    @Test
    void shouldReturnOneForNullOrEmpty() {
        assertThat(InstallmentParser.parseInstallmentCount(null)).isEqualTo(1);
        assertThat(InstallmentParser.parseInstallmentCount("")).isEqualTo(1);
        assertThat(InstallmentParser.parseInstallmentCount("03")).isEqualTo(1);
    }

    @Test
    void shouldParseInstallmentType() {
        assertThat(InstallmentParser.parseInstallmentType("0303")).isEqualTo("MERCHANT");
        assertThat(InstallmentParser.parseInstallmentType("0512")).isEqualTo("ISSUER");
        assertThat(InstallmentParser.parseInstallmentType("0101")).isEqualTo("CASH");
    }

    @Test
    void shouldReturnCashForNullOrEmpty() {
        assertThat(InstallmentParser.parseInstallmentType(null)).isEqualTo("CASH");
        assertThat(InstallmentParser.parseInstallmentType("")).isEqualTo("CASH");
    }

    @Test
    void shouldDetectInstallment() {
        assertThat(InstallmentParser.isInstallment("0303")).isTrue();
        assertThat(InstallmentParser.isInstallment("0301")).isFalse();
        assertThat(InstallmentParser.isInstallment(null)).isFalse();
    }
}
