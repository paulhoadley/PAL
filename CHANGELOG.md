# Changelog

This project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## Release 0.3 (2024-03-31)
### Changed
- Migrated to Maven for builds. This included setting up the test
  suite via JUnit. [#10](https://github.com/paulhoadley/pal/issues/10)
- Reduced reliance on `System.exit()` (outside of `main()`), largely
  to facilitate
  testing. [#11](https://github.com/paulhoadley/pal/issues/11)
