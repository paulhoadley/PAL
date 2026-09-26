[![build](https://github.com/paulhoadley/PAL/actions/workflows/build.yml/badge.svg)](https://github.com/paulhoadley/PAL/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

The PAL Abstract Machine—An implementation in Java
==================================================

What is this?
-------------

It's an implementation of a toy machine with a small instruction set,
conveniently packaged as a Java JAR.  You might use it as the target of
a toy compiler project from a higher-level language.

Getting started
---------------

You need a Java 21 or later runtime.  Either download `pal-VERSION.jar`
from the [latest release](https://github.com/paulhoadley/PAL/releases/latest),
or build it:

```bash
mvn clean package
```

which leaves `target/pal-VERSION.jar`, where `VERSION` matches the
version in `pom.xml`.  Building needs a JDK 21 or later and Maven 3.9 or
later.

A program is a plain text file, one instruction per line, each a
mnemonic and two fields separated by spaces or tabs.  This one is
complete:

```
LCS 0 'Hello, world.'
OPR 0 20
OPR 0 21
JMP 0 0
```

`LCS` loads a string constant onto the stack, `OPR 0 20` writes whatever
is on top of the stack, and `OPR 0 21` writes a newline.  **`JMP 0 0` is
how a program ends**; a program that runs off the end of its
instructions without one is treated as having failed.  Save that as
`HELLO` and run it:

```bash
java -jar target/pal-VERSION.jar HELLO
```

With no filename, the machine looks for a file called `CODE` in the
current directory.

Running a program
-----------------

Input and output are the console, so redirection works as usual:

```bash
java -jar target/pal-VERSION.jar HELLO < input > output
```

The most useful option is `--trace`, which writes each instruction to
standard error as it executes, along with the top of the stack — the
quickest way to see what a compiler of your own actually emitted.
`--help` lists the rest, and [the manual](docs/manual.md#running-it)
describes them.

The exit status is worth relying on, because it distinguishes a broken
program from a broken command line:

| Status | Meaning |
|---|---|
| 0 | The program executed `JMP 0 0`. |
| 1 | The program is at fault: a malformed object file, something the machine forbids, `SIG 0 1`, or running off the end without terminating. |
| 2 | Nothing ran: the command line was wrong, or a file it named could not be opened. |

Two limits apply, both adjustable with `--code-size=N` and
`--data-size=N`: a program may be at most 1,000 instructions, and the
data stack at most 500 items.  Exceeding either is a fault in the
program, so it is an exit status of 1 whether the limit is the default
or one you chose.

The manual
----------

[`docs/manual.md`](docs/manual.md) is the complete description of the
machine: its [architecture](docs/manual.md#architecture) and stack
frame, the [object file grammar](docs/manual.md#object-file-format),
and every one of the [19
instructions](docs/manual.md#instruction-set) and 32 `OPR` operations.
It also records [the behaviour this implementation
settles](docs/manual.md#behaviour-this-implementation-settles) where the
specification leaves a choice open, which is the section to read before
blaming your compiler.

Building and testing
--------------------

```bash
mvn test
```

Most of the suite is driven by fixtures — object files under
`src/test/resources`, in three directories: `basic` needs no input,
`interactive` is fed from a `.in` file, and `negative` collects the
programs that are meant to be rejected or to fail.  Beside each fixture
sits a `.ref` file holding its expected output, and a `.status` file
naming its expected exit code where that is not zero.  Adding a test is
a matter of dropping the files into place.

When a change is meant to alter what the machine prints, the reference
files can be rewritten from what it actually does:

```bash
mvn test -Dpal.updateRefs=true
```

That will make any failing test pass, so the diff it produces is the
thing to review, not the green build that follows it.  Note that it
deletes a fixture's `.status` file when the run succeeds, since a
missing one means "expected to exit zero".

History
-------

This project is an implementation of the PAL Abstract Machine, a
virtual, stack-based, Harvard architecture machine which might be used
in a course on compiler construction.  Indeed, the PAL machine was the
target architecture for Compiler Construction and Project III in the
Department of Computer Science at the University of Adelaide in 2002. 
At that time, the machine simulator was written in Ada, and this project
represents a re-write from scratch in Java.  The authors wrote this
implementation after taking Compiler Construction III, and donated it
back to the Department.

Where the design came from
--------------------------

The instruction set is recognisably descended from Niklaus Wirth's PL/0
machine, the teaching compiler in _Algorithms + Data Structures =
Programs_ (1976).  PAL keeps PL/0's three-field `mnemonic level operand`
format, four of its mnemonics unchanged (`CAL`, `JMP`, `STO`, `OPR`),
and renames two more (`INT` to `INC`, `JPC` to `JIF`).

The giveaway is the `OPR` table.  PL/0 dispatches return, negate, the
four arithmetic operations, `odd`, and then six comparisons in the
order `=`, `<>`, `<`, `>=`, `>`, `<=`.  PAL's table is the same
sequence, with a second return for functions inserted at 1 and
exponentiation and string concatenation inserted before `odd`, closing
the gap PL/0 leaves at 7.  That comparison order, with `>=` ahead of
`>`, is peculiar enough that arriving at it twice by chance is not
plausible.

What PL/0 does not have, PAL takes from the P-code of the Zurich
Pascal-P compilers: `MST` marking a frame separately from the call, and
`LDA` loading an address.  The rest are local additions, and the
lineage recorded in [the manual](docs/manual.md#history) says who made
them: an original Pascal implementation by Chris Marlin, translated to
Ada by Michael Oudshoorn, with indirection and exception handling added
by Kevin Maciunas — which is exactly the set `LDI`, `STI`, `REH` and
`SIG`.

We have not been able to establish what "PAL" stands for.  The obvious
search result, the Pedagogic Algorithmic Language devised at MIT by
Wozencraft and Evans around 1970, is a functional language with no
assignment and no memory, evaluated by a CSE machine; it has nothing in
common with this one.  The complication is that by 2004 the same
Adelaide course had moved its code generation onto "the RPAL machine",
and RPAL is the Right-reference Pedagogic Algorithmic Language, a
subset of exactly that MIT language.  So the name may have been
inherited from a tradition the department later adopted wholesale, or
the resemblance may be a coincidence.  We cannot tell from here.

The course handouts are not recoverable: the course website required a
password, so the Internet Archive holds only the 401 page.  Anyone who
knows the answer probably learnt it from one of the three people named
above.

Project Status
--------------

The build runs on Java 21 and Java 25, and the tests pass.  Known
problems and planned work are on the [issue
tracker](https://github.com/paulhoadley/PAL/issues).
