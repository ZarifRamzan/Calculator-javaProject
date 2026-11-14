# Implementation Guide — from fork to released pipeline

**Repository:** [ZarifRamzan/Calculator-javaProject](https://github.com/ZarifRamzan/Calculator-javaProject)
**Upstream:** [Saurabh-pec/Calculator-javaProject](https://github.com/Saurabh-pec/Calculator-javaProject)
**Date:** 2026-11-14
**Scope:** forking, repository hygiene, build hardening, testability refactor, CI/CD pipeline, deployment and tag-driven releases.

---

## Table of contents

1. [Executive summary](#1-executive-summary)
2. [Starting point](#2-starting-point)
3. [Phase 1 — Fork and repository hygiene](#3-phase-1--fork-and-repository-hygiene)
4. [Phase 2 — Build foundation](#4-phase-2--build-foundation)
5. [Phase 3 — Static analysis and formatting](#5-phase-3--static-analysis-and-formatting)
6. [Phase 4 — Tests and coverage](#6-phase-4--tests-and-coverage)
7. [Phase 5 — Security scanning](#7-phase-5--security-scanning)
8. [Phase 6 — The pipeline](#8-phase-6--the-pipeline)
9. [Phase 7 — Deploy stage](#9-phase-7--deploy-stage)
10. [Phase 8 — Releases with tags](#10-phase-8--releases-with-tags)
11. [Phase 9 — Testability refactor](#11-phase-9--testability-refactor)
12. [Verification evidence](#12-verification-evidence)
13. [Operations runbook](#13-operations-runbook)
14. [Design decisions and trade-offs](#14-design-decisions-and-trade-offs)
15. [Known limitations and roadmap](#15-known-limitations-and-roadmap)
- [Appendix A — File inventory](#appendix-a--file-inventory)
- [Appendix B — Command cheat sheet](#appendix-b--command-cheat-sheet)

---

## 1. Executive summary

| Area | Before (upstream) | After (this fork) |
| --- | --- | --- |
| Code | One 698-line NetBeans-generated Swing class, logic inline in event handlers | Domain engine + state model + thin Swing view |
| Tests | None | 54 JUnit 5 tests (25 engine, 29 model) |
| Build | 12-line POM, Java 14 target, no plugins, build output committed | Java 17, pinned plugins, reproducible JAR, CycloneDX SBOM |
| Quality gates | None | Maven Enforcer, Checkstyle, Spotless, JaCoCo (line/branch) |
| Security | None | Trivy (deps/secrets/config) + CodeQL, SARIF into code scanning |
| CI/CD | None | validate → test → build → security → deploy |
| Releases | None | Tag-driven GitHub Release with JAR, SBOM, SHA256SUMS |
| Artifact registry | None | GitHub Packages (Maven), versioned per build and per tag |

The end state is a repository where a pull request is blocked unless it is formatted, linted, tested,
coverage-gated, scanned and packaged — and where a version tag automatically produces a deployable,
auditable release.

---

## 2. Starting point

The upstream repository contained:

```text
Calculator-javaProject/
├── screenshots/            # demo images referenced by the README
├── src/main/java/
│   ├── Calculator.form     # NetBeans Matisse metadata
│   └── Calculator.java     # 698 generated lines, default package
├── target/                 # compiled .class files committed to git
├── README.md
└── pom.xml
```

Observations that drove the work:

- **No separation of concerns.** Arithmetic, parsing, button state and layout all live in one class.
- **Compile-time fragility.** Numeric parsing of the display happens inside every button handler,
  including the decimal point, so typing `.` can throw at runtime.
- **No tests, no CI.** Nothing verified the JAR even compiled on a clean machine.
- **Build output in version control.** `target/` contained 22 anonymous inner-class files.
- **POM was a skeleton** — Java 14 source/target, no plugin versions, no manifest, no test dependency.

---

## 3. Phase 1 — Fork and repository hygiene

1. Forked through the GitHub UI into `ZarifRamzan/Calculator-javaProject` (GitHub only copies the
   default branch, which is all that existed).
2. Deleted the committed `target/` directory through the repository file browser
   (*More options → Delete directory*).
3. Added `.gitignore` so build output can never come back:

```gitignore
# Maven build output (produced by CI, never committed)
target/

# IDE / tooling
.idea/
*.iml
nbproject/private/
.settings/
.classpath
.project

# Logs
*.log

# OS noise
.DS_Store
Thumbs.db
```

4. Added `.editorconfig` (LF, final newline, 4-space Java / 2-space YAML, 120 columns) so editors and
   CI agree on formatting before any linter runs.

**Why it matters:** a repository that tracks build output makes “is this reproducible?” unanswerable
and pollutes every code search. Removing it was a prerequisite for a trustworthy build artifact.

---

## 4. Phase 2 — Build foundation

The POM was rewritten from scratch. Every plugin is version-pinned, because unpinned Maven plugins
are a silent supply-chain and reproducibility risk.

| Plugin | Version | Purpose |
| --- | --- | --- |
| maven-enforcer-plugin | 3.4.1 | Maven ≥ 3.8.6, JDK ≥ 17, dependency convergence, no duplicate dependencies |
| maven-compiler-plugin | 3.13.0 | `<release>17</release>`, parameter names kept for reflection |
| maven-checkstyle-plugin | 3.3.1 | static analysis (see phase 3) |
| spotless-maven-plugin | 2.43.0 | formatting and unused-import checks |
| maven-surefire-plugin | 3.2.5 | JUnit 5 execution, headless AWT |
| jacoco-maven-plugin | 0.8.12 | coverage agent, HTML/XML report, quality gate |
| maven-jar-plugin | 3.4.1 | executable manifest with `Main-Class` |
| cyclonedx-maven-plugin | 2.8.1 | CycloneDX SBOM (`bom.json`) |

Key properties:

```xml
<version>${revision}</version>          <!-- CI-friendly versioning -->
<properties>
  <revision>1.0.0</revision>
  <maven.compiler.release>17</maven.compiler.release>
  <project.build.outputTimestamp>2024-01-01T00:00:00Z</project.build.outputTimestamp>
  <main.class>com.mycompany.calculator.ui.CalculatorFrame</main.class>
</properties>
```

- **`${revision}`** lets CI build any version (`-Drevision=1.0.0`) without editing the POM.
- **`outputTimestamp`** makes the JAR byte-reproducible for a given source tree.
- **`finalName` = `calculator-app-${project.version}`** keeps artifact names predictable.

---

## 5. Phase 3 — Static analysis and formatting

`config/checkstyle/checkstyle.xml` is a curated ruleset rather than a 300-rule dump, so every failure
is actionable:

| Category | Checks |
| --- | --- |
| Files | `NewlineAtEndOfFile`, `FileTabCharacter`, trailing-whitespace regex, `LineLength` (120) |
| Imports | `AvoidStarImport`, `UnusedImports`, `RedundantImport` |
| Layout | `LeftCurly`, `RightCurly`, `NeedBraces`, `WhitespaceAround/After`, `OneStatementPerLine`, `Indentation` |
| Correctness | `EmptyStatement`, `EqualsHashCode`, `MissingSwitchDefault`, `FallThrough`, `StringLiteralEquality` |
| Naming | package, type, method, member, parameter, local variable, constant |
| Docs | `MissingJavadocType` for public types |

Severity is `warning` and the plugin is configured with `violationSeverity=warning`, so warnings fail
the build instead of being ignored.

Spotless covers every Java file plus YAML workflows, and enforces trailing-whitespace removal, a final
newline and no unused imports. `mvn spotless:apply` fixes violations locally.

> **Historical note:** while the legacy generated class existed it was excluded from Checkstyle via a
> POM-level `<excludes>`. After the refactor (phase 9) the exclusion was deleted — nothing in the tree
> is exempt any more.

---

## 6. Phase 4 — Tests and coverage

- JUnit 5 (`junit-jupiter` 5.10.2, parameterised tests included).
- JaCoCo binds `prepare-agent`, `report` and `check` to the `verify` phase.
- The gate requires **≥ 80 % line**, **≥ 70 % branch** and **≥ 1 measured class**.

Two lessons worth recording:

1. **Class patterns are VM-style.** `com.mycompany.calculator.*` matched *zero* classes, which made the
   gate pass trivially (“Analyzed bundle … with 0 classes”). The working pattern is
   `com/mycompany/calculator/*`.
2. **A coverage gate needs an anti-empty guard.** The `CLASS / COVEREDCOUNT / minimum 1` limit turns a
   misconfigured filter into a failure instead of a false green.

The measurement scope is the domain package; the Swing view is deliberately outside the unit-test gate
(see phase 11).

---

## 7. Phase 5 — Security scanning

| Tool | Configuration | Output |
| --- | --- | --- |
| Trivy | `scan-type: fs`, `scanners: vuln,secret,misconfig`, `severity: HIGH,CRITICAL`, `ignore-unfixed`, `exit-code: 1` | SARIF → code scanning (`category: trivy`) |
| CodeQL | `languages: java-kotlin`, `queries: security-extended`, autobuild | SARIF → code scanning |

Supply-chain hygiene:

- Trivy is pinned to a **commit SHA** (`ed142fd…` = `v0.36.0`) with the version in a trailing comment,
  so a compromised tag cannot silently change what runs.
- Official GitHub actions use major tags (`actions/checkout@v7`, `setup-java@v6`,
  `upload-artifact@v7`, `codeql-action@v4`) and Dependabot proposes reviewed upgrades for both Maven
  and Actions.
- Jobs only receive the permissions they need: workflow default `contents: read`, `security-events:
  write` for scanning, `packages: write` for deploy.

Current state: **0 code-scanning alerts**, Trivy detects the Maven manifest and reports no
HIGH/CRITICAL findings.

---

## 8. Phase 6 — The pipeline

`.github/workflows/ci.yml`:

| Trigger | Purpose |
| --- | --- |
| `push` to `main` | full pipeline including deploy |
| `push` tag `v*.*.*` | verifies and deploys the release version |
| `pull_request` to `main` | validation only — no deploy |
| `schedule` (weekly, Mon 03:17 UTC) | dependency drift and CVE re-scans |
| `workflow_dispatch` | manual runs |

Job graph (arrows mean “needs”):

```text
validate ─────► test ─────┐
validate ─────────────────┤
security-sca ─────────────┼──► build ──► deploy (push / dispatch only)
codeql ───────────────────┘
```

Cross-cutting settings:

- `concurrency: ci-<workflow>-<ref>` with `cancel-in-progress: true` — a new push supersedes stale runs.
- Per-job `timeout-minutes` so a hung download cannot burn the quota.
- `actions/setup-java` with `cache: maven` keyed on the POM.
- `persist-credentials: false` on checkout — the job token is not left in `.git/config`.

---

## 9. Phase 7 — Deploy stage

The `deploy` job publishes `com.mycompany:calculator-app` to the repository’s Maven registry on
GitHub Packages.

```yaml
deploy:
  needs: [build, test, security-sca, codeql]
  if: github.event_name == 'push' || github.event_name == 'workflow_dispatch'
  environment:
    name: production
    url: https://github.com/ZarifRamzan/Calculator-javaProject/packages
  permissions:
    contents: read
    packages: write
```

Version resolution:

| Trigger | Published version | Example |
| --- | --- | --- |
| Tag `vX.Y.Z` | tag without the `v` prefix | `1.0.0` |
| Branch / manual | `<revision>-main.<run_number>` | `1.0.0-main.21` |

The branch suffix is deliberate: registries reject republishing an existing coordinate, so every
`main` build receives a unique, traceable version instead of colliding on `1.0.0-SNAPSHOT`.

Credentials are generated per run (`~/.m2/settings.xml` with the `github` server id from the POM’s
`distributionManagement`) using the job’s `GITHUB_TOKEN`; no long-lived secret is stored.

The `production` environment gives an audit trail (deployment history) and is the natural place to add
required reviewers.

---

## 10. Phase 8 — Releases with tags

`.github/workflows/release.yml` runs on `v*.*.*` tags (or manually with a tag input):

1. Checks out the tag with full history (for generated notes).
2. Builds with `-Drevision=${TAG#v}` and runs the full `verify` lifecycle (lint, tests, coverage).
3. Produces `SHA256SUMS` for the JAR and the SBOM.
4. Publishes the GitHub Release with **generated release notes** and three assets.

The publication step is **idempotent**: if the release already exists (for example it was created in
the UI), assets are refreshed with `gh release upload --clobber` instead of failing.

Tags containing a hyphen (`v1.1.0-rc1`) are automatically marked as pre-releases.

Release `v1.0.0` assets:

| Asset | Size | Purpose |
| --- | --- | --- |
| `calculator-app-1.0.0.jar` | 13,567 B | executable JAR (`Main-Class: com.mycompany.calculator.ui.CalculatorFrame`) |
| `bom.json` | 3,097 B | CycloneDX software bill of materials |
| `SHA256SUMS` | — | integrity verification |

---

## 11. Phase 9 — Testability refactor

The legacy class was replaced by three layers:

| Layer | Class | Responsibility | Tested |
| --- | --- | --- | --- |
| Domain | `calculator.CalculatorEngine` | arithmetic, division-by-zero policy, percentages, result formatting | 25 tests |
| State | `calculator.CalculatorModel` | display text, entry buffer, decimal handling, backspace, chaining, error recovery | 29 tests |
| View | `calculator.ui.CalculatorFrame` | widget construction and event wiring only | excluded from the unit gate |

Notable behaviour fixes that the old code could not express:

- Chained input (`2 + 3 + 4 = 9`) is evaluated left-to-right with a single pending operator.
- Pressing an operator twice replaces the operator instead of re-evaluating.
- Division by zero produces the `Error` state and recovers on the next digit.
- `%` converts the entry to a fraction (`50 → 0.5`) instead of appending a literal `%` character.
- Decimal input never throws: the display is only parsed when a value is actually needed.

Removed: `src/main/java/Calculator.java`, `src/main/java/Calculator.form`. The manifest’s main class,
the JaCoCo scope and the README were updated accordingly, and the Checkstyle exception was deleted.

---

## 12. Verification evidence

| Check | Result |
| --- | --- |
| CI on `main` ([run](https://github.com/ZarifRamzan/Calculator-javaProject/actions/runs/43564333956)) | ✅ 6/6 jobs, 2 m 35 s |
| CI on tag `v1.0.0` ([run](https://github.com/ZarifRamzan/Calculator-javaProject/actions/runs/43564368365)) | ✅ deployed `1.0.0` |
| Release on tag `v1.0.0` ([run](https://github.com/ZarifRamzan/Calculator-javaProject/actions/runs/43564368369)) | ✅ release + 3 assets in 30 s |
| Checkstyle | 0 violations across main and test sources |
| Spotless | all Java and YAML files clean |
| Tests | 54 run, 0 failures, 0 errors |
| Coverage gate | 3 classes measured, line ≥ 80 %, branch ≥ 70 % |
| Code scanning | 0 alerts (Trivy + CodeQL SARIF uploaded) |
| Packages | `1.0.0`, `1.0.0-main.19`, `1.0.0-main.21` published |
| Release | [v1.0.0](https://github.com/ZarifRamzan/Calculator-javaProject/releases/tag/v1.0.0) |

---

## 13. Operations runbook

### Day-to-day

```bash
mvn validate       # enforcer + Checkstyle + Spotless
mvn verify         # tests + coverage gate + JAR + SBOM
mvn spotless:apply # fix formatting before pushing
```

### Cutting a release

```bash
git checkout main && git pull
git tag -a v1.1.0 -m "CalculatorApp 1.1.0"
git push origin v1.1.0
```

This triggers the CI verification, the Packages deployment (`1.1.0`) and the GitHub Release.

### Hotfix

```bash
git checkout -b hotfix/1.1.1 main
# fix, commit, open PR (pipeline must be green)
git checkout main && git pull
git tag -a v1.1.1 -m "CalculatorApp 1.1.1"
git push origin v1.1.1
```

### Rollback

1. Mark the affected GitHub Release as *pre-release* (or delete it) so it is no longer “Latest”.
2. Publish the previous tag again if the package version must be restored, or re-tag the last good
   commit as `vX.Y.(Z+1)` — never delete a tag that consumers may have pinned.
3. GitHub Packages versions can be deleted from *Package → Package settings* if legally required.

### Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Gate passes with “0 classes” | wrong JaCoCo pattern | use `com/mycompany/calculator/*` (VM notation); the `CLASS/COVEREDCOUNT` limit prevents recurrence |
| `409 Conflict` on deploy | version already exists in the registry | branch builds already suffix `-main.<run>`; for tags, bump the version |
| Release job fails with “release already exists” | release created manually | the workflow is idempotent; ensure the `gh release view` check is present |
| CodeQL skips a fork PR | token restrictions on fork workflows | expected — scanning runs on push/schedule |
| Spotless fails only in CI | editor saved CRLF or trailing spaces | `mvn spotless:apply` and re-commit |

---

## 14. Design decisions and trade-offs

| Decision | Rationale | Trade-off |
| --- | --- | --- |
| JaCoCo `includes` on the domain package | UI code is not meaningfully unit-testable | the view has no coverage signal (accepted, it contains no logic) |
| CI-friendly `${revision}` | version comes from the tag, not a POM edit | consumers must use the deployed POM as-is |
| Unique `-main.<run>` versions | registries reject duplicate coordinates | many pre-release versions accumulate |
| Trivy pinned by SHA | protects against retagged/moved releases | manual bump needed if not using Dependabot |
| SBOM via CycloneDX Maven plugin | native to the build, attaches to the artifact | adds a plugin and ~3 s per build |
| `cancel-in-progress` | fast feedback, no wasted runner minutes | historical runs of superseded commits are cancelled |
| No sources/Javadoc JAR yet | keeps the release surface minimal | library consumers may want them (see roadmap) |

---

## 15. Known limitations and roadmap

- **Environment protection** for `production` is not enabled — add required reviewers in
  *Settings → Environments* to gate deploys and releases behind approval.
- **Branch protection / ruleset** must be enabled manually (required checks: Validate, Test, Build,
  Security).
- **No UI tests.** A TestFX or AssertJ-Swing smoke test would cover `CalculatorFrame` wiring.
- **No sources/Javadoc JARs and no artifact signing.** Both are required for Maven Central, together
  with a Sonatype namespace and GPG key.
- **Package visibility** is inherited from the repository; decide explicitly whether consumers outside
  the organisation should read it.
- **Scheduled scans** re-run the whole pipeline weekly; if cost matters, split security into its own
  scheduled workflow.

---

## Appendix A — File inventory

```text
.github/workflows/ci.yml              validate -> test -> build -> security -> deploy
.github/workflows/release.yml         tag-driven GitHub Release (idempotent)
.github/dependabot.yml                weekly Maven + Actions updates
config/checkstyle/checkstyle.xml      curated static-analysis ruleset
src/main/java/com/mycompany/calculator/CalculatorEngine.java
src/main/java/com/mycompany/calculator/CalculatorModel.java
src/main/java/com/mycompany/calculator/ui/CalculatorFrame.java
src/test/java/com/mycompany/calculator/CalculatorEngineTest.java
src/test/java/com/mycompany/calculator/CalculatorModelTest.java
pom.xml                               build, quality gates, distribution
.editorconfig  .gitignore  README.md  docs/IMPLEMENTATION-GUIDE.md
```

## Appendix B — Command cheat sheet

| Goal | Command |
| --- | --- |
| Lint + format check | `mvn enforcer:enforce checkstyle:check spotless:check` |
| Fix formatting | `mvn spotless:apply` |
| Full local verification | `mvn verify` |
| Build with a specific version | `mvn -Drevision=1.1.0 verify` |
| Package only | `mvn -DskipTests -Djacoco.skip=true package` |
| Publish to GitHub Packages | `mvn -Drevision=1.0.0 deploy` (needs `~/.m2/settings.xml`) |
| Run the app | `java -jar target/calculator-app-1.0.0.jar` |
| Coverage report | open `target/site/jacoco/index.html` |
