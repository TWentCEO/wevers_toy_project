import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// 메트릭 정의
const cacheReadLatency = new Trend('product_cache_read_latency_ms');
const errorRate = new Rate('product_cache_error_rate');

// CQRS Cache-Aside 성능 테스트 (최대 5,000 TPS 부하)
export const options = {
    scenarios: {
        cache_high_throughput: {
            executor: 'constant-arrival-rate',
            rate: 3000,          // 초당 3,000 요청 (3,000 TPS)
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 300,
            maxVUs: 1000,
        },
    },
    thresholds: {
        'product_cache_read_latency_ms': ['p(95)<30', 'p(99)<50'], // Cache Hit 시 p95 < 30ms 유지
        'product_cache_error_rate': ['rate<0.001'],                // 에러율 0.1% 미만
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';
const PRODUCT_ID = 1;

export default function () {
    // 1. 상품 상세 조회 (GET /api/v1/products/{id})
    const res = http.get(`${BASE_URL}/api/v1/products/${PRODUCT_ID}`);
    cacheReadLatency.add(res.timings.duration);

    const isOk = check(res, {
        'Status is 200': (r) => r.status === 200,
        'Price is correct': (r) => JSON.parse(r.body).price !== undefined,
    });

    if (!isOk) {
        errorRate.add(1);
    }
}
