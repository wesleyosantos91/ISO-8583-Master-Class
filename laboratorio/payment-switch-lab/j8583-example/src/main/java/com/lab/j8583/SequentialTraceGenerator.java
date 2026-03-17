package com.lab.j8583;

import com.solab.iso8583.TraceNumberGenerator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerador sequencial de STAN (System Trace Audit Number — campo 11).
 *
 * <p>Em jPOS, o STAN é setado manualmente: {@code msg.set(11, "000001")}.
 * Em j8583, você pode registrar um TraceNumberGenerator no MessageFactory
 * para que o campo 11 seja preenchido automaticamente.</p>
 */
public class SequentialTraceGenerator implements TraceNumberGenerator {

    private final AtomicInteger counter;

    public SequentialTraceGenerator() {
        this(0);
    }

    public SequentialTraceGenerator(int initialValue) {
        this.counter = new AtomicInteger(initialValue);
    }

    @Override
    public int getLastTraceNumber() {
        return counter.get();
    }

    @Override
    public int nextTrace() {
        int next = counter.incrementAndGet();
        if (next > 999_999) {
            counter.set(1);
            return 1;
        }
        return next;
    }
}
