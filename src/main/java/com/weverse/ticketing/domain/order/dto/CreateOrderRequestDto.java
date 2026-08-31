package com.weverse.ticketing.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateOrderRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotNull(message = "배송지 ID는 필수입니다.")
    private Long deliveryAddressId;

    @NotNull(message = "주문 수량은 필수입니다.")
    @Min(value = 1, message = "최소 1개 이상 주문해야 합니다.")
    private Integer quantity;
}
