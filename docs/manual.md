The PAL Abstract Machine
========================

An implementation in Java, by Philip J. Roberts and Paul A. Hoadley.

This document describes the PAL Abstract Machine in general, and an
implementation of it as a simulator written in Java.  The simulator reads a
human-readable object file (see [Object file
format](#object-file-format)) and performs input and output on the console.

Introduction
------------

The PAL Abstract Machine is a virtual, stack-based, Harvard architecture
machine.  Data in memory is tagged, so that a type system is supported
explicitly.  The instruction set is short, though reasonably powerful,
including an "operation" instruction (`OPR`, see [`OPR 0 I`](#opr-0-i)) with
some 32 variants covering arithmetic and logical operations, type conversions
and stack manipulation.

Architecture
------------

### Memory

Storage is divided into two regions: a linear instruction store, and a data
stack.  The instruction store is essentially write-once, at object file load
time, and then read-only: self-modifying code is not possible.  **The first
location in the instruction store is at address 1.**

The data stack is manipulated by both the user program and the machine
itself, as described in the [instruction set](#instruction-set).  In addition
to the conventional stack operations, the data stack provides a degree of
random access.  Variables are implemented by referring to a stack location
relative to the current stack frame.  The relation is described in terms of a
_level difference_ and a _displacement_:

- The **level difference** is the number of stack frames the target frame
  lies below the current frame.  This can be zero, in which case the current
  frame itself is referenced.
- The **displacement** is the number of stack positions the target lies above
  the top of its stack mark.  The first such position has a displacement of
  zero.

Throughout this document, reference is made to a top-of-stack pointer, which
the machine maintains as a reference to the top item in the data stack.  This
pointer is largely internal: although it can be manipulated directly (by
[`INC`](#inc-0-i)) and is moved about implicitly by other instructions, there
is no straightforward way to obtain its absolute value from within a program.

Data items of all sizes occupy only one position on the data stack.  An
integer and the string `'an integer'` can both be stored in a single
location.  Similarly, an instruction and all of its operands are stored in a
single location in the instruction store.

### The stack frame

The _stack mark_ is the area at the bottom of a stack frame holding
meta-information for that frame.  The machine sets up the stack mark for the
first frame automatically.

```
                                             higher addresses
             +-------------------------+            ^
             |                         |            |
             |       Local Space       |            |
             |                         |            |
  base ----> +=========================+
             |    Exception Handler    |  base - 1  \
             +-------------------------+             \  the
             |      Return Point       |  base - 2    > stack
             +-------------------------+             /  mark
             |      Dynamic Link       |  base - 3  /
             +-------------------------+
             |       Static Link       |  base - 4
             +-------------------------+
```

The mark is built from the bottom up, in the order the static link, the
dynamic link, space for the return point and space for the handler address,
so the static link sits at the lowest address of the four.  The base is the
first location _above_ the mark, which is displacement zero in the frame.

### Type system

Each item in the data stack is _tagged_ with a type.  The type of an item
affects how it can be manipulated, as described in the [instruction
set](#instruction-set).  The types are:

- `bool`
- `real`
- `int`
- `string`
- `undef`

A value of type `bool` can be only true or false.  The value of an item
tagged `undef` is undefined.  Values tagged `int`, `real` and `string` are
constrained by the same rules as Java's `int`, `float` and string literals
respectively; see [Behaviour this implementation
settles](#behaviour-this-implementation-settles) for what that means in
practice.

### Exception system

The PAL Abstract Machine supports a fairly primitive exception mechanism.
Three instructions relate to exception generation and handling:
[`SIG`](#sig-0-i), [`REH`](#reh-0-a) and [`OPR 0 31`](#opr-0-31-isexception).
In addition, the two input instructions [`RDI`](#rdi-l-d) and
[`RDR`](#rdr-l-d) may raise exceptions when they encounter unexpected input.

`SIG` is used primarily for raising custom user-defined exceptions: to raise
exception 5, use `SIG 0 5`.  `SIG` is also used within exception handlers, as
described below.

`REH` tells the machine where to find an exception handler, and associates
that handler with the currently active stack frame.

`OPR 0 31` compares the presently active exception to the integer value on
top of the stack, and pushes a boolean indicating whether they match.

The following exceptions are defined:

| Number | Name | Meaning |
|---|---|---|
| **0** | Re-raise the present active signal | Causes the current signal to be re-raised.  Typically used by a handler as a last resort for an unrecognised exception.  The current frame is therefore ignored in the search for a handler when `SIG 0 0` is used. |
| **1** | Program abort | Terminates the program instantly.  No handler is called.  This differs from terminating with `JMP 0 0` in that a non-zero exit status is returned to the operating system, signalling abnormal termination. |
| **2** | No return in function | No return statement was executed by a function. |
| **3** | Type mis-match in input | `RDI` found a non-integer value, or `RDR` a non-real value. |
| **4** | Attempt to read past end of file | The input was at end-of-file before an `RDI` or `RDR`. |

An exception handler is typically written as follows:

1. Use `OPR 0 31` to test whether the exception is one of a number of known
   exceptions.
2. If it is, execute the appropriate handler code.
3. If the exception is unknown, use `SIG 0 0` to re-raise it, in case a
   handler in a lower stack frame can deal with it.

For example:

```
1:    LCI 0 3        handler code begins here
      OPR 0 31
      JIF 0 10
      .
      .              code to handle input type mismatch
      .
10:   LCI 0 4
      OPR 0 31
      JIF 0 20
      .
      .              code to handle eof
      .
20:   SIG 0 0        unknown exception
```

Object file format
------------------

Object files are plain text, suitable for editing by hand or generation by
machine, such as the output of a compiler.  The grammar is very simple, and
is presented partially below in Augmented Backus-Naur Form.[^abnf]

```abnf
objectfile = 1*(line)
line       = (intline / realline / stringline) EOL
intline    = mnemonic WSP integer WSP integer *1(WSP comment) EOL
realline   = mnemonic WSP integer WSP real *1(WSP comment) EOL
stringline = mnemonic WSP integer WSP string *1(WSP comment) EOL
```

The undefined terminal symbols are largely self-explanatory.  `EOL` is the
end-of-line character or characters.  `WSP` is a non-zero number of
whitespace characters, space or horizontal tab.  A `mnemonic` is a
three-letter mnemonic from the [instruction set](#instruction-set).
`integer` and `real` are text that can be parsed as Java `int` and `float`
respectively, and a `string` is a string of characters delimited by the
apostrophe (`'`).  An optional comment, of any characters other than `EOL`,
can appear at the end of any line.

This implementation also accepts blank lines, which the grammar above does
not allow; see [Behaviour this implementation
settles](#behaviour-this-implementation-settles).

[^abnf]: Crocker D, Overell P. (1997) "Augmented BNF for Syntax
Specifications: ABNF (RFC 2234)", The Internet Society,
<https://www.rfc-editor.org/rfc/rfc2234.txt>.  RFC 2234 has since been
superseded, most recently by RFC 5234, but it is what this grammar was
written against.

Instruction set
---------------

| Mnemonic | First | Second | Effect |
|---|---|---|---|
| `MST` | _L_ | 0 | Mark the stack |
| `CAL` | _M_ | _A_ | Procedure call |
| `INC` | 0 | _I_ | Increment top-of-stack pointer by _I_ |
| `JIF` | 0 | _A_ | Jump if false to address _A_ |
| `JMP` | 0 | _A_ | Jump to address _A_ |
| `LCI` | 0 | _I_ | Load integer constant onto stack |
| `LCR` | 0 | _R_ | Load real constant onto stack |
| `LCS` | 0 | _S_ | Load string literal onto stack |
| `LDA` | _L_ | _D_ | Load absolute address of variable onto stack |
| `LDI` | 0 | 0 | Load value at address indicated by top-of-stack |
| `LDV` | _L_ | _D_ | Load value of a variable onto stack |
| `LDU` | 0 | 0 | Load undefined value onto stack |
| `OPR` | 0 | _I_ | Execute operation _I_ |
| `RDI` | _L_ | _D_ | Read a value into an integer variable |
| `RDR` | _L_ | _D_ | Read a value into a real variable |
| `STI` | 0 | 0 | Load (top-of-stack − 1) into address at top-of-stack |
| `STO` | _L_ | _D_ | Store into a variable |
| `SIG` | 0 | _I_ | Raise signal _I_ |
| `REH` | 0 | _A_ | Register exception handler at address _A_ |

Where:

- _A_ — an address in the instruction store
- _D_ — a displacement in the data store
- _I_ — an integer number
- _L_ — a level difference
- _M_ — the number of parameters
- _R_ — a real number
- _S_ — a string

### `MST` _L_ 0

Causes the machine to mark the stack frame.  `MST` is used in calling a
procedure or function.  A program containing a call should:

1. Mark the stack using `MST`.
2. Optionally push any parameters to the call onto the stack.
3. Call the procedure or function using `CAL`.

The machine constructs the stack mark by:

1. Pushing the static link.
2. Pushing the dynamic link.
3. Pushing space for the return point.
4. Pushing space for the address of an exception handler.

_L_ is the level difference between the call to the procedure or function and
its declaration, and is used to calculate the static link.  In the following,
the level difference is 1 between the call to `A` and the declaration of `A`:

```ada
procedure A is
begin
end;

procedure B is
begin
    A;
end;
```

In the following, the level difference is 0, as procedure `B` is declared at
the same level from which it is called:

```ada
procedure A is
    procedure B is
    begin
    end;
begin
    B;
end;
```

### `CAL` _M_ _A_

The base of the current stack frame is moved to point at the current
top-of-stack minus the _M_ parameters already on the stack.  The base of the
new activation record is thus the first location _above_ the stack mark,
which should have been constructed by the machine via an `MST` immediately
prior to the call.  The return address is stored by the machine in the stack
mark, and the program counter jumps to address _A_.

### `INC` 0 _I_

The top-of-stack pointer is incremented by _I_ positions.  Any stack
positions skipped through the increment are given the type `undef`.  This is
generally used to allocate space for variables.

### `JIF` 0 _A_

The value at the top-of-stack must be of type `bool`, otherwise an error is
signalled and the machine halts.  If the value is false _and_ the address _A_
is within the range of existing instructions, the program counter jumps to
address _A_.  If the value is false and _A_ is out of range, an error is
signalled and the machine halts.  If the value is true, this instruction has
no effect.

### `JMP` 0 _A_

If address _A_ is within the range of existing instructions, the program
counter jumps to address _A_, otherwise an error is signalled and the machine
halts.  **`JMP 0 0` is the correct way to terminate a program.**

### `LCI` 0 _I_

The integer value _I_ is pushed onto the top of the stack and tagged as type
`int`.

### `LCR` 0 _R_

The real value _R_ is pushed onto the top of the stack and tagged as type
`real`.

### `LCS` 0 _S_

The string value _S_ is pushed onto the top of the stack and tagged as type
`string`.

### `LDA` _L_ _D_

The absolute address of the variable at the stack location with level
difference _L_ and displacement _D_ is pushed onto the top of the stack.  See
[Memory](#memory) for the level difference and displacement addressing
scheme.

### `LDI` 0 0

The value of the variable whose address is on top of the stack is loaded into
the top-of-stack position.  The top-of-stack pointer is unchanged.

### `LDV` _L_ _D_

The value of the variable at the stack location with level difference _L_ and
displacement _D_ is pushed onto the top of the stack.

### `LDU` 0 0

A value of type `undef` is pushed onto the stack.

### `OPR` 0 _I_

| _I_ | Operation | | _I_ | Operation |
|---|---|---|---|---|
| 0 | [procedure return](#opr-0-0-procedure-return) | | 16 | [not (logical complement)](#opr-0-16-logical-not) |
| 1 | [function return](#opr-0-1-function-return) | | 17 | [true](#opr-0-17-true) |
| 2 | [negation](#opr-0-2-negation) | | 18 | [false](#opr-0-18-false) |
| 3 | [addition](#opr-0-3-addition) | | 19 | [end-of-file](#opr-0-19-eof) |
| 4 | [subtraction](#opr-0-4-subtraction) | | 20 | [write](#opr-0-20-write) |
| 5 | [multiplication](#opr-0-5-multiplication) | | 21 | [newline](#opr-0-21-newline) |
| 6 | [division](#opr-0-6-division) | | 22 | [swap the top two elements](#opr-0-22-swap-the-top-two-elements-of-the-stack) |
| 7 | [exponentiation](#opr-0-7-exponentiation) | | 23 | [duplicate the top element](#opr-0-23-duplicate-the-element-on-top-of-the-stack) |
| 8 | [string concatenation](#opr-0-8-string-concatenation) | | 24 | [drop the top element](#opr-0-24-drop-the-element-on-top-of-the-stack) |
| 9 | [odd](#opr-0-9-odd) | | 25 | [integer-to-real](#opr-0-25-integer-to-real-conversion) |
| 10 | [equality](#opr-0-10-equality) | | 26 | [real-to-integer](#opr-0-26-real-to-integer-conversion) |
| 11 | [inequality](#opr-0-11-inequality) | | 27 | [integer-to-string](#opr-0-27-integer-to-string-conversion) |
| 12 | [less-than](#opr-0-12-less-than) | | 28 | [real-to-string](#opr-0-28-real-to-string-conversion) |
| 13 | [greater-than-or-equal-to](#opr-0-13-greater-than-or-equal-to) | | 29 | [logical and](#opr-0-29-logical-and) |
| 14 | [greater-than](#opr-0-14-greater-than) | | 30 | [logical or](#opr-0-30-logical-or) |
| 15 | [less-than-or-equal-to](#opr-0-15-less-than-or-equal-to) | | 31 | [test exception](#opr-0-31-isexception) |

#### `OPR 0 0`: procedure return

Top of stack is returned to the item at which it pointed prior to the call.
The program counter is set to the return address stored in the procedure's
stack frame, and the base is set to the value of base before the call.

#### `OPR 0 1`: function return

The value on top of the stack is taken to be the return value of the
function.  The top-of-stack pointer is returned to the value at which it
pointed prior to the call.  The program counter is set to the return address
stored in the function's stack frame and the base is set to the value of base
before the call.  Finally, the function result is pushed onto the top of the
stack.

#### `OPR 0 2`: negation

If the value on top of the stack is of type `int` or `real` it is negated.
Otherwise, an error is issued and the program terminates.

#### `OPR 0 3`: addition

If the top two values on the stack are both `int` or both `real`, they are
removed from the stack and replaced by their sum.  Otherwise, an error is
issued and the program terminates.

#### `OPR 0 4`: subtraction

If the top two values on the stack are both `int` or both `real`, they are
removed from the stack, the top value is subtracted from the next-to-top
value and the result is pushed onto the stack.  Otherwise, an error is issued
and the program terminates.

#### `OPR 0 5`: multiplication

If the top two values on the stack are both `int` or both `real`, they are
removed from the stack and replaced by their product.  Otherwise, an error is
issued and the program terminates.

#### `OPR 0 6`: division

If the top two values on the stack are both `int` or both `real`, they are
removed from the stack, the next-to-top value is divided by the top value and
the result is pushed onto the stack.  Otherwise, or if division by zero is
attempted, an error is issued and the program terminates.

#### `OPR 0 7`: exponentiation

The value on top of the stack must be of type `int`, and the value at
next-to-top may be `int` or `real`.  The type of the latter determines the
type of the result.  If these conditions are not satisfied then an error is
issued and the program terminates.  Otherwise the top two values are removed
from the stack, the next-to-top value is raised to the power of the top value
and the result is pushed onto the stack.

#### `OPR 0 8`: string concatenation

If the top two values on the stack are both of type `string`, they are
removed from the stack, the top value is appended to the next-to-top value
and the result is pushed onto the stack.  Otherwise, an error is issued and
the program terminates.

#### `OPR 0 9`: odd

If the value at the top-of-stack is not of type `int`, an error is issued and
the program terminates.  Otherwise the value is replaced by true if it is
odd, and by false if it is even.

#### `OPR 0 10`: equality

If the top two values on the stack are both `int` or both `real`, they are
compared for equality and the boolean result is pushed onto the stack.
Otherwise, an error is issued and the program terminates.

#### `OPR 0 11`: inequality

If the top two values on the stack are both `int` or both `real`, they are
compared for inequality and the boolean result is pushed onto the stack.
Otherwise, an error is issued and the program terminates.

#### `OPR 0 12`: less-than

If the top two values on the stack are not both `int` or both `real`, an
error is issued and the program terminates.  Otherwise the top two values are
removed from the stack, and true is pushed if the next-to-top value is less
than the top value, false otherwise.

#### `OPR 0 13`: greater-than-or-equal-to

If the top two values on the stack are not both `int` or both `real`, an
error is issued and the program terminates.  Otherwise the top two values are
removed from the stack, and true is pushed if the next-to-top value is
greater than or equal to the top value, false otherwise.

#### `OPR 0 14`: greater-than

If the top two values on the stack are not both `int` or both `real`, an
error is issued and the program terminates.  Otherwise the top two values are
removed from the stack, and true is pushed if the next-to-top value is
greater than the top value, false otherwise.

#### `OPR 0 15`: less-than-or-equal-to

If the top two values on the stack are not both `int` or both `real`, an
error is issued and the program terminates.  Otherwise the top two values are
removed from the stack, and true is pushed if the next-to-top value is less
than or equal to the top value, false otherwise.

#### `OPR 0 16`: logical _not_

If the value on top of the stack is of type `bool`, it is replaced by its
logical complement.  Otherwise, an error is issued and the program
terminates.

#### `OPR 0 17`: true

The boolean value true is pushed onto the stack.

#### `OPR 0 18`: false

The boolean value false is pushed onto the stack.

#### `OPR 0 19`: eof

If the end of the input has been reached, true is pushed onto the stack.
Otherwise, false is pushed onto the stack.

#### `OPR 0 20`: write

If the value on top of the stack is of type `bool` or `undef`, an error is
issued and the program terminates.  Otherwise the value on top of the stack
is removed and written to the output.  No newline is written; see [`OPR 0
21`](#opr-0-21-newline).

#### `OPR 0 21`: newline

A newline is written to the output.

#### `OPR 0 22`: swap the top two elements of the stack

The top two elements on the stack are swapped.

#### `OPR 0 23`: duplicate the element on top of the stack

A copy of the top element of the stack is pushed onto the stack.

#### `OPR 0 24`: drop the element on top of the stack

The top element of the stack is removed.

#### `OPR 0 25`: integer-to-real conversion

If the value on top of the stack is of type `int`, it is replaced by the real
representation of the value.  Otherwise, an error is issued and the program
terminates.

#### `OPR 0 26`: real-to-integer conversion

If the value on top of the stack is of type `real`, it is replaced by the
integer representation of the value.  Otherwise, an error is issued and the
program terminates.

#### `OPR 0 27`: integer-to-string conversion

If the value on top of the stack is of type `int`, it is replaced by its
string representation.  Otherwise, an error is issued and the program
terminates.

#### `OPR 0 28`: real-to-string conversion

If the value on top of the stack is of type `real`, it is replaced by its
string representation.  Otherwise, an error is issued and the program
terminates.

#### `OPR 0 29`: logical _and_

If the top two values on the stack are both of type `bool`, they are removed
from the stack and replaced with the logical _and_ of the two values.
Otherwise, an error is issued and the program terminates.

#### `OPR 0 30`: logical _or_

If the top two values on the stack are both of type `bool`, they are removed
from the stack and replaced with the logical _or_ of the two values.
Otherwise, an error is issued and the program terminates.

#### `OPR 0 31`: is(exception)

If the value on top of the stack is not of type `int`, an error is issued and
the program terminates.  Otherwise the top value is removed from the stack
and compared to the number of the currently active exception.  If the two
numbers are equal, true is pushed onto the stack, otherwise false.

### `RDI` _L_ _D_

The machine reads an integer value from input and stores it in the location
indicated by the level difference _L_ and displacement _D_.  If the input is
at end-of-file, exception 4 is raised.  If the next line in the input is not
an integer, exception 3 is raised.

### `RDR` _L_ _D_

The machine reads a real value from input and stores it in the location
indicated by the level difference _L_ and displacement _D_.  If the input is
at end-of-file, exception 4 is raised.  If the next line in the input is not
a real, exception 3 is raised.

### `STI` 0 0

Loads the value in (top-of-stack − 1) into the variable at the absolute
address specified by the value on top of the stack.  The top two elements are
removed from the stack.

### `STO` _L_ _D_

Loads the value on top of the stack into the stack location specified by the
level difference _L_ and displacement _D_.  The top element of the stack is
removed.

### `SIG` 0 _I_

Causes the entire run-time stack to be searched for an exception handler.  If
a handler is found, all activation records down to the frame containing the
handler are discarded, and control is transferred to the handler.  See
[Exception system](#exception-system).

### `REH` 0 _A_

Registers an exception handler at address _A_.  Address _A_ is stored in the
stack mark as a reference to the exception handling code for this stack
frame.  An address of zero indicates that no handler is registered.  See
[Exception system](#exception-system).

The Java simulator
------------------

### History

The implementation of the PAL machine in Java described here was written by
the authors of this document in an effort to provide a portable
implementation of the machine simulator for the Compiler Construction course
in the Department of Computer Science at the University of Adelaide.  It was
written from scratch using the existing Ada implementation of the machine as
a reference.  The lineage of the Ada implementation can be traced as follows,
according to comments in the Ada source:

- Original Pascal implementation by Chris Marlin.
- Translation from Pascal to Ada by Michael Oudshoorn.
- Implementation of indirection and exception handling by Kevin Maciunas.

The instruction set itself is older than any of these; see [Where the design
came from](../README.md#where-the-design-came-from).

### Running it

```
java -jar pal.jar [options] [objectfile]
```

The simulator runs `objectfile`, performing input and output on the console.
If no object file is named, it looks for a file called `CODE` in the current
directory and complains if that file is not found.  Input and output
redirection can be used in the standard way:

```
java -jar pal.jar objectfile < input > output
```

The options are:

| Option | Effect |
|---|---|
| `-h`, `--help` | Print a usage message and exit. |
| `--version` | Print the version and exit. |
| `--trace` | Report each instruction on standard error as it executes. |
| `--input=FILE` | Read program input from `FILE` rather than the console. |
| `--code-size=N` | Allow _N_ instructions rather than the default 1000. |
| `--data-size=N` | Allow _N_ words of data stack rather than the default 500. |

A value is given to an option with an equals sign.  Options may appear before
or after the object file.

`--trace` writes one line per instruction, before that instruction executes.
Given this object file, whose first line is blank:

```
1:
2:    JMP 0 3
3:    LCS 0 'skipped'
4:    LCS 0 'reached'
5:    OPR 0 20
6:    OPR 0 21
7:    JMP 0 0
```

the trace on standard error is:

```
trace: 1:2 tos=- JMP 0 3
trace: 3:4 tos=- LCS 0 'reached'
trace: 4:5 tos=reached OPR 0 20
trace: 5:6 tos=- OPR 0 21
trace: 6:7 tos=- JMP 0 0
```

The two numbers are the instruction's **address** and its **line in the
object file**, and the example shows why both are given: they are not the
same thing whenever the file contains a blank line.  Addresses count
instructions, and it is addresses that `CAL`, `JMP` and `JIF` operands refer
to — which is why `JMP 0 3` lands on `LCS 0 'reached'`, at address 3 but on
line 4.  A diagnostic, by contrast, cites the line.

`tos=` is the value on top of the stack, or `-` when the current frame is
empty, as it is for every program's first instruction.  Because the trace
goes to standard error, the program's own output on standard output is
unaffected.

### Exit status

| Status | Meaning |
|---|---|
| 0 | The program executed a termination instruction (`JMP 0 0`), or `--help` or `--version` was asked for. |
| 1 | The program is at fault: the object file was malformed, the program did something the machine forbids, it raised exception 1, or it ran off the end of the instruction store without terminating. |
| 2 | Nothing ran: the command line was wrong, or a file it named could not be opened. |

The distinction between 1 and 2 is whose fault it was, so that a build script
can tell a broken program from a mistyped command without reading the
diagnostic.

### Limits

The simulator imposes two arbitrary limits, both adjustable on the command
line:

1. The size of the code store is limited to 1000 instructions, and their
   operands.
2. The size of the data store is limited to 500 items.

Exceeding either limit is a fault in the program, whether the limit is the
default or one given with `--code-size` or `--data-size`.

### Behaviour this implementation settles

The description above leaves some things open, and a few of the answers are
worth stating because a compiler emitting PAL will run into them.  All of the
following are consequences of the machine being written in Java.

- **Reals are 32-bit.** A `real` is a Java `float`, not a `double`.
- **Integer arithmetic wraps.** Addition, subtraction and multiplication
  overflow silently, as Java's `int` operators do: 2147483647 + 1 gives
  −2147483648 rather than an error.
- **Exponentiation does not wrap; it saturates.** `OPR 0 7` computes through
  a double and converts back, so an integer result that overflows clamps to
  2147483647 instead.  A negative exponent on an integer base yields 0
  rather than an error, so `2` to the power `-1` is 0.
- **Integer division truncates toward zero**, so −7 divided by 2 is −3.
- **Division by zero is refused for reals as well as integers.**  `1.0 / 0.0`
  is an error rather than an infinity.
- **Real-to-integer conversion truncates toward zero**, so 2.7 becomes 2 and
  −2.7 becomes −2.
- **A real prints as Java prints a `float`.** `OPR 0 20` and `OPR 0 28` on
  the real 1 give `1.0`, and very large or small values appear in scientific
  notation.
- **`RDI` is strict about surrounding whitespace, and `RDR` is not.**  The
  two are parsed by different Java methods and inherit their differences.
  `RDI` rejects ` 42` and `42 `, raising exception 3, where a bare `42` is
  read as 42.  `RDR` trims, so ` 4.5 ` is read as 4.5, and it accepts
  anything Java accepts as a `float` literal, including a trailing `f` or
  `d`: `4.5f` is read as 4.5.  Both accept a leading `+`.
- **Blank lines in an object file are ignored**, though the grammar does not
  allow them.  They do not count towards the code store limit, and they do
  not occupy an address, which is why an instruction's address and its line
  number can differ.
- **A comment must follow an operand.**  The grammar puts comments at the end
  of a line, after the three fields, and that is what is accepted: a line
  consisting only of a comment is rejected as an unknown mnemonic.
