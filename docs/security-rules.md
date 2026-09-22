# Security rules

Do not add Spring Security, JPA, or a database. This repository has none. The rules still forbid the unsafe pattern if a prompt asks for it.

## 1. No hardcoded secrets

No API keys, passwords, tokens, or connection strings in source, tests, comments, or prompts. Use environment variables when a spec names a secret. This spec names none.

- **GOOD:** no credential literal in the repo.
- **BAD:** `private static final String TOKEN = "sk-live-...";`

## 2. Validate at the boundary

Do not trust client input. `NumberStrings.requireDigits` rejects null, empty, and non-digits before addition. The page passes `stn1` and `stn2` through that check via `MyBigNumber`.

- **GOOD:** `requireDigits(stn1, "stn1")` before any digit is used.
- **BAD:** `stn1.charAt(0) - '0'` on an unchecked string.

## 3. No string-built queries

No database in this repo. Do not add one. If a spec later names a query, use a parameter bind. Never concatenate client input into SQL or JPQL.

- **GOOD:** no SQL in this repo.
- **BAD:** `"select * from t where id = '" + stn1 + "'"`.

## 4. Authorization

Do not add `@PreAuthorize`, role checks, or Spring Security unless the spec names a role. Inventing `TECHNICIAN` is a hallucination, not a control.

- **GOOD:** `GET /` stays anonymous, matching [api-spec.md](api-spec.md).
- **BAD:** `@PreAuthorize("hasRole('TECHNICIAN')")` on `SumController`.

## 5. Logs and prompts

Do not put secrets or PII (passwords, tokens, emails, phone numbers) in logs, exceptions, prompts, or commits. An operand name and a character index are enough for a rejected digit.

- **GOOD:** `LOG.warn("Invalid operand {0}: non-digit at index {1}", "stn1", index);`
- **BAD:** logging a password, a connection string, or a raw `Authorization` header.
