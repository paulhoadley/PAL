# Changelog

This project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## Release 0.5 (2026-09-14)
Housekeeping throughout: nothing here changes how the machine executes a
program. The test suite grew from 2 reported tests to 130.

### Added
- One test per fixture, discovered from the filesystem, asserting both
  output and process exit status.
  [#24](https://github.com/paulhoadley/pal/issues/24)
- Unit tests for the data stack, the object file loader, and all 32
  operations of the `OPR` instruction.
  [#26](https://github.com/paulhoadley/pal/issues/26)
- Fixtures covering the ways a program can be rejected or fail.
  [#27](https://github.com/paulhoadley/pal/issues/27)
- A constructor taking input, output and error streams, so a machine can
  run without disturbing the system streams.
  [#25](https://github.com/paulhoadley/pal/issues/25)
- A release workflow that attaches the JAR to the GitHub release, with
  notes taken from this file.
  [#22](https://github.com/paulhoadley/pal/issues/22)
- Dependabot for Maven and GitHub Actions.
  [#21](https://github.com/paulhoadley/pal/issues/21)

### Fixed
- The build workflow, which had never produced a run. It now uses
  current actions and builds on both JDK 21 and the current JDK.
  [#19](https://github.com/paulhoadley/pal/issues/19)

### Changed
- The POM pins every plugin, compiles against a release target, and
  carries project metadata.
  [#20](https://github.com/paulhoadley/pal/issues/20)
- `LICENSE` uses the canonical BSD-3-Clause text, so the licence can be
  detected automatically.
  [#23](https://github.com/paulhoadley/pal/issues/23)

### Removed
- The `Makefile`, which predated the Maven migration and no longer
  worked. `make test-ref` becomes `mvn test -Dpal.updateRefs=true`.
  [#18](https://github.com/paulhoadley/pal/issues/18)
- The unused Log4j test dependency.
  [#20](https://github.com/paulhoadley/pal/issues/20)

## Release 0.4 (2026-09-14)
### Fixed
- A program terminating normally via `JMP 0 0` now exits with status
  zero. This regressed in 0.3, where every program exited with status
  one. [#15](https://github.com/paulhoadley/pal/issues/15)
- An unrecognised mnemonic is now reported as a load error against the
  offending line, rather than escaping as a Java stack trace.
  [#16](https://github.com/paulhoadley/pal/issues/16)

### Changed
- `Mnemonic` is now an enum, and class visibility has been tightened
  throughout. [#14](https://github.com/paulhoadley/pal/issues/14)
- Replaced deprecated boxed primitive constructors with `valueOf()`.

## Release 0.3 (2024-03-31)
### Changed
- Migrated to Maven for builds. This included setting up the test
  suite via JUnit. [#10](https://github.com/paulhoadley/pal/issues/10)
- Reduced reliance on `System.exit()` (outside of `main()`), largely
  to facilitate
  testing. [#11](https://github.com/paulhoadley/pal/issues/11)
