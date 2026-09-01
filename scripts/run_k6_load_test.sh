#!/usr/bin/env bash
set -e

echo "=================================================================="
echo "🚀 [Weverse Ticketing] k6 부하 테스트 & 실시간 Grafana 모니터링"
echo "=================================================================="
echo "📊 Grafana Dashboard: http://localhost:3000 (ID: admin / PW: admin)"
echo "📈 Prometheus:        http://localhost:9090"
echo "🌐 API Server:         http://localhost:8080"
echo "=================================================================="

# 1. 서버 실행 여부 확인 (Health Check)
echo "🔍 1. 스프링 부트 서버(http://localhost:8080) 연결 상태 확인 중..."
if ! curl -s --max-time 3 http://localhost:8080/actuator/health > /dev/null; then
    echo "⚠️ [주의] 스프링 부트 애플리케이션(8080)이 아직 실행되지 않았습니다!"
    echo "💡 새 터미널 창에서 './gradlew bootRun'을 먼저 실행해주세요."
    exit 1
fi
echo "✅ 스프링 부트 서버가 정상 가동 중입니다!"

# 2. k6 실행
if command -v k6 &> /dev/null; then
    echo "▶️ 2. k6 로컬 실행 (scripts/k6/01_queue_spike_test.js)..."
    k6 run scripts/k6/01_queue_spike_test.js
else
    echo "▶️ 2. k6 Docker 컨테이너를 통해 부하 테스트를 실행합니다..."
    docker run --rm -i --add-host=host.docker.internal:host-gateway \
        -e TARGET_URL="http://host.docker.internal:8080" \
        grafana/k6 run - < scripts/k6/01_queue_spike_test.js
fi

echo "=================================================================="
echo "🎉 k6 부하 테스트 완료! Grafana(http://localhost:3000)에서 실시간 지표를 확인하세요."
echo "=================================================================="
