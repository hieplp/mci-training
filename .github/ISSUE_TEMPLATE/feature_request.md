---
name: Feature
about: Change mci-core, mci-web, or both
title: "feat: "
labels: enhancement
---

### Problem

### In scope

### Out of scope

### Layers
<!-- Delete any layer this change does not touch. Do not add a Data layer. -->
- **UI** (`mci-web` templates / `app.css`):
- **Core** (`mci-core`, no I/O):
- **Web** (`SumController` / `SumService`):

### Constraints
- **Stack:** Java 21. Core has no runtime dependencies. Web is Spring Boot 3.5 + Thymeleaf.
- **Target files:**
- **Dependencies:** none, unless listed here.

### Acceptance criteria
- [ ] Given …, when …, then …

### Definition of Ready
- [ ] Each touched layer is named. Untouched layers are deleted, not left blank.
- [ ] Non-goals are explicit.
- [ ] Acceptance criteria are testable.
- [ ] Input and secret rules are stated, or this change has neither.
- [ ] Target files are listed.
