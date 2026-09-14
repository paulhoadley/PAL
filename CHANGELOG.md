# Changelog

This project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

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
