# Challenge — Software Developer Intern

Source: https://xmyworkspace.com/learn/play/remote-internship-program/challenge-for-software-developers

You have just joined the **Software Developer Intern** team and have been assigned the following task by one of the company's clients. The client's intent is to assess how well you can build a small product.

Read the requirements carefully and submit the product as described. You may also include additional documentation or notes when submitting your work to the GIT Server.

**Estimated time:** 2 days.

---

## TASK 1 — High-Level Requirement: Add2Num

Detailed spec: [Add2Num_High-level-requirement_v1.8.pdf](https://thachln.github.io/chia-se/new-member/challenges/Add2Num_High-level-requirement_v1.8.pdf)

**The result of Task 1 must be published on a branch named `core`.**

### 1. Introduction

You are assigned to write a function implementing the addition of two large numbers (represented as strings) using the same algorithm elementary-school students use. This function will be packaged and handed to another team building the UI (or a console app); they will call your function in a larger project.

### 2. Input

The knowledge, skills, and materials you have already learned (per section 1).

### 3. Preparation

Prepare your own tools and programming documentation for the language you choose: GO, C, C++, Java, C#, Dart, Javascript, Typescript, Python, PHP, etc. Paper and pen if you find them useful.

### 4. Request

Write the core in a separate class **`MyBigNumber`**, method **`String sum(String stn1, String stn2)`**, implementing addition the way elementary students do:

- Iterate both strings `stn1`, `stn2` simultaneously from right to left, taking each character, converting it to a digit.
- Add digit by digit.
- Record the history of each step performed (prefer **LOGGING**; PRINT is acceptable if you don't know logging).

Example — `sum("1234", "897")`:

- Step 1: take 4 + 7 = 11. Write 1 into the result, carry 1.
- Step 2: take 3 + 9 = 12. Add carry 1 → 13. Write 3 → result is "31". Carry 1.
- Repeat the steps above (recall how a 3rd-grade student adds, program it the same way).

**Assumption:** input parameters are valid — only valid digits, no other characters. No input-error handling needed yet.

### 5. Output — Deliverable

- Project stored on a **public GIT Server**; submit the repository link (GITHUB.com, GITLAB.com, or any server you know).
- Tag/branch the completed version for evaluation as **`0.0.1`** (tag or branch).
- Include **Unit Testing** for the main source if available. If the unit-test source already lives inside the project, this requirement is waived.
- Source on GIT must be sufficient for someone else to clone and reproduce what the author did.
- **README.md** must contain detailed instructions: how to compile, how to run the test cases (if unit tests were written) as the author did.
- Writing unit tests in a separate project or directory from the main source is encouraged.
- After finishing, act as another person: clone the source into a conventional directory:
  - `https://github.com/youraccount/projectname` → `D:\Projects\github.com\youraccount\projectname` (Windows) or `~/Projects/github.com/youraccount/projectname` (macOS/Linux).
  - `D:\Projects` / `~/Projects` is your chosen base folder; `github.com` matches the GIT server site; `youraccount` and `projectname` must match the repo URL exactly.
  - Example: `https://github.com/thachln/Hawkeye` → `D:\Projects\github.com\thachln\Hawkeye` or `~/Projects/github.com/thachln/Hawkeye`.

---

## TASK 2 — Web Application: Adding Two Large Numbers

Develop a Web application using **Spring MVC** or **Spring Boot**, **Thymeleaf**, and **Bootstrap** that lets users visit the site and perform addition of two large numbers.

- Reuse the Task 1 result as a **sub-module** or as a **library (.jar package)**.
- The Web page must include a section displaying the **progress of the calculation**.

---

## Submission

Submit the product to the GIT Server following the branch structure described above (`core` branch for Task 1). Additional documentation or notes may be attached with the submission.
