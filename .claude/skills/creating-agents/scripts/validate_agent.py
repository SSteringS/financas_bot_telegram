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

# Skill reachability (see validate_skill_reachability).
# A skill mention only counts when it is written as an identifier -- inside
# backticks -- which is how this repo names skills in agent prose. Bare prose
# words are ignored on purpose: a false positive here trains readers to ignore
# the validator, and the cost of missing one mention is lower.
SKILL_MENTION_PATTERN = re.compile(r"`([A-Za-z0-9][A-Za-z0-9_-]*)`")
FENCED_BLOCK_PATTERN = re.compile(r"^```.*?^```", re.MULTILINE | re.DOTALL)
BLOCK_SEQUENCE_ITEM_PATTERN = re.compile(r"(?:^|\s)-\s+")
SKILL_TOOL_NAME = "Skill"


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


def parse_yaml_sequence(value: str | None) -> list[str]:
    """Parse a frontmatter list in inline OR block form.

    `extract_frontmatter` collapses a block sequence onto one line, so
    `skills:\\n  - a\\n  - b` arrives here as `- a - b`. Splitting that on
    commas (as `parse_yaml_list` does) would return a single bogus entry, and
    splitting on a bare '-' would shred hyphenated skill names, so block form
    is split on a dash followed by whitespace.
    """
    if value is None:
        return []

    stripped = value.strip()

    if not stripped:
        return []

    if stripped.startswith("-"):
        entries = [
            entry.strip().strip("'\"")
            for entry in BLOCK_SEQUENCE_ITEM_PATTERN.split(stripped)
        ]
        return [entry for entry in entries if entry]

    return parse_yaml_list(stripped)


def find_skills_dir(agent_file: Path) -> Path | None:
    """Locate `.claude/skills/` by walking up from the agent file.

    Falls back to the invocation directory (the script is run from the repo
    root today) and then to the script's own location, so a validated agent
    file living outside the repo still resolves the repo's skill catalog.
    """
    starts = [
        agent_file.resolve().parent,
        Path.cwd().resolve(),
        Path(__file__).resolve().parent,
    ]

    for start in starts:
        for directory in [start, *start.parents]:
            candidate = directory / ".claude" / "skills"
            if candidate.is_dir():
                return candidate

    return None


def discover_skill_names(skills_dir: Path | None) -> set[str]:
    if skills_dir is None:
        return set()

    return {
        entry.name
        for entry in skills_dir.iterdir()
        if entry.is_dir() and (entry / "SKILL.md").is_file()
    }


def validate_skill_reachability(
    agent_file: Path, metadata: dict[str, str], body: str
) -> list[str]:
    """Catch skills the agent's prose delegates to but cannot reach.

    Two independent mechanisms deliver a skill to a subagent: preload, via the
    `skills:` frontmatter list, and on demand, via the `Skill` tool. `skills:`
    is preload only, never access control. `tools:`, when present, is an
    exhaustive allowlist; when absent the agent inherits every built-in tool,
    `Skill` included. So a referenced skill is unreachable only when it is
    absent from `skills:` AND `tools:` is present without `Skill`. Claude Code
    raises no error in that case -- the delegation just never happens.
    """
    known_skills = discover_skill_names(find_skills_dir(agent_file))

    if not known_skills:
        return []

    preloaded = set(parse_yaml_sequence(metadata.get("skills")))

    tools_value = metadata.get("tools")
    tools_declared = tools_value is not None and tools_value.strip() != ""

    if not tools_declared:
        # No `tools:` field: all built-in tools are inherited, `Skill` included.
        return []

    tool_entries = {entry.lower() for entry in parse_yaml_sequence(tools_value)}

    if SKILL_TOOL_NAME.lower() in tool_entries:
        return []

    prose = FENCED_BLOCK_PATTERN.sub("", body)
    mentioned = set(SKILL_MENTION_PATTERN.findall(prose)) & known_skills
    unreachable = sorted(mentioned - preloaded)

    return [
        f"skills: '{name}' is referenced in the body of {agent_file.name} but is "
        "unreachable: it is not preloaded in 'skills:' and the 'tools:' allowlist "
        f"does not include '{SKILL_TOOL_NAME}'. Fix by adding '{name}' to 'skills:' "
        f"(preload) or by adding '{SKILL_TOOL_NAME}' to 'tools:' (on demand)."
        for name in unreachable
    ]


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

        if metadata is not None:
            errors += validate_skill_reachability(agent_file, metadata, body)

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
