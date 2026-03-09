# ADR-0010: Discovery uses balanced ranking

- Status: Accepted
- Date: 2026-03-09

## Context
MVP needs meaningful discovery quality without full ML infrastructure.

## Decision
Use balanced ranking that combines personalized and trending signals. Candidate pools include followed creators, trending, fresh, and similar-tag content.

## Consequences
- Pros: better relevance than purely chronological or purely trending feeds
- Pros: feasible at MVP scale
- Cons: tuning required to avoid bias and repetition
- Mitigation: diversity caps and exploration quotas
