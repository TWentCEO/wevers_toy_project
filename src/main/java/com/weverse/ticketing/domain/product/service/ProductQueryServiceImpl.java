package com.weverse.ticketing.domain.product.service;

import com.weverse.ticketing.domain.product.dto.ProductResponseDto;
import com.weverse.ticketing.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public ProductResponseDto getProductDetail(Long productId) {
        // TODO: [학습자 주도 영역] 논리 적용 필요 - CQRS Read 패턴: Redis 캐시 우선 조회 및 Cache-Aside 로직 구현
        return productRepository.findById(productId)
                .map(ProductResponseDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + productId));
    }

    @Override
    public Integer getAvailableStock(Long productId) {
        // TODO: [학습자 주도 영역] 논리 적용 필요 - Redis 실시간 잔여 재고 키 조회 로직 구현
        return productRepository.findById(productId)
                .map(p -> p.getAvailableStock())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + productId));
    }
}
