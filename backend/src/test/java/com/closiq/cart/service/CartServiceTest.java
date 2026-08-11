package com.closiq.cart.service;

import com.closiq.cart.domain.CartItem;
import com.closiq.cart.repository.CartItemRepository;
import com.closiq.cart.web.dto.CartItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void mergeGuestCart_keepsAccountItemsAndAddsGuestOnlyLines() {
        List<CartItem> saved = new ArrayList<>();
        when(cartItemRepository.findByCustomerIdOrderByUpdatedAtDesc(CUSTOMER_ID)).thenAnswer(inv -> saved);
        when(cartItemRepository.save(any())).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            saved.add(item);
            return item;
        });
        doAnswer(inv -> {
            saved.clear();
            return null;
        }).when(cartItemRepository).deleteByCustomerId(CUSTOMER_ID);

        saved.add(CartItem.builder()
                .productSlug("account-only")
                .variantSize("M")
                .rentalStartDate(LocalDate.of(2026, 8, 15))
                .rentalEndDate(LocalDate.of(2026, 8, 17))
                .build());

        var guest = List.of(
                new CartItemRequest("shared-slug", "L", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)),
                new CartItemRequest("guest-only", "S", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)));

        var response = cartService.mergeGuestCart(CUSTOMER_ID, guest);

        assertThat(response.getItems())
                .extracting(item -> item.getProductSlug())
                .containsExactlyInAnyOrder("account-only", "shared-slug", "guest-only");
    }

    @Test
    void replaceCart_flushesAfterDeleteBeforeInsert() {
        when(cartItemRepository.findByCustomerIdOrderByUpdatedAtDesc(CUSTOMER_ID)).thenReturn(List.of());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var items = List.of(
                new CartItemRequest("blush-kids-lehenga-set", "S", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)));

        cartService.replaceCart(CUSTOMER_ID, items);

        InOrder order = inOrder(cartItemRepository);
        order.verify(cartItemRepository).deleteByCustomerId(CUSTOMER_ID);
        order.verify(cartItemRepository).flush();
        order.verify(cartItemRepository).save(any());
    }

    @Test
    void mergeGuestCart_flushesAfterDeleteBeforeInsert() {
        when(cartItemRepository.findByCustomerIdOrderByUpdatedAtDesc(CUSTOMER_ID)).thenReturn(List.of());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var guest = List.of(
                new CartItemRequest("midnight-embroidered-sherwani", "L", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)));

        cartService.mergeGuestCart(CUSTOMER_ID, guest);

        InOrder order = inOrder(cartItemRepository);
        order.verify(cartItemRepository).deleteByCustomerId(CUSTOMER_ID);
        order.verify(cartItemRepository).flush();
        order.verify(cartItemRepository).save(any());
    }
}
