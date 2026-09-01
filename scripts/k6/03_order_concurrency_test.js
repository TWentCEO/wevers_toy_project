import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// 메트릭 정의
const orderLatency = new Trend('order_post_latency_ms');
const successOrders = new Counter('orders_success_202_count');
const soldOutOrders = new Counter('orders_sold_out_500_or_400_count');

// 선착순 한정판 주문 동시성 부하 테스트 (1,000건 동시 인입)
export const options = {
    scenarios: {
        instant_order_burst: {
            executor: 'per-vu-iterations',
            vus: 1000,           // 1,000명의 유저가
            iterations: 1,       // 1번씩 동시에 주문 버튼을 누름!
            maxDuration: '15s',
        },
    },
    thresholds: {
        'order_post_latency_ms': ['p(95)<150', 'p(99)<300'],
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';
const PRODUCT_ID = 1;

export default function () {
    const userId = __VU; // 가상 유저 번호 (1 ~ 1000)

    // 1. 대기열 진입하여 토큰 발급
    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, JSON.stringify({
        userId: userId,
        productId: PRODUCT_ID,
    }), { headers: { 'Content-Type': 'application/json' } });

    if (enterRes.status !== 200) return;
    const token = JSON.parse(enterRes.body).token;

    // 2. 선착순 주문 요청 (POST /api/v1/orders)
    const orderPayload = JSON.stringify({
        userId: userId,
        productId: PRODUCT_ID,
        quantity: 1,
    });

    const orderParams = {
        headers: {
            'Content-Type': 'application/json',
            'Queue-Token': token,
        },
    };

    const orderRes = http.post(`${BASE_URL}/api/v1/orders`, orderPayload, orderParams);
    orderLatency.add(orderRes.timings.duration);

    if (orderRes.status === 202) {
        successOrders.add(1);
    } else {
        soldOutOrders.add(1);
    }

    check(orderRes, {
        'Order response is 202 or Handled Failure': (r) => r.status === 202 || r.status >= 400,
    });
}
