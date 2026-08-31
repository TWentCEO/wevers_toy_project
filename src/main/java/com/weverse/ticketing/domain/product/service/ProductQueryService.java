package com.weverse.ticketing.domain.product.service;

import com.weverse.ticketing.domain.product.dto.ProductResponseDto;

public interface ProductQueryService {

    /**
     * CQRS Read: 상품 상세 및 잔여 재고 조회 진입점
     * Redis 캐시를 우선 조회하고 Cache Miss 시 RDBMS를 폴백하는 구조
     */
    ProductResponseDto getProductDetail(Long productId);

    /**
     * CQRS Read: 실시간 잔여 재고 조회 진입점
     */
    Integer getAvailableStock(Long productId);
}
