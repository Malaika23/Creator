package com.ecommerce.cartservice.controller;

import com.ecommerce.common.context.UserContext;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.dto.CartItemDto;
import com.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartDto> addItemToCart(@Valid @RequestBody CartItemDto itemDto) {
        UUID userId = getAuthenticatedUserId();
        CartDto cart = cartService.addItemToCart(userId, itemDto);
        return ResponseEntity.ok(cart);
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart() {
        UUID userId = getAuthenticatedUserId();
        CartDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<CartDto> updateQuantity(
            @PathVariable UUID productId,
            @RequestParam int quantity) {
        UUID userId = getAuthenticatedUserId();
        CartDto cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<CartDto> removeItem(@PathVariable UUID productId) {
        UUID userId = getAuthenticatedUserId();
        CartDto cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        UUID userId = getAuthenticatedUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedUserId() {
        String userIdStr = UserContext.getCurrentContext().getUserId();
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new UnauthorizedException("Authentication context missing");
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid User ID format");
        }
    }
}
