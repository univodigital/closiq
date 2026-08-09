package com.closiq.common.identifier;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class OrderNumberGenerator implements CodeGenerationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final BusinessSequenceRepository sequences;

    public OrderNumberGenerator(BusinessSequenceRepository sequences) {
        this.sequences = sequences;
    }

    @Override
    public String nextCode() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DATE);
        long seq = sequences.nextOrderNumberSequence();
        return "VST-ORD-" + date + "-" + String.format("%04d", seq % 10_000);
    }
}
