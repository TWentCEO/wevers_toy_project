package com.weverse.ticketing.domain.product.dto;

import com.weverse.ticketing.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer availableStock;
    private final Product.ProductStatus status;
    private final LocalDateTime salesStartAt;
    private final LocalDateTime salesEndAt;

    @Builder
    public ProductResponseDto(Long id, String name, String description, BigDecimal price,
                              Integer availableStock, Product.ProductStatus status,
                              LocalDateTime salesStartAt, LocalDateTime salesEndAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.availableStock = availableStock;
        this.status = status;
        this.salesStartAt = salesStartAt;
        this.salesEndAt = salesEndAt;
    }

    public static ProductResponseDto fromEntity(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .availableStock(product.getAvailableStock())
                .status(product.getStatus())
                .salesStartAt(product.getSalesStartAt())
                .salesEndAt(product.getSalesEndAt())
                .build();
    }
}
