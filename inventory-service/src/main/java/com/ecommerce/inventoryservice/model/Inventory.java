package com.ecommerce.inventoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    private UUID productId;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    private Long version;
}
