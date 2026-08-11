package com.closiq.seller.service;

import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.user.domain.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProductAccessService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Product requireOwnedProduct(SellerProfile seller, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        if (product.getSellerProfileId() == null || !product.getSellerProfileId().equals(seller.getId())) {
            throw new ClosiqException(ErrorCode.FORBIDDEN, "You do not own this product");
        }
        return product;
    }
}
