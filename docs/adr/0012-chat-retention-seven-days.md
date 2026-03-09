# ADR-0012: Chat retention is seven days

- Status: Accepted
- Date: 2026-03-09

## Context
Live chat needs short-term moderation visibility without long-term storage bloat.

## Decision
Retain chat messages for seven days, then purge via scheduled lifecycle jobs.

## Consequences
- Pros: supports recent moderation investigations
- Pros: controls storage and privacy exposure
- Cons: limited long-term forensic history
- Mitigation: preserve moderation action logs separately for longer retention
