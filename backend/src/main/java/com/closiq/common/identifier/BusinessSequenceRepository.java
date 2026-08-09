package com.closiq.common.identifier;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BusinessSequenceRepository {

    private final EntityManager entityManager;

    public long nextProductCodeSequence() {
        return nextVal("product_code_seq");
    }

    public long nextUserCodeSequence() {
        return nextVal("user_code_seq");
    }

    public long nextOrderNumberSequence() {
        return nextVal("order_number_seq");
    }

    public long nextRentalNumberSequence() {
        return nextVal("rental_number_seq");
    }

    private long nextVal(String sequenceName) {
        Number value = (Number) entityManager
                .createNativeQuery("SELECT nextval('" + sequenceName + "')")
                .getSingleResult();
        return value.longValue();
    }
}
