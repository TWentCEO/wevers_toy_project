package com.weverse.ticketing.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateOrderResponseDto {

    private final String orderNumber;
    private final String status;
    private final String message;

    @Builder
    public CreateOrderResponseDto(String orderNumber, String status, String message) {
        this.orderNumber = orderNumber;
        this.status = status;
        this.message = message;
    }

    public static CreateOrderResponseDto accepted(String orderNumber) {
        return CreateOrderResponseDto.builder()
                .orderNumber(orderNumber)
                .status("ACCEPTED")
                .message("주문 요청이 성공적으로 접수되어 대기열/비동기 처리 중입니다.")
                .build();
    }
}
