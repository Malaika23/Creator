package com.ecommerce.orderservice.controller;

import com.ecommerce.common.context.UserContext;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        UUID userId = getAuthenticatedUserId();
        OrderResponseDto order = orderService.createOrder(userId, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID orderId) {
        OrderResponseDto order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrderHistory() {
        UUID userId = getAuthenticatedUserId();
        List<OrderResponseDto> history = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(history);
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
