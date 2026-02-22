<!--
Sync Impact Report
===================
Version change: N/A → 1.0.0 (initial ratification)
Modified principles: N/A (initial)
Added sections:
  - Core Principles (5): Privacy & Offline-First, Pragmatic Simplicity,
    Performance-Conscious Data Handling, Focused Testing,
    Future-Ready Data Boundaries
  - Technology Stack & Constraints
  - Product Identity
  - Governance
Templates requiring updates:
  - .specify/templates/plan-template.md — ✅ no updates needed
    (Constitution Check section is dynamic, filled per feature)
  - .specify/templates/spec-template.md — ✅ no updates needed
    (generic structure, no principle-specific sections)
  - .specify/templates/tasks-template.md — ✅ no updates needed
    (task categorization is generic, filled per feature)
  - .specify/templates/agent-file-template.md — ✅ no updates needed
    (auto-generated from plans)
Follow-up TODOs: none
-->

# Currere Constitution

## Core Principles

### I. Privacy & Offline-First

All health data MUST stay on-device unless the user explicitly opts
into cloud sync. Health Connect is the single source of truth — never
duplicate or cache health data beyond what is needed for display.

The app MUST be fully functional without network access. A future
backend/sync layer will be additive, never required. No feature may
depend on network availability for its core functionality.

**Rationale**: Running data is personal health data. Users MUST have
full control over where it lives. Offline-first ensures the app
works on runs where there is no connectivity.

### II. Pragmatic Simplicity

No dependency injection framework, no multi-module Gradle setup, no
unnecessary abstractions. This is a personal project — prefer direct,
readable solutions over architectural purity.

MUST maintain clean separation between data, domain, and presentation
layers so a backend module can be added later without major
refactoring. But do not introduce indirection that serves no current
purpose.

No backward compatibility concerns — take advantage of the latest
platform APIs (SDK 36+) where useful.

**Rationale**: Over-engineering a personal project creates maintenance
burden without users to justify it. Clean layers are the minimum
structure needed for future extensibility.

### III. Performance-Conscious Data Handling

All Health Connect reads MUST happen off the main thread. Use
pagination where health data queries can return large result sets.
Cache computed aggregates (pace splits, averages) in memory for
smooth scrolling — but never persist health data beyond the
current session unless required for display.

**Rationale**: Health data queries can be large. A blocked main
thread means dropped frames and a poor user experience on a tool
meant for quick data review.

### IV. Focused Testing

Unit tests MUST cover domain and business logic: pace calculation,
split computation, aggregations, and data transformations.

Do NOT over-test UI composables or trivial mappers. Tests MUST be
meaningful and exercise real logic, not exist as ceremony.

**Rationale**: Testing effort MUST go where bugs cause real damage —
calculation logic. Trivial UI tests break on cosmetic changes and
add maintenance cost without catching real defects.

### V. Future-Ready Data Boundaries

Define a clear boundary between DTOs and domain models. The data
layer MUST be structured so a REST/gRPC backend can be added later
for a web dashboard and AI analysis features without rewriting
domain or presentation code.

This does NOT mean building the abstraction now — it means not making
choices that preclude it (e.g., leaking Health Connect types into
the presentation layer).

**Rationale**: A backend is planned but not yet needed. The
architecture MUST leave the door open without paying the cost of
walking through it prematurely.

## Technology Stack & Constraints

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Async**: Coroutines and Flow
- **Minimum SDK**: 36 (Android 16)
- **Data source**: Health Connect API
- **Accessibility**: Follow Material 3 accessibility guidelines.
  All charts MUST have text alternatives.

## Product Identity

Currere is a **pure data viewer** for running activity. It presents
Health Connect running data clearly and accurately.

The app MUST NOT include:
- Calorie counts, heart points, or fitness scores
- Badges, streaks, achievements, or gamification of any kind
- Encouragement messages, motivational quotes, or coaching prompts

If a feature does not help the user understand their running data,
it does not belong in this app.

## Governance

This constitution is the authoritative reference for all design and
implementation decisions in Currere. When a feature specification,
implementation plan, or code review conflicts with a principle stated
here, this document takes precedence.

**Amendments**: Any change to this constitution MUST be documented
with a version bump, rationale, and updated date. Principle removals
or redefinitions require a MAJOR version bump. New principles or
materially expanded guidance require a MINOR bump. Clarifications
and wording fixes require a PATCH bump.

**Compliance**: Every feature plan MUST include a Constitution Check
section verifying alignment with these principles before
implementation begins.

**Version**: 1.0.0 | **Ratified**: 2026-02-22 | **Last Amended**: 2026-02-22
