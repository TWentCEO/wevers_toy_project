"""
Multi-Agent Orchestrator using Google Antigravity SDK
Spawns actual independent Subagents (@Engineer, @SRE, @TechWriter) as separate agent instances.
"""
import os
import sys
import asyncio
from dotenv import load_dotenv

load_dotenv(".env.local")
load_dotenv(".env")
load_dotenv()

from google.antigravity import Agent, LocalAgentConfig, types

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

    print("=================================================================")
    print("🤖 Main Orchestrator: Spawning @PerformanceArchitect Subagent")
    print("=================================================================")

    # Task for @PerformanceArchitect: Produce Performance & SLO Benchmark Report
    perf_task = (
        "위버스컴퍼니 수준의 대규모 선착순 예매 시스템(초당 1,000~10,000건 스파이크 트래픽)을 위한 "
        "1) 국제 표준(ISO/IEC 25010) 기반 성능 평가 체계, "
        "2) 4대 골든 시그널 기반 SLO/SLI 목표치, "
        "3) 핵심 I/O 및 병목 지표(Redis, DB HikariCP, Kafka Lag, Network/Disk), "
        "4) 도메인(선착순 티켓팅) 통용 기준 및 Mermaid 시각화 다이어그램을 포함한 "
        "종합 성능 아키텍처 보고서를 마크다운으로 작성하라."
    )
    perf_report = await run_performance_architect_subagent(perf_task)

    os.makedirs("docs", exist_ok=True)
    with open("docs/04_PERFORMANCE_METRICS_AND_SLO.md", "w", encoding="utf-8") as f:
        f.write(perf_report)
    print("\n✅ Saved Performance Architecture Report to docs/04_PERFORMANCE_METRICS_AND_SLO.md")

    print("\n=================================================================")
    print("🎉 @PerformanceArchitect Successfully Generated Performance & SLO Benchmark Report!")
    print("=================================================================")

if __name__ == "__main__":
    asyncio.run(main())


