package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CART_PREFIX = "cart:";
    private static final long CART_TTL_DAYS = 7;

    public CartDto addItemToCart(UUID userId, CartItemDto itemDto) {
        String cartKey = getCartKey(userId);
        String field = itemDto.getProductId().toString();

        // Increment existing quantity if already present in Redis hash
        Object existingQtyObj = redisTemplate.opsForHash().get(cartKey, field);
        int newQuantity = itemDto.getQuantity();
        if (existingQtyObj != null) {
            newQuantity += Integer.parseInt(existingQtyObj.toString());
        }

        redisTemplate.opsForHash().put(cartKey, field, String.valueOf(newQuantity));
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);

        return getCart(userId);
    }

    public CartDto updateItemQuantity(UUID userId, UUID productId, int quantity) {
        String cartKey = getCartKey(userId);
        String field = productId.toString();

        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(cartKey, field);
        } else {
            redisTemplate.opsForHash().put(cartKey, field, String.valueOf(quantity));
            redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);
        }

        return getCart(userId);
    }

    public CartDto removeItemFromCart(UUID userId, UUID productId) {
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
        return getCart(userId);
    }

    public CartDto getCart(UUID userId) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);

        List<CartItemDto> items = new ArrayList<>();
        entries.forEach((prodIdKey, qtyVal) -> {
            items.add(CartItemDto.builder()
                    .productId(UUID.fromString(prodIdKey.toString()))
                    .quantity(Integer.parseInt(qtyVal.toString()))
                    .build());
        });

        return CartDto.builder()
                .userId(userId)
                .items(items)
                .build();
    }

    public void clearCart(UUID userId) {
        String cartKey = getCartKey(userId);
        redisTemplate.delete(cartKey);
    }

    private String getCartKey(UUID userId) {
        return CART_PREFIX + userId.toString();
    }
}
