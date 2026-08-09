package com.closiq.user.service;

import com.closiq.common.exception.ErrorCode;
import com.closiq.common.exception.ClosiqException;
import com.closiq.common.util.IdGenerator;
import com.closiq.common.web.PageBoundary;
import com.closiq.common.web.PageTokenCodec;
import com.closiq.common.web.PagedResult;
import com.closiq.catalog.domain.Product;
import com.closiq.catalog.repository.ProductRepository;
import com.closiq.identity.domain.User;
import com.closiq.identity.service.UserService;
import com.closiq.user.domain.WishlistItem;
import com.closiq.user.domain.WishlistItemId;
import com.closiq.user.mapper.UserProfileMapper;
import com.closiq.user.repository.WishlistItemRepository;
import com.closiq.user.web.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final int MAX_WISHLIST_ITEMS = 100;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String ACTIVE_PRODUCT = "ACTIVE";

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public PagedResult<WishlistItemResponse> listWishlist(UUID userId, String pageToken, Integer limit) {
        int pageSize = normalizeLimit(limit);
        PageBoundary boundary = PageTokenCodec.wishlistBoundary(pageToken);

        List<WishlistItem> items = wishlistItemRepository.findPage(
                userId,
                boundary.beforeCreatedAt(),
                boundary.beforeId(),
                PageRequest.of(0, pageSize + 1));

        boolean hasMore = items.size() > pageSize;
        List<WishlistItem> pageItems = hasMore ? items.subList(0, pageSize) : items;

        List<WishlistItemResponse> responses = pageItems.stream()
                .map(userProfileMapper::toWishlistResponse)
                .toList();

        String nextPageToken = hasMore && !pageItems.isEmpty()
                ? PageTokenCodec.encodeWishlist(new PageTokenCodec.WishlistPageToken(
                        pageItems.getLast().getCreatedAt(),
                        pageItems.getLast().getId().getProductId()))
                : null;

        return PagedResult.of(responses, pageSize, hasMore, nextPageToken);
    }

    @Transactional
    public WishlistItemResponse addToWishlist(UUID userId, UUID productId) {
        if (wishlistItemRepository.countByIdUserId(userId) >= MAX_WISHLIST_ITEMS) {
            throw new ClosiqException(ErrorCode.VALIDATION_ERROR, "Maximum of 100 wishlist items allowed");
        }

        if (wishlistItemRepository.existsByIdUserIdAndIdProductId(userId, productId)) {
            throw new ClosiqException(ErrorCode.ALREADY_EXISTS, "Product is already in wishlist");
        }

        Product product = productRepository.findByIdAndDeletedAtIsNullAndStatus(productId, ACTIVE_PRODUCT)
                .orElseThrow(() -> new ClosiqException(ErrorCode.NOT_FOUND, "Product not found"));

        User user = userService.requireActiveUser(userId);
        WishlistItem item = WishlistItem.builder()
                .id(new WishlistItemId(userId, productId))
                .user(user)
                .product(product)
                .build();

        wishlistItemRepository.save(item);
        return userProfileMapper.toWishlistResponse(item);
    }

    @Transactional
    public void removeFromWishlist(UUID userId, UUID productId) {
        WishlistItemId id = new WishlistItemId(userId, productId);
        if (!wishlistItemRepository.existsById(id)) {
            throw new ClosiqException(ErrorCode.NOT_FOUND, "Wishlist item not found");
        }
        wishlistItemRepository.deleteById(id);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
