# CalculatorApp

[![CI](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/ci.yml/badge.svg)](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/ci.yml)
[![Release](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/release.yml/badge.svg)](https://github.com/ZarifRamzan/Calculator-javaProject/actions/workflows/release.yml)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36.svg)](https://maven.apache.org/)

A Java Swing desktop calculator. It performs addition, subtraction, multiplication and division on
numbers, and it can calculate the percentage of a given number. There is also an on/off switch that
disables the keypad when the calculator is not in use.

This repository is a fork of
[Saurabh-pec/Calculator-javaProject](https://github.com/Saurabh-pec/Calculator-javaProject) that adds
an enterprise-grade CI/CD pipeline, a testable domain core and repository hygiene.

## Architecture

| Layer | Class | Responsibility |
| --- | --- | --- |
| View | `com.mycompany.calculator.ui.CalculatorFrame` | Swing widgets only; every gesture is delegated to the model |
| State | `com.mycompany.calculator.CalculatorModel` | Display, entry buffer, pending operator, error handling |
| Domain | `com.mycompany.calculator.CalculatorEngine` | Pure arithmetic, percentages and result formatting |

All behaviour lives in the model and the engine, which are covered by unit tests. The view is a thin
adapter and is therefore excluded from the unit-test coverage gate; it is intentionally free of
calculation logic.

## Project layout

```
.
├── .github/
│   ├── workflows/ci.yml          # validate -> test -> build -> security -> deploy
│   ├── workflows/release.yml     # tag-driven GitHub Release
│   └── dependabot.yml            # weekly Maven and GitHub Actions updates
├── config/checkstyle/checkstyle.xml
├── src/main/java/com/mycompany/calculator/
│   ├── CalculatorEngine.java
│   ├── CalculatorModel.java
│   └── ui/CalculatorFrame.java
├── src/test/java/com/mycompany/calculator/
│   ├── CalculatorEngineTest.java
│   └── CalculatorModelTest.java
├── pom.xml
└── .editorconfig / .gitignore
```

## Pipeline

### `ci.yml` - continuous integration and delivery

Runs on every push and pull request to `main`, on `v*.*.*` tags, weekly and on demand.

| Stage | Job | What is enforced |
| --- | --- | --- |
| Validate | `validate` | Maven Enforcer (Maven >= 3.8.6, JDK >= 17, dependency convergence, no duplicate dependencies), Checkstyle, Spotless |
| Test | `test` | JUnit 5 suites plus a JaCoCo gate (>= 80% line, >= 70% branch, at least one class measured) |
| Build | `build` | Reproducible JAR, CycloneDX SBOM, manifest and checksum verification, artifacts uploaded |
| Security | `security-sca` | Trivy scans dependencies, secrets and configuration for HIGH/CRITICAL findings (SARIF) |
| Security | `codeql` | CodeQL `security-extended` analysis (SARIF) |
| Deploy | `deploy` | Publishes the Maven artifact to GitHub Packages behind the `production` environment |

Deployment versioning is derived automatically: tag pushes deploy the tag version (`1.0.0`), while
`main` builds deploy a unique pre-release version (`1.0.0-main.<run_number>`) so the registry never
receives a colliding coordinate.

### `release.yml` - releases with tags

Triggered by pushing a `v*.*.*` tag (or manually through *Run workflow*). It rebuilds and verifies the
exact tagged commit, then publishes a GitHub Release containing:

- `calculator-app-<version>.jar` - the executable JAR;
- `bom.json` - CycloneDX software bill of materials;
- `SHA256SUMS` - checksums for both files;
- generated release notes (`--generate-notes`).

Tags containing a hyphen (for example `v1.1.0-rc1`) are published as pre-releases.

### Cutting a release

```bash
# 1. make sure main is green
git checkout main && git pull

# 2. tag the commit (the tag is the single source of truth for the release version)
git tag -a v1.0.0 -m "CalculatorApp 1.0.0"
git push origin v1.0.0
```

The release workflow builds with `-Drevision=1.0.0`, the deploy job publishes the same version to
GitHub Packages, and the GitHub Release is created with the assets above.

### Pipeline design choices

- **Least privilege** - the workflow defaults to `permissions: contents: read`; only the security
  jobs receive `security-events: write` and only the deploy job receives `packages: write`.
- **Fail fast and cheap** - `test` runs only after `validate` passed, `build` depends on both, and
  `deploy` waits for every security job.
- **Deterministic behavior** - pinned plugin and action versions, CI-friendly `${revision}` versions,
  `project.build.outputTimestamp` for reproducible archives, Maven dependency cache.
- **No floating third-party code** - Trivy is pinned to a commit SHA; Dependabot proposes reviewed
  upgrades for both Maven and Actions.
- **Supply-chain visibility** - JAR checksums and a CycloneDX SBOM are produced for every build and
  attached to every release; SARIF results land in GitHub code scanning.

### Recommended repository settings

1. **Ruleset for `main`** - require pull requests and the `Validate`, `Test`, `Build` and
   `Security` checks; disallow force pushes.
2. **Environment protection** - add required reviewers to the `production` environment so deploys
   need approval.
3. **Secret scanning and push protection**, plus **Dependabot alerts**.

## Local development

Prerequisites: JDK 17+ and Maven 3.8.6+.

```bash
mvn validate       # enforcer + Checkstyle + Spotless
mvn verify         # unit tests + coverage gate + JAR + SBOM
java -jar target/calculator-app-1.0.0.jar
```

Auto-format before raising a pull request:

```bash
mvn spotless:apply
```

## Using the published package

```xml
<dependency>
  <groupId>com.mycompany</groupId>
  <artifactId>calculator-app</artifactId>
  <version>1.0.0</version>
</dependency>
```

Consumers must authenticate against GitHub Packages with a token that has `read:packages`.

## License

No license is declared upstream; this fork keeps the same terms as the original repository.
