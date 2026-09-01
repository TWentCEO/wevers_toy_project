package com.weverse.ticketing.domain.order.service;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;
import com.weverse.ticketing.domain.product.entity.Product;
import com.weverse.ticketing.domain.product.repository.ProductRepository;
import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import com.weverse.ticketing.domain.queue.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderCommandService orderCommandService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long testProductId;
    private static final int INITIAL_STOCK = 100; // 초기 한정 수량 100개

    @BeforeEach
    void setUp() {
        // 1. Redis 초기화
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 2. MySQL 상품 DB 저장 및 Redis 초기 재고 100개 세팅
        Product product = Product.builder()
                .name("BTS 한정판 LP 앨범")
                .description("전 세계 100장 한정판")
                .price(new BigDecimal("120000.00"))
                .totalStock(INITIAL_STOCK)
                .availableStock(INITIAL_STOCK)
                .status(Product.ProductStatus.ON_SALE)
                .salesStartAt(LocalDateTime.now().minusDays(1))
                .salesEndAt(LocalDateTime.now().plusDays(1))
                .build();
        Product savedProduct = productRepository.save(product);
        testProductId = savedProduct.getId();

        // Redis 원자적 재고 100개 세팅
        redisTemplate.opsForValue().set("product:stock:" + testProductId, String.valueOf(INITIAL_STOCK));
    }

    @Test
    @DisplayName("동시에 1,000명의 유저가 100개 한정 수량 주문을 요청하면, 정확히 100명만 성공하고 900명은 품절 실패(Zero Overselling)해야 한다.")
    void concurrent_1000_Orders_Zero_Overselling_Success() throws InterruptedException {
        int totalRequests = 1000;
        int threadPoolSize = 32;

        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 1,000명의 유저에 대해 미리 ACTIVE 토큰 발급
        for (int i = 1; i <= totalRequests; i++) {
            long userId = (long) i;
            // 대기열 진입
            QueueResponseDto enterRes = queueService.enterQueue(new QueueEnterRequestDto(userId, testProductId));
            // 테스트를 위해 모두 ACTIVE 토큰으로 승급 처리
            redisTemplate.opsForValue().set("queue:token:" + enterRes.getToken(), "ACTIVE");

            String token = enterRes.getToken();

            // 멀티 스레드 동시 주문 요청 실행
            executorService.submit(() -> {
                try {
                    CreateOrderRequestDto requestDto = new CreateOrderRequestDto(userId, testProductId, null, 1);
                    CreateOrderResponseDto response = orderCommandService.createOrderAsync(token, requestDto);

                    if (response != null && response.getStatus() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (IllegalStateException e) {
                    // 품절로 인한 정상 실패 (Out of stock)
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 1,000개의 스레드가 모두 끝날 때까지 대기
        executorService.shutdown();

        // Then 검증:
        // 1. 성공 주문 수는 정확히 초기 재고인 100건이어야 함!
        assertThat(successCount.get()).isEqualTo(100);

        // 2. 실패 주문 수는 정확히 900건이어야 함!
        assertThat(failCount.get()).isEqualTo(900);

        // 3. Redis 최종 잔여 재고는 0이어야 함 (초과 판매 Zero!)
        String finalStockStr = redisTemplate.opsForValue().get("product:stock:" + testProductId);
        assertThat(finalStockStr).isEqualTo("0");
    }
}
