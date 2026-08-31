#!/usr/bin/env python3
import sys
import json
import re

def main():
    try:
        input_data = json.load(sys.stdin)
    except Exception:
        print(json.dumps({"decision": "allow"}))
        return

    tool_call = input_data.get("toolCall", {})
    name = tool_call.get("name", "")
    args = tool_call.get("args", {})

    # Check target file names and content in write_to_file, replace_file_content, multi_replace_file_content
    target_file = args.get("TargetFile", "")
    code_content = args.get("CodeContent", "")
    replacement_content = args.get("ReplacementContent", "")
    instruction = args.get("Instruction", "")
    description = args.get("Description", "")

    # Keywords to intercept: Lock, Redis, Kafka, 주문
    keywords = ["Lock", "Redis", "Kafka", "주문"]

    is_intercepted = False
    reasons = []

    # 1. *Service.java 파일 수정 감지
    if target_file.endswith("Service.java") or "*Service.java" in target_file:
        is_intercepted = True
        reasons.append(f"Service 파일({target_file}) 수정 감지")

    # 2. 키워드 감지 (코드 내용, 지시사항 등)
    text_to_check = f"{instruction} {description} {code_content} {replacement_content}"
    for kw in keywords:
        if re.search(re.escape(kw), text_to_check, re.IGNORECASE):
            is_intercepted = True
            reasons.append(f"핵심 키워드 '{kw}' 감지")

    if is_intercepted:
        output = {
            "decision": "force_ask",
            "reason": "⚠️ [학습 주도권 방어] 핵심 비즈니스 로직 수정이 감지되었습니다. 진행을 위해 유저가 논리를 제시하고 명시적으로 승인(Approve)해야 합니다."
        }
        print(json.dumps(output))
    else:
        print(json.dumps({"decision": "allow"}))

if __name__ == "__main__":
    main()
