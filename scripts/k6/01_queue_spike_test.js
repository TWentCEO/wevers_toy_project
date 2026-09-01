import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// 메트릭 정의
const enterQueueLatency = new Trend('queue_enter_latency_ms');
const statusPollLatency = new Trend('queue_status_latency_ms');
const errorRate = new Rate('queue_error_rate');
const activeTokensCount = new Counter('queue_active_tokens_count');

// 테스트 시나리오 (스파이크 부하: 0 -> 2,000 VUs 급증)
export const options = {
    scenarios: {
        spike_queue_traffic: {
            executor: 'ramping-arrival-rate',
            startRate: 50,
            timeUnit: '1s',
            preAllocatedVUs: 500,
            maxVUs: 3000,
            stages: [
                { target: 200, duration: '10s' },  // 워밍업
                { target: 2000, duration: '20s' }, // 0.1초 만에 2,000 TPS 스파이크 급증!
                { target: 2000, duration: '30s' }, // 피크 유지
                { target: 0, duration: '10s' },    // 쿨다운
            ],
        },
    },
    thresholds: {
        'queue_enter_latency_ms': ['p(95)<100', 'p(99)<200'],
        'queue_status_latency_ms': ['p(95)<50', 'p(99)<100'],
        'queue_error_rate': ['rate<0.01'], // 에러율 1% 미만
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';
const PRODUCT_ID = 1;

export default function () {
    const userId = Math.floor(Math.random() * 1000000) + 1;

    // 1. 대기열 진입 요청 (POST /api/v1/queue/enter)
    const enterPayload = JSON.stringify({
        userId: userId,
        productId: PRODUCT_ID,
    });

    const enterParams = {
        headers: { 'Content-Type': 'application/json' },
    };

    const enterRes = http.post(`${BASE_URL}/api/v1/queue/enter`, enterPayload, enterParams);
    enterQueueLatency.add(enterRes.timings.duration);

    const isEnterOk = check(enterRes, {
        'Queue enter status is 200': (r) => r.status === 200,
        'Token is present': (r) => JSON.parse(r.body).token !== undefined,
    });

    if (!isEnterOk) {
        errorRate.add(1);
        return;
    }

    const token = JSON.parse(enterRes.body).token;
    sleep(0.5);

    // 2. 대기열 실시간 순번 폴링 (GET /api/v1/queue/status)
    const statusParams = {
        headers: { 'Queue-Token': token },
    };

    const statusRes = http.get(`${BASE_URL}/api/v1/queue/status?token=${token}&productId=${PRODUCT_ID}`, statusParams);
    statusPollLatency.add(statusRes.timings.duration);

    const isStatusOk = check(statusRes, {
        'Queue status is 200': (r) => r.status === 200,
    });

    if (!isStatusOk) {
        errorRate.add(1);
    } else {
        const body = JSON.parse(statusRes.body);
        if (body.status === 'ACTIVE') {
            activeTokensCount.add(1);
        }
    }
}
