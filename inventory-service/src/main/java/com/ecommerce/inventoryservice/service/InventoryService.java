package com.ecommerce.inventoryservice.service;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventoryservice.dto.InventoryDto;
import com.ecommerce.inventoryservice.dto.ReservationResponseDto;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "invlock:";

    @Transactional
    public InventoryDto addStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElse(Inventory.builder()
                        .productId(productId)
                        .stockQuantity(0)
                        .reservedQuantity(0)
                        .build());
        inventory.setStockQuantity(inventory.getStockQuantity() + quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return mapToDto(saved);
    }

    public InventoryDto getStock(UUID productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found"));
        return mapToDto(inventory);
    }

    // 1. Optimistic Locking reservation
    @Transactional
    public ReservationResponseDto reserveOptimistic(UUID productId, int quantity) {
        try {
            Inventory inventory = inventoryRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stock item not found"));

            int available = inventory.getStockQuantity() - inventory.getReservedQuantity();
            if (available < quantity) {
                return new ReservationResponseDto(productId, false, "Insufficient stock");
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
            inventoryRepository.save(inventory);
            return new ReservationResponseDto(productId, true, "Success (Optimistic Lock)");
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for product {}", productId);
            return new ReservationResponseDto(productId, false, "Optimistic lock conflict; try again");
        }
    }

    // 2. Pessimistic Locking reservation
    @Transactional
    public ReservationResponseDto reservePessimistic(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found"));

        int available = inventory.getStockQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            return new ReservationResponseDto(productId, false, "Insufficient stock");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        return new ReservationResponseDto(productId, true, "Success (Pessimistic Lock)");
    }

    // 3. Redis Distributed Lock reservation (with Redisson)
    public ReservationResponseDto reserveWithRedisLock(UUID productId, int quantity) {
        String lockKey = LOCK_PREFIX + productId.toString();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Attempt to acquire lock. Wait up to 5 seconds, lease for 10 seconds.
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    // Execute DB write in a transaction-like pattern (simulate service isolation)
                    return executeStockReservationInTransaction(productId, quantity);
                } finally {
                    lock.unlock();
                }
            } else {
                return new ReservationResponseDto(productId, false, "Could not acquire Redis lock; timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ReservationResponseDto(productId, false, "Lock request interrupted");
        }
    }

    @Transactional
    protected ReservationResponseDto executeStockReservationInTransaction(UUID productId, int quantity) {
        // Read without locking since we have the Redis distributed lock covering this product ID block
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found"));

        int available = inventory.getStockQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            return new ReservationResponseDto(productId, false, "Insufficient stock");
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        return new ReservationResponseDto(productId, true, "Success (Redis Distributed Lock)");
    }

    @Transactional
    public void releaseStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock item not found"));

        int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        log.info("Released {} reserved units for product {}", quantity, productId);
    }

    private InventoryDto mapToDto(Inventory inventory) {
        return InventoryDto.builder()
                .productId(inventory.getProductId())
                .stockQuantity(inventory.getStockQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();
    }
}
