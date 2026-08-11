package com.closiq.cart.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class MergeCartRequest {

    @NotNull
    @Valid
    List<CartItemRequest> guestItems;
}
