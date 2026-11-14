# CalculatorApp

[![CI](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/ci.yml/badge.svg)](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36.svg)](https://maven.apache.org/)

A Java Swing desktop calculator. It performs addition, subtraction, multiplication and division on
numbers, and it can calculate the percentage of a given number. There is also an on/off switch that
disables the keypad when the calculator is not in use.

This repository is a fork of
[Saurabh-pec/Calculator-javaProject](https://github.com/Saurabh-pec/Calculator-javaProject) that adds
an enterprise-grade continuous integration pipeline, a testable calculation core and repository
hygiene.

## Features

- Java Swing GUI built with NetBeans Matisse (`Calculator.form`).
- Testable, UI-free arithmetic core (`com.mycompany.calculator.CalculatorEngine`).
- Reproducible, executable JAR (`java -jar target/calculator-app-1.0.0.jar`).
- Enforced quality gates: build hygiene, static analysis, formatting, unit tests, coverage and
  security scanning.

## Project layout

```
.
├── .github/
│   ├── workflows/ci.yml          # validate -> test -> build + security scans
│   └── dependabot.yml            # weekly Maven and GitHub Actions updates
├── config/checkstyle/checkstyle.xml
├── src/main/java/
│   ├── Calculator.java           # legacy NetBeans UI (Swing)
│   └── com/mycompany/calculator/
│       └── CalculatorEngine.java # unit-tested arithmetic core
├── src/test/java/com/mycompany/calculator/
│   └── CalculatorEngineTest.java
├── pom.xml
└── .editorconfig / .gitignore
```

## CI pipeline

The pipeline lives in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) and runs on every push
and pull request to `main`, on a weekly schedule (dependency drift) and on demand.

| Stage | Job | What is enforced |
| --- | --- | --- |
| Validate | `validate` | Maven Enforcer (Maven >= 3.8.6, JDK >= 17, dependency convergence, no duplicate dependencies), Checkstyle, Spotless formatting |
| Test | `test` | JUnit 5 suite plus a JaCoCo gate (>= 80% line, >= 70% branch coverage) |
| Build | `build` | Reproducible `package`, manifest verification and the runnable JAR as a build artifact |
| Security | `security-sca` | Trivy scans dependencies, secrets and configuration for HIGH/CRITICAL findings (SARIF to code scanning) |
| Security | `codeql` | CodeQL static analysis with the `security-extended` query pack (SARIF to code scanning) |

Pipeline design choices:

- **Least privilege** - the workflow defaults to `permissions: contents: read`; only the security
  jobs receive `security-events: write`.
- **Fail fast and cheap** - `test` runs only after `validate` passed, and `build` depends on both.
- **Deterministic behavior** - pinned Maven and action versions, `project.build.outputTimestamp` for
  reproducible archives, and Maven's dependency cache keyed by `pom.xml`.
- **No floating third-party code** - third-party actions (Trivy) are pinned to a commit SHA;
  Dependabot proposes reviewed upgrades.
- **Supply-chain visibility** - every build publishes a JAR artifact, its checksum, and SARIF results
  to GitHub code scanning.

### Recommended repository settings

Enable these in *Settings* to complete the governance story:

1. **Branch protection / ruleset for `main`** - require pull requests, require the Validate, Test,
   Build and Security checks to pass, and disallow force pushes.
2. **Secret scanning and push protection** - free for public repositories.
3. **Dependabot alerts and security updates.**
4. **Code scanning** - enabled automatically once the `security-sca` and `codeql` jobs upload SARIF.

## Local development

Prerequisites: JDK 17+ and Maven 3.8.6+.

```bash
mvn validate       # enforcer + Checkstyle + Spotless
mvn verify         # unit tests + coverage gate + package
java -jar target/calculator-app-1.0.0.jar
```

Auto-format new code before raising a pull request:

```bash
mvn spotless:apply
```

## Legacy code

`src/main/java/Calculator.java` is the original NetBeans-generated Swing class. It is excluded from
Checkstyle/Spotless and from coverage measurement because it mixes UI construction with inline event
handling. The refactoring path is to move the remaining handlers onto `CalculatorEngine`, after
which the exclusions can be removed. New code has no exclusions.

## License

No license is declared upstream; this fork keeps the same terms as the original repository.
