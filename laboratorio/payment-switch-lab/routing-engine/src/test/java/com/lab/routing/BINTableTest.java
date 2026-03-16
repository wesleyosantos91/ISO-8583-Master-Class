package com.lab.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BINTableTest {

    private BINTable table;

    @BeforeEach
    void setUp() {
        table = new BINTable();
        table.addRoute("45320151", new Route("45320151", "Itau", "VISA", true, "local-issuer", "visa-mux"));
        table.addRoute("40000000", new Route("40000000", "Bradesco", "VISA", false, "visa-mux", null));
        table.addRoute("51230000", new Route("51230000", "Nubank", "MASTERCARD", false, "mastercard-mux", null));
    }

    @Test
    void shouldLookupByExact8DigitBIN() {
        Route route = table.lookup("4532015112830366");
        assertThat(route).isNotNull();
        assertThat(route.issuerName()).isEqualTo("Itau");
        assertThat(route.isOnUs()).isTrue();
    }

    @Test
    void shouldLookupOffUsBIN() {
        Route route = table.lookup("4000000012345678");
        assertThat(route).isNotNull();
        assertThat(route.issuerName()).isEqualTo("Bradesco");
        assertThat(route.isOnUs()).isFalse();
    }

    @Test
    void shouldReturnNullForUnknownBIN() {
        Route route = table.lookup("9999999912345678");
        assertThat(route).isNull();
    }

    @Test
    void shouldReturnNullForNullPAN() {
        assertThat(table.lookup(null)).isNull();
    }

    @Test
    void shouldReturnNullForShortPAN() {
        assertThat(table.lookup("123")).isNull();
    }

    @Test
    void shouldTrackSize() {
        assertThat(table.size()).isEqualTo(3);
    }
}
