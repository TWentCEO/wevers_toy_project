package com.weverse.ticketing.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderEventPayload {

    private final String orderNumber;
    private final Long userId;
    private final Long productId;
    private final Long deliveryAddressId;
    private final Integer quantity;
    private final LocalDateTime requestedAt;

    @Builder
    public OrderEventPayload(String orderNumber, Long userId, Long productId,
                             Long deliveryAddressId, Integer quantity, LocalDateTime requestedAt) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.productId = productId;
        this.deliveryAddressId = deliveryAddressId;
        this.quantity = quantity;
        this.requestedAt = requestedAt;
    }
}
