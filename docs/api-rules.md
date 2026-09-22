# API rules

This repository has no REST API. The spec is [api-spec.md](api-spec.md): library methods `sum` / `sumWithSteps`, and one page, `GET /`, with query fields `stn1` and `stn2` only.

Do not add an endpoint, a JSON body, or a field the spec does not name. The page shows the `IllegalArgumentException` message. It does not return a JSON error document.

The rules below apply only when a spec names an HTTP API. They do not authorize adding one here.

## 1. Resource naming

Plural nouns. No verb in the path. No camelCase path segment.

- **GOOD:** `/api/workorders`
- **BAD:** `/api/workOrder`, `/api/createWorkOrder`

## 2. Verbs

`POST` creates, `GET` reads, `PUT` replaces, `PATCH` updates, `DELETE` removes. This repo's page is `GET /` only.

- **GOOD:** `GET /` for the sum form.
- **BAD:** `POST /api/workorders` added because a prompt asked for it and the spec did not.

## 3. Schema

Do not invent JSON fields or query parameters. Here the only query names are `stn1` and `stn2`.

- **GOOD:** a handler that accepts only fields the spec lists.
- **BAD:** adding `traceId`, `success`, or `workOrderId` because they look useful.

## 4. Errors (RFC 7807)

When a spec names a JSON error, return Problem Details and only these fields: `type`, `title`, `status`, `detail`. No stack trace. No extra property.

- **GOOD:** `400` with `type`, `title`, `status`, `detail`.
- **BAD:** a raw string, `e.printStackTrace()`, or a body that adds `timestamp` and `errors[]` the spec did not name.

This page does not use that document. Do not add a `@ControllerAdvice` problem writer unless the spec names one.

## 5. Validation

When a spec names a request body, annotate it `@Valid` and constrain the fields (`@NotNull`, `@NotBlank`). Do not trust the client. This page has no request body; validate operands with `NumberStrings.requireDigits`.

- **GOOD:** reject invalid input before calling `sum`.
- **BAD:** binding a client payload straight into a query or a log line.
