package com.closiq.cart.web;

import com.closiq.cart.service.CartService;
import com.closiq.cart.web.dto.CartResponse;
import com.closiq.cart.web.dto.MergeCartRequest;
import com.closiq.cart.web.dto.ReplaceCartRequest;
import com.closiq.common.security.UserPrincipal;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Server-side bag for authenticated users")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get account bag")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                cartService.getCart(principal.userId()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PutMapping
    @Operation(summary = "Replace account bag with client state")
    public ResponseEntity<ApiResponse<CartResponse>> replaceCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReplaceCartRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                cartService.replaceCart(principal.userId(), body.getItems()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest bag into account bag after login")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MergeCartRequest body,
            HttpServletRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                cartService.mergeGuestCart(principal.userId(), body.getGuestItems()),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
