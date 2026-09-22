# mci-training

Training project for the MCI.

The repository contains two Gradle modules: **mci-core**, a library that
adds arbitrarily large non-negative integers supplied as decimal strings,
and **mci-web**, a Spring Boot application that exposes the same
functionality through a browser interface.

## Getting started

A step-by-step guide to running the application — written for readers
without programming experience — is available at
[docs/how-to-run.md](docs/how-to-run.md).

## Modules

### `mci-core`

Core library implementing addition of two large numbers represented as
strings, using the elementary-school column-addition algorithm. Java 21,
no runtime dependencies.

See [mci-core/README.md](mci-core/README.md) for build, test, and usage
instructions.

### `mci-web`

Spring Boot + Thymeleaf web application that reuses `mci-core` through a
Gradle composite build and renders the column-addition steps in the
browser.

See [mci-web/README.md](mci-web/README.md) for run and test instructions.

## Documentation

- [docs/how-to-run.md](docs/how-to-run.md) — beginner's guide to running the application
- [docs/requirement.md](docs/requirement.md) — project requirements
- [docs/Add2Num_High-level-requirement_v1.8.md](docs/Add2Num_High-level-requirement_v1.8.md) — high-level requirements specification

## Contributing

Open an issue or pull request from the templates in `.github/`. Branch,
commit, and review rules are in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Distributed under the terms specified in [LICENSE](LICENSE).
