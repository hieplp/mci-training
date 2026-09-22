# Contributing

Do not push to `main`. Open a pull request from an issue.

## Branches

Branch, loop, and generation rules are in [docs/coding-rules.md](docs/coding-rules.md).

`feature/<issue>-<desc>`, `fix/<issue>-<desc>`, or `perf/<desc>`.

`docs/requirement.md` asks for the Task 1 snapshot on branch `core` or tag `0.0.1`. Do not use that branch for unrelated work.

## Commits

`type(scope): what changed`

Types already in this repo: `feat`, `fix`, `docs`, `refactor`, `perf`. Scope is `core` or `web` when one module changes. Omit the scope for repo-wide docs.

## Pull requests

1. Use the template. `Closes #<n>` needs the GitHub issue number, not a title prefix.
2. `./gradlew test` must pass in every module you touch. On Windows, `gradlew.bat test`.
3. A human reads the diff before merge. AI output is untrusted until then.
4. No secrets in the diff or in a Copilot prompt.

## AI

Generation constraints are in `.github/copilot-instructions.md`. Copilot reads that file. Disclose use in the pull request.

There is no database. Do not add one, and do not add a runtime dependency to `mci-core`, unless the issue says so.

Secret exclusion in Copilot is a GitHub repository setting (Copilot Business or Enterprise), not a file in this repo. The control that works here is `.gitignore`: do not commit `.env`, keys, or `secrets/`.
