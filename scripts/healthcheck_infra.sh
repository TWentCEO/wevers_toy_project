#!/usr/bin/env bash
set -eo pipefail

echo "====================================================="
echo "🔍 [SRE Health Check] Infrastructure Sync & Health Test"
echo "====================================================="

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

wait_for_service() {
    local service_name=$1
    local check_command=$2
    local max_attempts=${3:-30}
    local attempt=1

    echo -n "⏳ Checking ${service_name} ... "
    until eval "$check_command" > /dev/null 2>&1; do
        if [ $attempt -ge $max_attempts ]; then
            echo -e "${RED}FAILED${NC} (Timeout after ${max_attempts} attempts)"
            return 1
        fi
        sleep 2
        attempt=$((attempt + 1))
        echo -n "."
    done
    echo -e " ${GREEN}HEALTHY${NC}"
    return 0
}

# 1. Check Docker Daemon
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker daemon is not running. Please start Docker first.${NC}"
    exit 1
fi

echo "🚀 Verifying Compose container configurations..."
docker compose config > /dev/null

# 2. Check MySQL
wait_for_service "MySQL (3306)" \
    "docker compose exec -T mysql mysqladmin ping -h localhost -u root -proot_secret_password" 20

# 3. Check Redis
wait_for_service "Redis (6379)" \
    "docker compose exec -T redis redis-cli ping" 15

# 4. Check Zookeeper
wait_for_service "Zookeeper (2181)" \
    "docker compose exec -T zookeeper nc -z localhost 2181" 15

# 5. Check Kafka broker
wait_for_service "Kafka Broker (9092)" \
    "docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092" 25

echo "====================================================="
echo -e "${GREEN}✅ All Backing Services (MySQL, Redis, Zookeeper, Kafka) are in SYNC & HEALTHY!${NC}"
echo "====================================================="
