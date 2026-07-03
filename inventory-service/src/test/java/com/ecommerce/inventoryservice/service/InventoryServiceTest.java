package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.dto.InventoryDto;
import com.ecommerce.inventoryservice.dto.ReservationResponseDto;
import com.ecommerce.inventoryservice.model.Inventory;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    public void testAddStock() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory initial = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(2)
                .build();

        Inventory saved = Inventory.builder()
                .productId(productId)
                .stockQuantity(15)
                .reservedQuantity(2)
                .build();

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(initial));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(saved);

        // Act
        InventoryDto result = inventoryService.addStock(productId, 5);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getStockQuantity());
        verify(inventoryRepository).findById(productId);
        verify(inventoryRepository).save(initial);
    }

    @Test
    public void testReserveOptimistic_Success() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(2)
                .build();

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // Act
        ReservationResponseDto response = inventoryService.reserveOptimistic(productId, 3);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Success (Optimistic Lock)", response.getMessage());
        assertEquals(5, inventory.getReservedQuantity());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    public void testReserveOptimistic_InsufficientStock() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(8)
                .build();

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // Act
        ReservationResponseDto response = inventoryService.reserveOptimistic(productId, 3);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Insufficient stock", response.getMessage());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    public void testReserveOptimistic_LockConflict() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(2)
                .build();

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(Inventory.class, productId));

        // Act
        ReservationResponseDto response = inventoryService.reserveOptimistic(productId, 3);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Optimistic lock conflict; try again", response.getMessage());
    }

    @Test
    public void testReservePessimistic_Success() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(2)
                .build();

        when(inventoryRepository.findByProductIdWithPessimisticLock(productId)).thenReturn(Optional.of(inventory));

        // Act
        ReservationResponseDto response = inventoryService.reservePessimistic(productId, 4);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Success (Pessimistic Lock)", response.getMessage());
        assertEquals(6, inventory.getReservedQuantity());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    public void testReserveWithRedisLock_Success() throws InterruptedException {
        // Arrange
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .stockQuantity(10)
                .reservedQuantity(2)
                .build();

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(inventory));

        // Act
        ReservationResponseDto response = inventoryService.reserveWithRedisLock(productId, 4);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Success (Redis Distributed Lock)", response.getMessage());
        assertEquals(6, inventory.getReservedQuantity());
        verify(lock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(lock).unlock();
        verify(inventoryRepository).save(inventory);
    }

    @Test
    public void testReserveWithRedisLock_LockFailure() throws InterruptedException {
        // Arrange
        UUID productId = UUID.randomUUID();
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act
        ReservationResponseDto response = inventoryService.reserveWithRedisLock(productId, 4);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Could not acquire Redis lock; timeout", response.getMessage());
        verify(inventoryRepository, never()).findById(any());
        verify(lock, never()).unlock();
    }
}
