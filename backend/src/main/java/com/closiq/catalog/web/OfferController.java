package com.closiq.catalog.web;

import com.closiq.catalog.service.OfferService;
import com.closiq.catalog.web.dto.OfferResponse;
import com.closiq.common.web.ApiResponse;
import com.closiq.common.web.ClosiqRequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@Tag(name = "Offers", description = "Active promotional offers")
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    @Operation(summary = "List active promotional offers")
    public ResponseEntity<ApiResponse<List<OfferResponse>>> listOffers(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                offerService.listActiveOffers(),
                ClosiqRequestIdFilter.getRequestId(request)));
    }
}
