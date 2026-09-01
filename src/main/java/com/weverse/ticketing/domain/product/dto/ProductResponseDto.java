package com.weverse.ticketing.domain.product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.weverse.ticketing.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer availableStock;
    private Product.ProductStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime salesStartAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime salesEndAt;

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
