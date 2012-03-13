The PAL Abstract Machine—An implementation in Java
==================================================

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

The initial import to GitHub (marked by tag 'v0.1', but with prior
history preserved right back to an old CVS repository) represents the
code pulled from mothballs after 8 or 9 years.  There were no changes
made during this period.  Quite probably no one used the code during
this period.  It probably doesn't build (there are some hard-coded paths
in the `Makefile` that were no doubt meaningful on some long-dead
UltraSPARC workhorse).

It would be nice to get the code building at least.  So if Java
simulations of Harvard architecture machines are your thing, stay tuned.
