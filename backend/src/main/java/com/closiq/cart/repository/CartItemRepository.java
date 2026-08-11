package com.closiq.cart.repository;

import com.closiq.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCustomerIdOrderByUpdatedAtDesc(UUID customerId);

    Optional<CartItem> findByCustomerIdAndProductSlug(UUID customerId, String productSlug);

    void deleteByCustomerId(UUID customerId);
}
