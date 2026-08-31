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

async def main():
    if not GEMINI_API_KEY:
        print("❌ Error: GEMINI_API_KEY environment variable is not set.")
        print("Please export GEMINI_API_KEY='your_api_key' or set it in .env file.")
        sys.exit(1)

    print("=================================================================")
    print("🤖 Main Orchestrator: Dispatching tasks to independent Subagents")
    print("=================================================================")

    # Task for @TechWriter: Draft TIL for Weverse architecture
    techwriter_task = (
        "위버스컴퍼니 수준의 대규모 선착순 예매 시스템에 "
        "Modular Monolith, EDA(Kafka), CQRS(Redis)를 도입하는 이유와 설계 배경에 대해 "
        "깔끔한 Markdown 형태의 TIL(Today I Learned) 초안을 작성하라."
    )
    til_content = await run_techwriter_subagent(techwriter_task)

    os.makedirs("docs", exist_ok=True)
    with open("docs/TIL_Weverse_Architecture.md", "w", encoding="utf-8") as f:
        f.write(til_content)
    print("\n✅ Saved TIL to docs/TIL_Weverse_Architecture.md")

if __name__ == "__main__":
    asyncio.run(main())
