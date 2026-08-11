package com.closiq.cart.service;

import com.closiq.cart.domain.CartItem;
import com.closiq.cart.repository.CartItemRepository;
import com.closiq.cart.web.dto.CartItemRequest;
import com.closiq.cart.web.dto.CartResponse;
import com.closiq.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID customerId) {
        return toResponse(cartItemRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId));
    }

    /** Replace server cart with client state — server becomes authoritative. */
    @Transactional
    public CartResponse replaceCart(UUID customerId, List<CartItemRequest> items) {
        replaceAllItems(customerId, items);
        return getCart(customerId);
    }

    /**
     * Merge guest items into account cart without dropping existing account lines.
     * On slug conflict, keep the item with the longer rental span; tie-breaker prefers guest.
     */
    @Transactional
    public CartResponse mergeGuestCart(UUID customerId, List<CartItemRequest> guestItems) {
        Map<String, CartItemRequest> merged = new LinkedHashMap<>();
        for (CartItem entity : cartItemRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId)) {
            merged.put(entity.getProductSlug(), toRequest(entity));
        }
        for (CartItemRequest guest : guestItems) {
            CartItemRequest existing = merged.get(guest.getProductSlug());
            if (existing == null) {
                merged.put(guest.getProductSlug(), guest);
            } else {
                merged.put(guest.getProductSlug(), pickPreferred(existing, guest));
            }
        }
        replaceAllItems(customerId, new ArrayList<>(merged.values()));
        return getCart(customerId);
    }

    private void replaceAllItems(UUID customerId, List<CartItemRequest> items) {
        cartItemRepository.deleteByCustomerId(customerId);
        cartItemRepository.flush();
        saveItems(customerId, items);
    }

    private void saveItems(UUID customerId, List<CartItemRequest> items) {
        for (CartItemRequest item : items) {
            cartItemRepository.save(CartItem.builder()
                    .id(IdGenerator.uuidV7())
                    .customerId(customerId)
                    .productSlug(item.getProductSlug())
                    .variantSize(item.getVariantSize())
                    .rentalStartDate(item.getRentalStartDate())
                    .rentalEndDate(item.getRentalEndDate())
                    .build());
        }
    }

    private static CartItemRequest pickPreferred(CartItemRequest account, CartItemRequest guest) {
        long accountDays = rentalDays(account);
        long guestDays = rentalDays(guest);
        if (guestDays > accountDays) {
            return guest;
        }
        if (guestDays < accountDays) {
            return account;
        }
        return guest;
    }

    private static long rentalDays(CartItemRequest item) {
        return item.getRentalEndDate().toEpochDay() - item.getRentalStartDate().toEpochDay() + 1;
    }

    private static CartItemRequest toRequest(CartItem entity) {
        return new CartItemRequest(
                entity.getProductSlug(),
                entity.getVariantSize(),
                entity.getRentalStartDate(),
                entity.getRentalEndDate());
    }

    private static CartResponse toResponse(List<CartItem> items) {
        List<CartResponse.Item> mapped = items.stream()
                .map(i -> CartResponse.Item.builder()
                        .productSlug(i.getProductSlug())
                        .variantSize(i.getVariantSize())
                        .rentalStartDate(i.getRentalStartDate())
                        .rentalEndDate(i.getRentalEndDate())
                        .build())
                .toList();
        return CartResponse.builder().items(mapped).build();
    }
}
