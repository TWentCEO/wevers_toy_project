"""
Multi-Agent Orchestrator using Google Antigravity SDK
Spawns actual independent Subagents (@Engineer, @SRE, @TechWriter) as separate agent instances.
"""
import os
import sys
import glob
import asyncio
from pathlib import Path
from dotenv import load_dotenv

# Auto-detect and include .venv site-packages if running outside virtualenv
project_root = Path(__file__).resolve().parent.parent
venv_site_packages = glob.glob(str(project_root / ".venv" / "lib" / "python*" / "site-packages"))
if venv_site_packages and venv_site_packages[0] not in sys.path:
    sys.path.insert(0, venv_site_packages[0])

load_dotenv(project_root / ".env.local")
load_dotenv(project_root / ".env")
load_dotenv()

try:
    from google.antigravity import Agent, LocalAgentConfig, types
except ModuleNotFoundError as e:
    print("❌ [Import Error] google-antigravity SDK is not installed in the current environment.")
    print("Please install dependencies using: .venv/bin/pip install google-antigravity python-dotenv")
    sys.exit(1)

# Verify API key
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")

async def run_engineer_subagent(task_prompt: str):
    """
    Subagent: @Engineer
    Responsible for Java / Spring Boot 3 / Gradle / Modular Monolith domain logic.
    """
    print("\n[🚀 Spawning Subagent: @Engineer] ...")
    engineer_config = LocalAgentConfig(
        model="gemini-3.5-flash-lite",
        system_instructions=(
            "You are @Engineer, a senior backend developer specializing in Spring Boot 3, Java 17, and Modular Monolith.\n"
            "You strictly follow AGENTS.md.\n"
            "Rules:\n"
            "1. Controller -> Service -> Repository layered dependency.\n"
            "2. Never return JPA Entity in API, use DTO.\n"
            "3. Concurrency control core logic MUST only contain TODO comment: '// TODO: [학습자 주도 영역] 논리 적용 필요'.\n"
            "Deliver clean, high-concurrency-ready Java code."
        ),
        capabilities=types.CapabilitiesConfig(
            agent_behavior=types.AgentBehavior.AUTONOMOUS,
            enable_subagents=False
        ),
        api_key=GEMINI_API_KEY
    )

    async with Agent(engineer_config) as agent:
        response = await agent.chat(task_prompt)
        content = await response.text()
        print("\n[@Engineer Output]:")
        print(content)
        return content

async def run_sre_subagent(task_prompt: str):
    """
    Subagent: @SRE
    Responsible for Docker Compose, MySQL, Redis, Kafka, and CI/CD pipelines.
    """
    print("\n[🚀 Spawning Subagent: @SRE] ...")
    sre_config = LocalAgentConfig(
        model="gemini-3.5-flash-lite",
        system_instructions=(
            "You are @SRE, a DevOps / Site Reliability Engineer specializing in Docker, MySQL, Redis, Kafka, and GitHub Actions.\n"
            "Ensure high availability, non-root container security, and robust health checks."
        ),
        capabilities=types.CapabilitiesConfig(
            agent_behavior=types.AgentBehavior.AUTONOMOUS,
            enable_subagents=False
        ),
        api_key=GEMINI_API_KEY
    )

    async with Agent(sre_config) as agent:
        response = await agent.chat(task_prompt)
        content = await response.text()
        print("\n[@SRE Output]:")
        print(content)
        return content

async def run_techwriter_subagent(task_prompt: str):
    """
    Subagent: @TechWriter
    Responsible for architecture documentation and TIL (Today I Learned).
    """
    print("\n[🚀 Spawning Subagent: @TechWriter] ...")
    writer_config = LocalAgentConfig(
        model="gemini-3.5-flash-lite",
        system_instructions=(
            "You are @TechWriter, a technical documentation specialist.\n"
            "Explain architectural rationale (Modular Monolith, EDA, CQRS) clearly for backend architects."
        ),
        capabilities=types.CapabilitiesConfig(
            agent_behavior=types.AgentBehavior.AUTONOMOUS,
            enable_subagents=False
        ),
        api_key=GEMINI_API_KEY
    )

    async with Agent(writer_config) as agent:
        response = await agent.chat(task_prompt)
        content = await response.text()
        print("\n[@TechWriter Output]:")
        print(content)
        return content

async def run_performance_architect_subagent(task_prompt: str):
    """
    Subagent: @PerformanceArchitect
    Responsible for analyzing high-traffic performance standards, SLO/SLI metrics,
    I/O bottlenecks (IOPS, Connection Pool, Cache latency), domain benchmarks,
    and generating Mermaid visualization charts.
    """
    print("\n[🚀 Spawning Subagent: @PerformanceArchitect] ...")
    perf_config = LocalAgentConfig(
        model="gemini-3.5-flash-lite",
        system_instructions=(
            "You are @PerformanceArchitect, a Principal Performance Architect and SRE Reliability Specialist.\n"
            "Your mission is to analyze and visualize large-scale traffic performance based on:\n"
            "1. International Standards (ISO/IEC 25010 Performance Efficiency, Little's Law, Universal Scalability Law).\n"
            "2. SLO / SLI / SLA Hierarchies (Google SRE 4 Golden Signals: Latency p95/p99, Traffic TPS, Error Rate, Saturation).\n"
            "3. I/O & System Bottleneck Metrics (Storage IOPS, Network Throughput, HikariCP Connection Pool wait time, Redis p99 command latency, Kafka lag).\n"
            "4. Domain-specific Benchmarks (Ticketing / Flash-sale: 1,000~10,000 TPS, zero overselling, max queue wait time).\n"
            "5. Visualizations: Always provide clear Markdown comparison tables and Mermaid diagrams for architecture bottlenecks and metric flows."
        ),
        capabilities=types.CapabilitiesConfig(
            agent_behavior=types.AgentBehavior.AUTONOMOUS,
            enable_subagents=False
        ),
        api_key=GEMINI_API_KEY
    )

    async with Agent(perf_config) as agent:
        response = await agent.chat(task_prompt)
        content = await response.text()
        print("\n[@PerformanceArchitect Output]:")
        print(content)
        return content

async def main():
    if not GEMINI_API_KEY:
        print("❌ Error: GEMINI_API_KEY environment variable is not set.")
        print("Please export GEMINI_API_KEY='your_api_key' or set it in .env file.")
        sys.exit(1)

    print("\n" + "="*70)
    print("🏢 [Weverse Engineering Team] Multi-Agent Live Standup Meeting")
    print("="*70)

    # 1. @Engineer Subagent
    print("\n----------------------------------------------------------------------")
    print("👨‍💻 [1/4] @Engineer (Spring Boot / Java 17 Backend Specialist)")
    print("----------------------------------------------------------------------")
    engineer_task = (
        "선착순 대기열(Queue) 모듈을 Redis Sorted Set(ZSET)으로 구현할 때, "
        "대기열 진입(ZADD)과 순번 확인(ZRANK), 활성화 승급 로직의 핵심 자바 구현 전략을 2줄로 명쾌하게 보고하라."
    )
    engineer_output = await run_engineer_subagent(engineer_task)

    # 2. @SRE Subagent
    print("\n----------------------------------------------------------------------")
    print("🛠️ [2/4] @SRE (Infrastructure & Site Reliability Specialist)")
    print("----------------------------------------------------------------------")
    sre_task = (
        "대기열에 10만 명의 토큰이 쌓일 때, Redis 메모리 사용량 계산과 "
        "인프라 다운을 방지하기 위한 maxmemory-policy 및 Docker 튜닝 기준을 2줄로 보고하라."
    )
    sre_output = await run_sre_subagent(sre_task)

    # 3. @PerformanceArchitect Subagent
    print("\n----------------------------------------------------------------------")
    print("📊 [3/4] @PerformanceArchitect (Performance & SLO Specialist)")
    print("----------------------------------------------------------------------")
    perf_task = (
        "대기열 진입(POST /queue/enter)과 순번 폴링(GET /queue/status) API의 "
        "목표 SLO(p95 < 50ms)와 네트워크/커넥션 풀 병목 방어 기준을 2줄로 보고하라."
    )
    perf_output = await run_performance_architect_subagent(perf_task)

    # 4. @TechWriter Subagent
    print("\n----------------------------------------------------------------------")
    print("📝 [4/4] @TechWriter (Technical Documentation Specialist)")
    print("----------------------------------------------------------------------")
    writer_task = (
        f"[@Engineer 보고]:\n{engineer_output}\n\n"
        f"[@SRE 보고]:\n{sre_output}\n\n"
        f"[@PerformanceArchitect 보고]:\n{perf_output}\n\n"
        "위 3명의 전문 에이전트 보고를 종합하여, '🚀 [스탠드업 브리프] 선착순 대기열 구현 킥오프 합의안'을 "
        "깔끔한 3줄 마크다운 리포트로 작성하라."
    )
    writer_output = await run_techwriter_subagent(writer_task)

    print("\n" + "="*70)
    print("🎉 All 4 Subagents (@Engineer, @SRE, @PerformanceArchitect, @TechWriter) Executed Live!")
    print("="*70 + "\n")

if __name__ == "__main__":
    asyncio.run(main())



