package com.weverse.ticketing.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Column(name = "sales_start_at", nullable = false)
    private LocalDateTime salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private LocalDateTime salesEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ProductStatus {
        READY,
        ON_SALE,
        SOLD_OUT,
        CLOSED
    }

    @Builder
    public Product(String name, String description, BigDecimal price, Integer totalStock, Integer availableStock, LocalDateTime salesStartAt, LocalDateTime salesEndAt, ProductStatus status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalStock = totalStock;
        this.availableStock = availableStock;
        this.salesStartAt = salesStartAt;
        this.salesEndAt = salesEndAt;
        this.status = status != null ? status : ProductStatus.READY;
    }

    public void decreaseStock(int quantity) {
        if (this.availableStock < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + this.availableStock);
        }
        this.availableStock -= quantity;
        if (this.availableStock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }
}
