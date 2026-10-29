---
layout: page
title: DevOps guide
---

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Build automation

This project uses Gradle for **build automation and dependency management**. **We recommend reading [this Gradle tutorial from se-edu/guides](https://se-education.org/guides/tutorials/gradle.html).**


The following commands perform common Gradle tasks.


* **`clean`**: Deletes the files created during the previous build tasks (e.g. files in the `build` folder).<br>
  For example: `./gradlew clean`

* **`shadowJar`**: Uses the Shadow plugin to create the fat JAR file `build/libs/PolicyHarbor.jar`.<br>
  For example: `./gradlew shadowJar`

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The necessary workflow configuration files are in `.github/workflows`. No further setup is required.

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage.

Coverage is evidence for investigating untested behavior, not a fixed release percentage. Check the workflow result and report-upload logs rather than assuming a green badge proves correctness.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are implemented as POSIX shell scripts, and thus can only be run on POSIX-compliant operating systems such as macOS and Linux. To run all checks locally on these operating systems, execute the following in the repository root directory:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

Release from the tested team `master` commit. Do not upload a JAR built with a local OS-only dependency override.

1. Set `MainApp.VERSION` and the window title to the intended version; review branding, command documentation and `docs/images/Ui.png` against the product.
1. Use Java 25. On Mac, activate the [prescribed Zulu JDK+FX distribution](https://se-education.org/guides/tutorials/javaInstallationMac.html) for building and running.
1. Confirm that all intended release changes are merged into team `master`; record its full SHA and require successful Java CI on that exact commit. Run `./gradlew clean check coverage shadowJar`, then `.github/run-checks.sh` and `git diff --check` (Windows: `gradlew.bat`; repository shell checks run on macOS/Linux or CI). Keep audit evidence and other worktrees outside the build directory before using `clean`.
1. Copy `build/libs/PolicyHarbor.jar` into a fresh writable folder. From that folder run `java -jar PolicyHarbor.jar` and check the [manual workflow](DeveloperGuide.md#appendix-instructions-for-manual-testing), including save/restart. Also verify `java -ea -jar PolicyHarbor.jar`; assertions must not change validation or storage behavior. Keep synthetic screenshot data separate from personal data.
1. Inspect the existing product screenshot against the tested JAR. Retain `docs/images/Ui.png` if it still shows the real dashboard, permanent Client IDs and meaningful fictional clients; replace it only when the interface has changed. Verify the deployed homepage, User Guide, Developer Guide, About Us page and their links after Pages completes for the release commit.
1. Once publication is authorized, create tag `v1.5` at the exact tested commit in the team repository. [Create a GitHub release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) titled **PolicyHarbor v1.5 — Release Candidate** and attach `PolicyHarbor.jar`. A short description of changes since v1.4, Java 25 requirements, the User Guide link and known limitations is sufficient; detailed release notes are optional. Do not tag or publish while required checks, intended merges or artifact verification remain incomplete.
1. Download the uploaded JAR and verify startup. Compare its SHA-256 with the local artifact (`shasum -a 256 PolicyHarbor.jar` on Mac, `sha256sum` on Linux, or `Get-FileHash` in PowerShell).
1. After publishing, ensure completed issues/PRs are assigned to the iteration milestone, move any incomplete items to a later milestone, and close the completed milestone. Preserve PR source branches.

CI uses Zulu JDK+FX on Windows, macOS and Linux. CI builds/tests are useful evidence but do not replace running the exact uploaded JAR or checking its GUI.
