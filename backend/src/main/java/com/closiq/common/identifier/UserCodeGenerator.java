package com.closiq.common.identifier;

import org.springframework.stereotype.Service;

@Service
public class UserCodeGenerator implements CodeGenerationService {

    private final BusinessSequenceRepository sequences;

    public UserCodeGenerator(BusinessSequenceRepository sequences) {
        this.sequences = sequences;
    }

    @Override
    public String nextCode() {
        return "VST-USR-" + String.format("%06d", sequences.nextUserCodeSequence());
    }
}
