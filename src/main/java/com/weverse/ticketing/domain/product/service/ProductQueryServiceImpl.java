package com.weverse.ticketing.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weverse.ticketing.domain.product.dto.ProductResponseDto;
import com.weverse.ticketing.domain.product.entity.Product;
import com.weverse.ticketing.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * [CQRS Read 서비스]
 * - 대규모 트래픽에서 RDBMS의 부하를 막기 위해 Redis 캐시를 우선 조회(Cache-Aside)합니다.
 * - 읽기 전용 트랜잭션(@Transactional(readOnly = true))을 적용하여 DB 성능을 최적화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRODUCT_DETAIL_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_STOCK_KEY_PREFIX = "product:stock:";
    private static final long CACHE_TTL_MINUTES = 10;

    /**
     * 1. 상품 상세 정보 조회 (Cache-Aside 패턴)
     * - 1순위: Redis 캐시에서 JSON 문자열 조회 (Cache Hit -> 0.001초 반환)
     * - 2순위: 캐시에 없으면 MySQL DB 조회 (Cache Miss -> DB 부하 발생)
     * - 3순위: DB에서 읽은 데이터를 다음 유저들을 위해 Redis에 저장(Cache Warming) 후 반환
     */
    @Override
    public ProductResponseDto getProductDetail(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId must not be null.");
        }

        String cacheKey = PRODUCT_DETAIL_CACHE_KEY_PREFIX + productId;

        // Step 1: Redis 캐시 조회 (Cache Hit 검사)
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.info("[CQRS Cache Hit] 상품 상세 캐시 조회 성공: ProductId={}", productId);
                return objectMapper.readValue(cachedJson, ProductResponseDto.class);
            }
        } catch (Exception e) {
            log.warn("[CQRS Cache Read Warning] Redis 캐시 읽기 실패 (DB Fallback 진행): {}", e.getMessage());
        }

        // Step 2: Cache Miss -> RDBMS 조회
        log.info("[CQRS Cache Miss] RDBMS 상품 상세 조회: ProductId={}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + productId));

        ProductResponseDto responseDto = ProductResponseDto.fromEntity(product);

        // Step 3: 다음 조회를 위해 Redis 캐시에 저장 (TTL 10분)
        try {
            String jsonToCache = objectMapper.writeValueAsString(responseDto);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("[CQRS Cache Warming] Redis 캐시 적재 완료: Key={}, TTL={}분", cacheKey, CACHE_TTL_MINUTES);
        } catch (Exception e) {
            log.error("[CQRS Cache Write Error] Redis 캐시 쓰기 실패: {}", e.getMessage());
        }

        return responseDto;
    }

    /**
     * 2. 실시간 잔여 재고 조회
     * - 선착순 예매 시 수만 명이 실시간으로 남은 재고를 새로고침할 때 DB를 보호하기 위해
     *   Redis In-Memory 원자적 재고 키(product:stock:{id})를 직접 조회합니다.
     */
    @Override
    public Integer getAvailableStock(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("ProductId must not be null.");
        }

        String stockKey = PRODUCT_STOCK_KEY_PREFIX + productId;

        // 1) Redis 재고 키 확인
        String stockStr = redisTemplate.opsForValue().get(stockKey);

        // 2) Redis에 재고 키가 존재하면 즉시 반환
        if (stockStr != null && !stockStr.isBlank()) {
            return Integer.parseInt(stockStr);
        }

        // 3) Redis에 아직 재고가 세팅되지 않은 경우(초기화 전), DB에서 읽어와 Redis에 초기화(Cache Warm-up)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. ID: " + productId));

        Integer availableStock = product.getAvailableStock();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock));
        log.info("[CQRS Stock Initialized] Redis 원자적 재고 초기화 완료: ProductId={}, Stock={}", productId, availableStock);

        return availableStock;
    }
}
