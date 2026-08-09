package com.closiq.common.identifier;

import org.springframework.stereotype.Service;

@Service
public class ProductCodeGenerator implements CodeGenerationService {

    private final BusinessSequenceRepository sequences;

    public ProductCodeGenerator(BusinessSequenceRepository sequences) {
        this.sequences = sequences;
    }

    @Override
    public String nextCode() {
        return "VST-PROD-" + String.format("%06d", sequences.nextProductCodeSequence());
    }
}
