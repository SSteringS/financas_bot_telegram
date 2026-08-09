#!/usr/bin/env python3
"""
Validates the basic structure and best practices of a Skill.

Usage:
    python scripts/validate_skill.py <skill-directory>

Example:
    python scripts/validate_skill.py .

Section policy modes
--------------------
The official Claude Code Skills spec (docs/claude/skills/01-skills.md) only
requires a valid YAML frontmatter with `name` and `description` and a
Markdown body. Section headings inside the body are conventions, not part
of the spec.

This validator supports two policy modes:

- `v2` (default): No section heading is required. But for every heading in
  RECOMMENDED_SECTIONS that appears in SKILL.md, an
  `<!-- inclusion-reason: ... -->` HTML comment must appear inside the
  section (between the heading and the next `## ` heading). For every
  recommended heading that is absent, an
  `<!-- omission-reason: ... -->` comment mentioning that section title
  must appear somewhere in the file. This makes every inclusion and every
  omission an explicit, auditable choice.

- `legacy`: Opt-in via the marker `<!-- section-policy: legacy -->`
  anywhere in SKILL.md. Applies the older rule that all 10 sections listed
  in LEGACY_REQUIRED_SECTIONS must be present. Used for skills authored
  before v2 policy existed; new skills should not use this mode.

Basic checks (frontmatter, name, description, 500-line limit, backslashes,
link targets, ToC-on-long-files) run regardless of policy mode.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MAX_NAME_LENGTH = 64
MAX_DESCRIPTION_LENGTH = 1024
MAX_SKILL_LINES = 500

NAME_PATTERN = re.compile(r"^[a-z0-9-]+$")
MARKDOWN_LINK_PATTERN = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
HEADING_PATTERN = re.compile(r"^##\s+(.+?)\s*$")
POLICY_LEGACY_PATTERN = re.compile(r"<!--\s*section-policy:\s*legacy\s*-->", re.IGNORECASE)
INCLUSION_REASON_PATTERN = re.compile(r"<!--\s*inclusion-reason:", re.IGNORECASE)

RESERVED_WORDS = ("anthropic", "claude")

LEGACY_REQUIRED_SECTIONS = (
    "Objective",
    "When to use",
    "When not to use",
    "Minimum required data",
    "If information is missing",
    "Mandatory process",
    "Mandatory rules",
    "Stop criteria",
    "Completion criteria",
    "Output format",
)

RECOMMENDED_SECTIONS = (
    "Objective",
    "When to use",
    "When not to use",
    "Minimum required data",
    "If information is missing",
    "Mandatory process",
    "Mandatory rules",
    "Additional resources",
    "Stop criteria",
    "Completion criteria",
    "Output format",
)


def print_result(title: str, items: list[str]) -> None:
    print(title)
    for item in items:
        print(f"- {item}")


def extract_frontmatter(content: str) -> tuple[str | None, str | None, list[str]]:
    errors: list[str] = []

    if not content.startswith("---\n"):
        return None, None, ["SKILL.md: missing or invalid YAML frontmatter."]

    parts = content.split("---", 2)

    if len(parts) < 3:
        return None, None, ["SKILL.md: YAML frontmatter closing not found."]

    metadata: dict[str, str] = {}

    for line in parts[1].strip().splitlines():
        if ":" not in line:
            continue

        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip().strip('"').strip("'")

    return metadata.get("name"), metadata.get("description"), errors


def validate_name(name: str) -> list[str]:
    errors: list[str] = []

    if len(name) > MAX_NAME_LENGTH:
        errors.append(
            f"name: has {len(name)} characters; maximum allowed: {MAX_NAME_LENGTH}."
        )

    if not NAME_PATTERN.fullmatch(name):
        errors.append("name: use only lowercase letters, numbers, and hyphens.")

    for reserved_word in RESERVED_WORDS:
        if reserved_word in name:
            errors.append(
                f"name: must not contain the reserved word '{reserved_word}'."
            )

    return errors


def validate_description(description: str) -> list[str]:
    errors: list[str] = []
    lowered = description.lower()

    if not description.strip():
        errors.append("description: must not be empty.")

    if len(description) > MAX_DESCRIPTION_LENGTH:
        errors.append(
            "description: has "
            f"{len(description)} characters; maximum allowed: "
            f"{MAX_DESCRIPTION_LENGTH}."
        )

    if "<" in description or ">" in description:
        errors.append("description: must not contain XML tags.")

    first_person_terms = (" i ", " my ", " mine ", " me ")

    for term in first_person_terms:
        if term in f" {lowered} ":
            errors.append(
                "description: must be in the third person; avoid first person."
            )
            break

    return errors


def detect_policy(content: str) -> str:
    if POLICY_LEGACY_PATTERN.search(content):
        return "legacy"
    return "v2"


def find_headings(lines: list[str]) -> list[tuple[int, str]]:
    positions: list[tuple[int, str]] = []
    for idx, line in enumerate(lines):
        match = HEADING_PATTERN.match(line)
        if match:
            positions.append((idx, match.group(1).strip()))
    return positions


def find_omission_reason(content: str, section_title: str) -> bool:
    pattern = re.compile(
        r"<!--\s*omission-reason:.*?"
        + re.escape(section_title)
        + r".*?-->",
        re.DOTALL | re.IGNORECASE,
    )
    return bool(pattern.search(content))


def validate_v2_sections(content: str) -> list[str]:
    errors: list[str] = []
    lines = content.splitlines()
    headings = find_headings(lines)
    present_titles = {title for _, title in headings}

    for section in RECOMMENDED_SECTIONS:
        if section in present_titles:
            for i, (idx, title) in enumerate(headings):
                if title != section:
                    continue

                end = headings[i + 1][0] if i + 1 < len(headings) else len(lines)
                block = "\n".join(lines[idx + 1 : end])

                if not INCLUSION_REASON_PATTERN.search(block):
                    errors.append(
                        f"SKILL.md: section '## {section}' is present but is missing "
                        "an '<!-- inclusion-reason: ... -->' comment inside it."
                    )
                break
        else:
            if not find_omission_reason(content, section):
                errors.append(
                    f"SKILL.md: recommended section '## {section}' is absent and no "
                    f"'<!-- omission-reason: ... {section} ... -->' comment was found."
                )

    return errors


def validate_legacy_sections(content: str) -> list[str]:
    errors: list[str] = []
    for section in LEGACY_REQUIRED_SECTIONS:
        if f"## {section}" not in content:
            errors.append(
                f"SKILL.md (legacy policy): missing required section: '## {section}'."
            )
    return errors


def validate_skill_content(content: str) -> list[str]:
    errors: list[str] = []
    line_count = len(content.splitlines())

    if line_count > MAX_SKILL_LINES:
        errors.append(
            f"SKILL.md: has {line_count} lines; recommended maximum: "
            f"{MAX_SKILL_LINES}."
        )

    if "\\" in content:
        errors.append(
            "SKILL.md: contains a backslash. Use '/' paths instead of '\\'."
        )

    policy = detect_policy(content)

    if policy == "legacy":
        errors.extend(validate_legacy_sections(content))
    else:
        errors.extend(validate_v2_sections(content))

    return errors


def validate_markdown_files(skill_dir: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    for markdown_file in skill_dir.rglob("*.md"):
        content = markdown_file.read_text(encoding="utf-8")
        relative_path = markdown_file.relative_to(skill_dir).as_posix()
        line_count = len(content.splitlines())

        if "\\" in content:
            errors.append(
                f"{relative_path}: contains a backslash; use '/' paths."
            )

        if relative_path != "SKILL.md" and line_count > 100:
            has_contents_heading = (
                "## Table of contents" in content
                or "## Contents" in content
                or "## Índice" in content
            )

            if not has_contents_heading:
                warnings.append(
                    f"{relative_path}: has more than 100 lines and no table of contents."
                )

        for link_target in MARKDOWN_LINK_PATTERN.findall(content):
            if link_target.startswith(("http://", "https://", "#")):
                continue

            if "\\" in link_target:
                errors.append(
                    f"{relative_path}: reference uses '\\': '{link_target}'."
                )

            target_path = (markdown_file.parent / link_target).resolve()

            if not target_path.exists():
                warnings.append(
                    f"{relative_path}: reference not found: '{link_target}'."
                )

    return errors, warnings


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python scripts/validate_skill.py <skill-directory>")
        return 2

    skill_dir = Path(sys.argv[1]).resolve()
    skill_file = skill_dir / "SKILL.md"

    if not skill_dir.exists():
        print_result("VALIDATION FAILED", [f"Directory not found: {skill_dir}"])
        return 1

    if not skill_dir.is_dir():
        print_result(
            "VALIDATION FAILED",
            [f"The provided path is not a directory: {skill_dir}"],
        )
        return 1

    if not skill_file.exists():
        print_result("VALIDATION FAILED", ["Required file not found: SKILL.md"])
        return 1

    content = skill_file.read_text(encoding="utf-8")
    name, description, errors = extract_frontmatter(content)

    if name:
        errors.extend(validate_name(name))

    if description:
        errors.extend(validate_description(description))

    errors.extend(validate_skill_content(content))

    markdown_errors, warnings = validate_markdown_files(skill_dir)
    errors.extend(markdown_errors)

    policy = detect_policy(content)

    if errors:
        print_result("VALIDATION FAILED", errors)

        if warnings:
            print_result("VALIDATION WARNINGS", warnings)

        return 1

    print("VALIDATION PASSED")
    print(f"- Skill: {name}")
    print(f"- Directory: {skill_dir}")
    print(f"- Section policy: {policy}")

    if warnings:
        print_result("VALIDATION WARNINGS", warnings)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
