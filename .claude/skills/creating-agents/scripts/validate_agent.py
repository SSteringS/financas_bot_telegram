#!/usr/bin/env python3
"""
Validates custom agent files (dual-standard: VS Code Custom Agents + Claude Code subagents).

Usage:
    python scripts/validate_agent.py <agent-file-or-directory>

Example:
    python scripts/validate_agent.py .claude/agents/reviewer.agent.md

Scope
-----
This repo uses a dual-standard convention: agents live in .claude/agents/
using the VS Code Custom Agent format (.agent.md, vendor model, lowercase
tool aliases, `handoffs`) so they appear in BOTH the VS Code picker AND
the Claude Code picker (Claude Code is lenient about unknown fields).

The validator is intentionally permissive:
- Accepts either extension (.agent.md or .md).
- Accepts either model format (VS Code vendor, Claude Code alias, full ID,
  `inherit`, or array of any of the above).
- Accepts either tools format (VS Code lowercase aliases or Claude Code
  uppercase names).
- Accepts VS Code fields (`handoffs`, `user-invocable`, `argument-hint`,
  `target`, `mcp-servers`, `disable-model-invocation`) without warning.
- Accepts Claude Code fields (`permissionMode`, `mcpServers`, `memory`,
  `isolation`, `background`, `effort`, `initialPrompt`, `disallowedTools`,
  `maxTurns`, `skills`, `hooks`, `color`) without warning.

Errors are raised only when a field is invalid in BOTH standards or when a
value violates the enum specified by whichever standard defines it.

References
----------
- Claude Code subagents: docs/claude/agentes/02-subagentes.md
- VS Code Custom Agents: FRONTMATTER-vscode.md
- Dual-standard usage in this repo: FRONTMATTER.md
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MAX_DESCRIPTION_LENGTH = 1024

NAME_PATTERN = re.compile(r"^[A-Za-z0-9_-]+$")
MARKDOWN_LINK_PATTERN = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
TOP_LEVEL_KEY_PATTERN = re.compile(r"^([A-Za-z_][A-Za-z0-9_-]*):\s?(.*)$")

CLAUDE_CODE_MODEL_ALIASES = {"sonnet", "opus", "haiku", "fable", "inherit"}
CLAUDE_CODE_MODEL_ID_PATTERN = re.compile(r"^claude-[a-z0-9-]+$")
VENDOR_MODEL_PATTERN = re.compile(r"^.+\([A-Za-z0-9_-]+\)$")

PERMISSION_MODES = {
    "default",
    "acceptEdits",
    "auto",
    "dontAsk",
    "bypassPermissions",
    "plan",
    "manual",
}

# Source: docs/claude/harness/04-referencia-de-ferramentas.md, the tool table.
# This set mirrors that table one-for-one. When the doc is refreshed from
# upstream, re-sync this set instead of appending ad-hoc names: an allowlist
# that drifts behind the harness produces false-positive warnings, which trains
# readers to ignore the validator entirely.
CLAUDE_CODE_BUILTIN_TOOLS = {
    "Agent", "Artifact", "AskUserQuestion", "Bash",
    "CronCreate", "CronDelete", "CronList",
    "Edit", "EnterPlanMode", "EnterWorktree", "ExitPlanMode", "ExitWorktree",
    "Glob", "Grep", "ListMcpResourcesTool", "LSP", "Monitor", "NotebookEdit",
    "PowerShell", "PushNotification", "Read", "ReadMcpResourceTool",
    "RemoteTrigger", "ReportFindings", "ScheduleWakeup", "SendMessage",
    "SendUserFile", "ShareOnboardingGuide", "Skill",
    "TaskCreate", "TaskGet", "TaskList", "TaskOutput", "TaskStop", "TaskUpdate",
    "TodoWrite", "ToolSearch", "WaitForMcpServers", "WebFetch", "WebSearch",
    "Workflow", "Write",
}

# Retired names kept accepted so agent files written before a rename do not
# start failing. Not in the current tool reference; do not use them in new
# agents. `Task` was the pre-rename name of `Agent`; `SlashCommand` was
# superseded by `Skill` plus the slash-command frontmatter.
LEGACY_TOOL_NAMES = {"Task", "SlashCommand"}

# VS Code Custom Agent lowercase aliases (FRONTMATTER-vscode-archived.md).
VSCODE_TOOL_ALIASES = {
    "read", "write", "edit", "search", "execute", "agent", "web", "todo",
}

KNOWN_TOOL_NAMES = CLAUDE_CODE_BUILTIN_TOOLS | LEGACY_TOOL_NAMES | VSCODE_TOOL_ALIASES

MCP_TOOL_PATTERN = re.compile(r"^(mcp__.+|.+/\*|[A-Za-z0-9_-]+:.+)$")

RECOMMENDED_BODY_SECTIONS = ("Scope", "Output Format")


def print_result(title: str, items: list[str]) -> None:
    print(title)
    for item in items:
        print(f"- {item}")


def extract_frontmatter(content: str) -> tuple[dict[str, str] | None, str, list[str]]:
    lines = content.splitlines()

    if not lines or lines[0].strip() != "---":
        return None, "", ["missing or invalid YAML frontmatter."]

    try:
        end_index = lines[1:].index("---") + 1
    except ValueError:
        return None, "", ["YAML frontmatter closing '---' not found."]

    metadata: dict[str, str] = {}
    current_key: str | None = None

    for line in lines[1:end_index]:
        if not line.strip():
            continue

        if not line[0].isspace():
            match = TOP_LEVEL_KEY_PATTERN.match(line)
            if match:
                current_key = match.group(1)
                metadata[current_key] = match.group(2).strip()
                continue

        if current_key is not None:
            addition = line.strip()
            metadata[current_key] = (metadata[current_key] + " " + addition).strip()

    body = "\n".join(lines[end_index + 1 :])
    return metadata, body, []


def validate_description(description: str | None) -> list[str]:
    errors: list[str] = []

    if not description or not description.strip():
        errors.append("description: is required and must not be empty.")
        return errors

    if len(description) > MAX_DESCRIPTION_LENGTH:
        errors.append(
            "description: has "
            f"{len(description)} characters; keep it concise "
            f"(~{MAX_DESCRIPTION_LENGTH} max)."
        )

    if "<" in description or ">" in description:
        errors.append("description: must not contain XML/HTML tags.")

    return errors


def parse_yaml_list(value: str) -> list[str]:
    stripped = value.strip()

    if not stripped:
        return []

    if stripped.startswith("["):
        inner = stripped.strip("[]")
        entries = [entry.strip().strip("'\"") for entry in inner.split(",")]
        return [entry for entry in entries if entry]

    entries = [entry.strip().strip("'\"") for entry in stripped.split(",")]
    return [entry for entry in entries if entry]


def is_valid_model_entry(entry: str) -> bool:
    entry = entry.strip().strip("'\"")

    if entry in CLAUDE_CODE_MODEL_ALIASES:
        return True
    if CLAUDE_CODE_MODEL_ID_PATTERN.match(entry):
        return True
    if VENDOR_MODEL_PATTERN.match(entry):
        return True
    return False


def validate_model(value: str | None) -> list[str]:
    errors: list[str] = []

    if not value:
        return errors

    entries = parse_yaml_list(value)

    for entry in entries:
        if not is_valid_model_entry(entry):
            errors.append(
                f"model: '{entry}' is not a recognized value. Accepted formats: "
                "Claude Code alias (sonnet, opus, haiku, fable, inherit), "
                "full ID (claude-*), "
                "or vendor format 'Name (vendor)'."
            )

    return errors


def validate_tools(value: str | None) -> list[str]:
    warnings: list[str] = []

    if value is None or value.strip() == "" or value.strip() == "[]":
        return warnings

    entries = parse_yaml_list(value)

    for entry in entries:
        if entry in KNOWN_TOOL_NAMES:
            continue
        if MCP_TOOL_PATTERN.match(entry):
            continue
        warnings.append(
            f"tools: '{entry}' is not a recognized tool name (Claude Code uppercase, "
            "VS Code alias, or MCP pattern). Check docs/claude/harness/04-referencia-de-ferramentas.md "
            "or FRONTMATTER-vscode-archived.md."
        )

    return warnings


def validate_permission_mode(value: str | None) -> list[str]:
    errors: list[str] = []

    if not value:
        return errors

    normalized = value.strip().strip("'\"")

    if normalized not in PERMISSION_MODES:
        errors.append(
            f"permissionMode: '{normalized}' is not valid. Allowed: "
            f"{sorted(PERMISSION_MODES)}."
        )

    return errors


def validate_name(value: str | None) -> list[str]:
    warnings: list[str] = []

    if value and not NAME_PATTERN.fullmatch(value):
        warnings.append("name: prefer letters, numbers, hyphens, or underscores only.")

    return warnings


def validate_body(body: str) -> list[str]:
    warnings: list[str] = []

    for section in RECOMMENDED_BODY_SECTIONS:
        if f"## {section}" not in body and f"### {section}" not in body:
            warnings.append(
                f"body: recommended section missing: '{section}' "
                "(explicit scope / output format)."
            )

    if "\\" in body:
        warnings.append("body: contains a backslash; prefer '/' paths.")

    return warnings


def validate_links(agent_file: Path, body: str) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    for link_target in MARKDOWN_LINK_PATTERN.findall(body):
        if link_target.startswith(("http://", "https://", "#")):
            continue

        if "\\" in link_target:
            errors.append(f"reference uses '\\': '{link_target}'.")

        target_path = (agent_file.parent / link_target).resolve()

        if not target_path.exists():
            warnings.append(f"reference not found: '{link_target}'.")

    return errors, warnings


def validate_file(agent_file: Path) -> tuple[list[str], list[str]]:
    content = agent_file.read_text(encoding="utf-8")
    metadata, body, fm_errors = extract_frontmatter(content)

    errors = list(fm_errors)
    warnings: list[str] = []

    if metadata is not None:
        errors += validate_description(metadata.get("description"))
        errors += validate_model(metadata.get("model"))
        errors += validate_permission_mode(metadata.get("permissionMode"))
        warnings += validate_tools(metadata.get("tools"))
        warnings += validate_name(metadata.get("name"))

    if body:
        warnings += validate_body(body)
        link_errors, link_warnings = validate_links(agent_file, body)
        errors += link_errors
        warnings += link_warnings

    return errors, warnings


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python scripts/validate_agent.py <agent-file-or-directory>")
        return 2

    target = Path(sys.argv[1]).resolve()

    if target.is_dir():
        agent_files = sorted(
            list(target.rglob("*.md")) + list(target.rglob("*.agent.md"))
        )
        agent_files = [f for f in agent_files if "agents" in f.parts]
        # Deduplicate (rglob("*.md") already matches .agent.md too)
        agent_files = sorted(set(agent_files))
        if not agent_files:
            print_result(
                "VALIDATION FAILED",
                [f"No agent files found under: {target}"],
            )
            return 1
    elif target.is_file():
        agent_files = [target]
    else:
        print_result("VALIDATION FAILED", [f"Not found: {target}"])
        return 1

    exit_code = 0

    for agent_file in agent_files:
        errors, warnings = validate_file(agent_file)

        if errors:
            print_result(f"VALIDATION FAILED: {agent_file.name}", errors)
            exit_code = 1
        else:
            print(f"OK: {agent_file.name}")

        if warnings:
            print_result(f"WARNINGS: {agent_file.name}", warnings)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
