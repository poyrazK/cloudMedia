# ADR-0008: Comments use basic filter + manual moderation

- Status: Accepted
- Date: 2026-03-09

## Context
MVP moderation is manual-first, but comment abuse needs baseline prevention.

## Decision
Use write-time blocked-word filtering plus report-and-review moderation queues. Comments remain editable and revisions are retained for audit.

## Consequences
- Pros: manageable moderation load without heavy ML systems
- Pros: faster MVP delivery with safety baseline
- Cons: limited coverage versus advanced moderation pipelines
- Mitigation: rate limiting, report prioritization, and escalation workflows
