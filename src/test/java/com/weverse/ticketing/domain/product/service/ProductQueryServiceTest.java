package com.weverse.ticketing.domain.product.service;

import com.weverse.ticketing.domain.product.dto.ProductResponseDto;
import com.weverse.ticketing.domain.product.entity.Product;
import com.weverse.ticketing.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductQueryServiceTest {

    @Autowired
    private ProductQueryService productQueryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Redis 상품 캐시 초기화
        Set<String> keys = redisTemplate.keys("product:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 테스트용 상품 DB 저장
        testProduct = Product.builder()
                .name("BTS 한정판 포토북")
                .description("2026 월드투어 한정판 포토북")
                .price(new BigDecimal("45000.00"))
                .totalStock(100)
                .availableStock(100)
                .status(Product.ProductStatus.ON_SALE)
                .salesStartAt(LocalDateTime.now().minusDays(1))
                .salesEndAt(LocalDateTime.now().plusDays(1))
                .build();
        productRepository.save(testProduct);
    }

    @Test
    @DisplayName("첫 조회 시 Cache Miss가 발생하여 DB에서 조회하고, 이후 Redis에 캐시가 적재되어야 한다.")
    void getProductDetail_CacheAside_Success() {
        Long productId = testProduct.getId();
        String cacheKey = "product:detail:" + productId;

        // 1. 조회 전에는 Redis에 캐시가 없어야 함
        assertThat(redisTemplate.hasKey(cacheKey)).isFalse();

        // 2. 첫 번째 조회 (Cache Miss -> DB 조회 & Redis 캐시 적재)
        ProductResponseDto firstResponse = productQueryService.getProductDetail(productId);
        assertThat(firstResponse.getName()).isEqualTo("BTS 한정판 포토북");
        assertThat(firstResponse.getPrice()).isEqualByComparingTo(new BigDecimal("45000.00"));

        // 3. 조회 후에는 Redis에 캐시가 존재해야 함!
        assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

        // 4. 두 번째 조회 (Cache Hit -> Redis에서 0.001초 만에 바로 반환)
        ProductResponseDto secondResponse = productQueryService.getProductDetail(productId);
        assertThat(secondResponse.getName()).isEqualTo(firstResponse.getName());
        assertThat(secondResponse.getId()).isEqualTo(firstResponse.getId());
    }

    @Test
    @DisplayName("실시간 잔여 재고 조회 시 Redis에 재고 키가 초기화되고 정확한 재고(100)가 반환되어야 한다.")
    void getAvailableStock_Success() {
        Long productId = testProduct.getId();
        String stockKey = "product:stock:" + productId;

        // When
        Integer availableStock = productQueryService.getAvailableStock(productId);

        // Then
        assertThat(availableStock).isEqualTo(100);
        assertThat(redisTemplate.hasKey(stockKey)).isTrue();
        assertThat(redisTemplate.opsForValue().get(stockKey)).isEqualTo("100");
    }
}
