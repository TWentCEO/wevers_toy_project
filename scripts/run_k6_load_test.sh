#!/usr/bin/env bash
set -e

echo "=================================================================="
echo "🚀 [Weverse Ticketing] k6 부하 테스트 & 실시간 Grafana 모니터링"
echo "=================================================================="
echo "📊 Grafana Dashboard: http://localhost:3000 (ID: admin / PW: admin)"
echo "📈 Prometheus:        http://localhost:9090"
echo "🌐 API Server:         http://localhost:8080"
echo "=================================================================="

# k6 설치 여부 확인
if ! command -v k6 &> /dev/null; then
    echo "⚠️ k6가 설치되어 있지 않습니다. Mac의 경우 'brew install k6'로 설치해주세요."
    echo "💡 Docker를 통해 실행합니다..."
    
    docker run --rm -i --network="host" grafana/k6 run - < scripts/k6/01_queue_spike_test.js
else
    echo "▶️ 1. 대기열 스파이크 부하 테스트 실행 (scripts/k6/01_queue_spike_test.js)..."
    k6 run scripts/k6/01_queue_spike_test.js
fi

echo "✅ k6 부하 테스트 완료!"
