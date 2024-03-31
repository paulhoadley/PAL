![](https://github.com/paulhoadley/pal/workflows/build/badge.svg)
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

Project Status
--------------

The code builds again (certainly on Mac OS X, and probably any flavour
of Unix), and the tests all pass.  There are no _known_ bugs.
