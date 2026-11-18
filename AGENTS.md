# JoshHack Development Guidelines

## Environment

JoshHack is a Java 17 Maven project using the Maven wrapper.

.\mvnw.cmd

The project uses JUnit 5, PIT mutation testing, SpotBugs, and a local libs/asciiPanel.jar dependency.
SpotBugs generates SARIF output for GitHub Code Scanning. Review both the local SpotBugs report and uploaded SARIF findings.

## Required Quality Checks

Before considering work complete, run:

.\mvnw.cmd test
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage
.\mvnw.cmd verify -DskipTests

Review JUnit results, PIT mutation results, and SpotBugs findings. Preserve the existing JUnit and PIT configuration.

## Change Expectations

- Preserve existing behavior unless a change is explicitly requested.
- Do not weaken, remove, skip, or disable tests merely to make the build pass.
- Clearly identify all production-code changes for human review.
- Keep changes focused and avoid unrelated refactoring.
- Document intentional SpotBugs suppressions with a justification.
- Do not push, merge, or deploy changes automatically.