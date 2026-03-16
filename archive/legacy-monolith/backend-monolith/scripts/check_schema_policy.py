#!/usr/bin/env python3
"""
Validate migration-first schema policy for non-local deployment profiles.

This check is intentionally static and fast so it can run early in CI before
full test jobs.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

UNSAFE_DDL_MODES = {"update", "create", "create-drop"}
REQUIRED_NON_LOCAL_PROFILES = ("staging", "prod")


def normalize(value: str | None) -> str:
    return (value or "").strip().strip("'\"").lower()


def resolve_placeholder(value: str | None) -> str | None:
    if value is None:
        return None
    trimmed = value.strip()
    with_default = re.fullmatch(r"\$\{[^:}]+:([^}]*)\}", trimmed)
    if with_default:
        return with_default.group(1).strip()
    no_default = re.fullmatch(r"\$\{[^}]+\}", trimmed)
    if no_default:
        return None
    return trimmed


def parse_properties(path: Path) -> dict[str, str | None]:
    data: dict[str, str | None] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" in line:
            key, value = line.split("=", 1)
        elif ":" in line:
            key, value = line.split(":", 1)
        else:
            continue
        data[key.strip()] = resolve_placeholder(value)
    return data


def parse_yaml(path: Path) -> dict[str, str | None]:
    data: dict[str, str | None] = {}
    stack: list[tuple[int, str]] = []

    for raw in path.read_text(encoding="utf-8").splitlines():
        line_without_comment = raw.split("#", 1)[0].rstrip()
        if not line_without_comment.strip():
            continue
        indent = len(line_without_comment) - len(line_without_comment.lstrip(" "))
        content = line_without_comment.strip()
        if ":" not in content:
            continue

        key, _, remainder = content.partition(":")
        key = key.strip()
        value = remainder.strip()

        while stack and indent <= stack[-1][0]:
            stack.pop()

        current_path = [item[1] for item in stack] + [key]
        dotted_path = ".".join(current_path)

        if value == "":
            stack.append((indent, key))
            continue

        data[dotted_path] = resolve_placeholder(value)

    return data


def load_key_values(path: Path) -> dict[str, str | None]:
    suffix = path.suffix.lower()
    if suffix in (".yml", ".yaml"):
        return parse_yaml(path)
    if suffix == ".properties":
        return parse_properties(path)
    return {}


def load_base_config(resources_dir: Path) -> dict[str, str | None]:
    merged: dict[str, str | None] = {}
    for filename in ("application.yml", "application.yaml", "application.properties"):
        path = resources_dir / filename
        if path.exists():
            merged.update(load_key_values(path))
    return merged


def load_profile_overrides(resources_dir: Path, profile: str) -> dict[str, str | None]:
    merged: dict[str, str | None] = {}
    for suffix in (".yml", ".yaml", ".properties"):
        path = resources_dir / f"application-{profile}{suffix}"
        if path.exists():
            merged.update(load_key_values(path))
    return merged


def validate_profile(profile: str, effective_config: dict[str, str | None]) -> list[str]:
    errors: list[str] = []
    ddl_mode = normalize(effective_config.get("spring.jpa.hibernate.ddl-auto"))
    flyway_enabled = normalize(effective_config.get("spring.flyway.enabled"))

    if ddl_mode in UNSAFE_DDL_MODES:
        errors.append(
            f"profile '{profile}' uses unsafe spring.jpa.hibernate.ddl-auto='{ddl_mode}'"
        )

    if flyway_enabled in {"false", "0", "off", "no"}:
        errors.append(f"profile '{profile}' disables spring.flyway.enabled")

    if ddl_mode == "":
        errors.append(
            f"profile '{profile}' does not resolve spring.jpa.hibernate.ddl-auto "
            "(expected migration-first value such as 'validate')"
        )

    if flyway_enabled == "":
        errors.append(
            f"profile '{profile}' does not resolve spring.flyway.enabled "
            "(expected true in non-local profiles)"
        )

    return errors


def main() -> int:
    backend_dir = Path(__file__).resolve().parents[1]
    resources_dir = backend_dir / "src" / "main" / "resources"

    if not resources_dir.exists():
        print(f"[schema-policy] resources directory not found: {resources_dir}")
        return 1

    base_config = load_base_config(resources_dir)
    all_errors: list[str] = []

    print("[schema-policy] validating migration-first policy for non-local profiles")
    for profile in REQUIRED_NON_LOCAL_PROFILES:
        effective = dict(base_config)
        effective.update(load_profile_overrides(resources_dir, profile))
        ddl_mode = normalize(effective.get("spring.jpa.hibernate.ddl-auto")) or "<unset>"
        flyway_enabled = normalize(effective.get("spring.flyway.enabled")) or "<unset>"
        print(
            f"[schema-policy] profile={profile} "
            f"ddl-auto={ddl_mode} flyway.enabled={flyway_enabled}"
        )
        all_errors.extend(validate_profile(profile, effective))

    if all_errors:
        print("[schema-policy] FAILED")
        for error in all_errors:
            print(f"  - {error}")
        return 1

    print("[schema-policy] OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
