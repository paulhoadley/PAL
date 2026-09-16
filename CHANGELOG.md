# Changelog

This project adheres to [Semantic
Versioning](https://semver.org/spec/v2.0.0.html).

## Release 0.7 (2026-09-16)
Structure. The machine is modelled in the language's own terms rather than
in tagged integers and `Object`s, and the single 1,258-line class it all
lived in is now a loader, a machine and a command line. No program that
ran before runs differently.

Diagnostics do change, so anything reading them should expect different
text. A fault in an `OPR` operation names the operation, an operand of
the wrong kind is refused before the program runs rather than partway
through it, and the instruction shown beneath a runtime error is the
source line as written, comment and all, rather than a reconstruction.

### Changed
- Values on the data stack are a sealed `Datum` type with a record per
  kind, immutable, in cells that are not. This removes every cast against
  a stack value, and the hand-written `clone()` that copying them needed.
  [#32](https://github.com/paulhoadley/pal/issues/32)
- Each mnemonic declares the kind of operand it takes, so the loader
  parses it once and the interpreter no longer opens every instruction by
  checking what it got.
  [#33](https://github.com/paulhoadley/pal/issues/33)
- The 32 `OPR` operations have names. `doOperation` is an exhaustive
  switch over them dispatching to one method per operation or family,
  rather than a 490-line switch on a raw number.
  [#34](https://github.com/paulhoadley/pal/issues/34)
- The loader, the machine and the command line are separate classes.
  Only the command line decides to stop the JVM, and the name of the file
  being run is no longer a property of the whole process.
  [#35](https://github.com/paulhoadley/pal/issues/35)
- Javadoc lint is back on, having been suppressed since the POM was
  retrofitted, along with a pass of source tidying and an `.editorconfig`.
  [#36](https://github.com/paulhoadley/pal/issues/36)

### Fixed
- An operand of the wrong kind is reported with its line number before
  the program runs, and the message names the mnemonic and what it
  wanted.
  [#33](https://github.com/paulhoadley/pal/issues/33)
- A fault in an `OPR` operation names the operation, so a reader need not
  look the number up.
  [#34](https://github.com/paulhoadley/pal/issues/34)
- The instruction shown with a runtime error is the line as written. A
  real operand used to appear as the parsed `float` rather than as typed,
  and comments and spacing were lost.
  [#33](https://github.com/paulhoadley/pal/issues/33)

## Release 0.6 (2026-09-16)
Error handling. A PAL program can no longer make the machine emit a raw
JVM exception, and several faults that were reported as something else,
or not diagnosed at all, now say what actually happened.

Anything parsing the machine's diagnostics should expect different text.
Load errors are now `file:line: message`, and three runtime faults that
used to print a bare line of JVM text now produce the same full
diagnostic, with offending instruction and stack dump, as every other
fault.

### Changed
- `OutOfMemoryError` and `IndexOutOfBoundsException` give way to a PAL
  exception hierarchy. A fault in the program being run is now always
  distinguishable from a bug in the simulator running it, and only the
  latter reaches the user as a stack trace.
  [#28](https://github.com/paulhoadley/pal/issues/28)

### Fixed
- Popping more than a frame holds is refused, instead of silently
  consuming the frame's stack mark and failing later with an unrelated
  complaint. Returning from a call still unwinds past the mark, which is
  now a separate operation.
  [#30](https://github.com/paulhoadley/pal/issues/30)
- The data stack holds the 500 words the manual promises, whichever way
  it grew, and a refused push no longer leaves its value behind.
  [#30](https://github.com/paulhoadley/pal/issues/30)
- The code store limit counts instructions rather than source lines, so a
  program padded with blank lines is no longer rejected for exceeding a
  limit it never reached.
  [#30](https://github.com/paulhoadley/pal/issues/30)
- An unterminated string literal is reported as one, rather than throwing
  out of the loader.
  [#29](https://github.com/paulhoadley/pal/issues/29)
- Naming a file that cannot be opened, or naming none when there is no
  `./CODE`, prints the reason and the usage message rather than a stack
  trace. Usage goes to stderr, where a diagnostic belongs.
  [#29](https://github.com/paulhoadley/pal/issues/29)
- Comparisons no longer complain about arithmetic, and `OPR 0 31` no
  longer runs two words together.
  [#31](https://github.com/paulhoadley/pal/issues/31)

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
