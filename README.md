[![build](https://github.com/paulhoadley/PAL/actions/workflows/build.yml/badge.svg)](https://github.com/paulhoadley/PAL/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

The PAL Abstract Machine—An implementation in Java
==================================================

What is this?
-------------

It's an implementation of a toy machine with a small instruction set,
conveniently packaged as a Java JAR.  You might use it as the target of
a toy compiler project from a higher-level language.

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
manual's lineage (see `doc/PAL.tex`) says who made them: an original
Pascal implementation by Chris Marlin, translated to Ada by Michael
Oudshoorn, with indirection and exception handling added by Kevin
Maciunas — which is exactly the set `LDI`, `STI`, `REH` and `SIG`.

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

The code builds again (certainly on Mac OS X, and probably any flavour
of Unix), and the tests all pass.  There are no _known_ bugs.
