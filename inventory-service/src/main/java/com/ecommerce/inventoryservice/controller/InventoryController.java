package com.ecommerce.inventoryservice.controller;

import com.ecommerce.common.context.UserContext;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.inventoryservice.dto.InventoryDto;
import com.ecommerce.inventoryservice.dto.ReservationRequestDto;
import com.ecommerce.inventoryservice.dto.ReservationResponseDto;
import com.ecommerce.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock")
    public ResponseEntity<InventoryDto> addStock(
            @RequestParam UUID productId,
            @RequestParam int quantity) {
        verifyAdmin();
        InventoryDto dto = inventoryService.addStock(productId, quantity);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryDto> getStock(@PathVariable UUID productId) {
        InventoryDto dto = inventoryService.getStock(productId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/reserve/optimistic")
    public ResponseEntity<ReservationResponseDto> reserveOptimistic(@Valid @RequestBody ReservationRequestDto request) {
        ReservationResponseDto response = inventoryService.reserveOptimistic(request.getProductId(), request.getQuantity());
        if (!response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reserve/pessimistic")
    public ResponseEntity<ReservationResponseDto> reservePessimistic(@Valid @RequestBody ReservationRequestDto request) {
        ReservationResponseDto response = inventoryService.reservePessimistic(request.getProductId(), request.getQuantity());
        if (!response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reserve/redis")
    public ResponseEntity<ReservationResponseDto> reserveRedis(@Valid @RequestBody ReservationRequestDto request) {
        ReservationResponseDto response = inventoryService.reserveWithRedisLock(request.getProductId(), request.getQuantity());
        if (!response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok(response);
    }

    private void verifyAdmin() {
        String role = UserContext.getCurrentContext().getRole();
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new UnauthorizedException("Access denied: Admin role required");
        }
    }
}
