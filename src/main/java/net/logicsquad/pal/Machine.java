package net.logicsquad.pal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The PAL abstract machine: runs a {@link Program}, and reports how it
 * finished.
 *
 * <p>
 * Writes only to the streams it is given and never stops the JVM, so a machine
 * can be run inside a test without taking the test runner down with it or
 * disturbing the system streams.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class Machine {
	/** How many words the data stack holds unless told otherwise. */
	static final int DATASIZE = 500;

	/** The program being run. */
	private final Program program;

	/** Stack for data. */
	private final DataStack dataStack;

	/** The program counter. */
	private int pc;

	/**
	 * The instruction currently executing. Held so that a {@link MachineFault}
	 * raised in {@link DataStack}, which has no idea what the machine is doing,
	 * can still be reported against the instruction that provoked it.
	 */
	private Instruction currentInstruction;

	/** Input reader. */
	private final BufferedReader inputReader;

	/** Stream for program output. */
	private final PrintStream out;

	/** Stream for diagnostics. */
	private final PrintStream err;

	/**
	 * Wrapper to enable pushback of bytes into the input stream, for OPR 19.
	 */
	private final PushbackReader pushBack;

	/** The number of the present exception. */
	private int currentException;

	/** Predefined exception type: re-raise the current exception. */
	private static final int reraise = 0;

	/** Predefined exception type: program abort, which cannot be caught. */
	private static final int programAbort = 1;

	/** Predefined exception type: a value was not of the expected type. */
	private static final int typeMismatch = 3;

	/** Predefined exception type: input reached end of file. */
	private static final int reachedEOF = 4;

	/**
	 * Constructor.
	 *
	 * @param program
	 *            the program to run
	 * @param in
	 *            stream the running program reads from
	 * @param out
	 *            stream the running program writes to
	 * @param err
	 *            stream for runtime diagnostics
	 */
	Machine(Program program, InputStream in, PrintStream out, PrintStream err) {
		this(program, in, out, err, DATASIZE);
	}

	/**
	 * Constructor, with a data stack of a given size.
	 *
	 * @param program
	 *            the program to run
	 * @param in
	 *            stream the running program reads from
	 * @param out
	 *            stream the running program writes to
	 * @param err
	 *            stream for runtime diagnostics
	 * @param dataSize
	 *            how many words the data stack holds
	 */
	Machine(Program program, InputStream in, PrintStream out, PrintStream err,
			int dataSize) {
		this.program = program;
		this.out = out;
		this.err = err;

		dataStack = new DataStack(dataSize);

		pushBack = new PushbackReader(
				new InputStreamReader(in, StandardCharsets.UTF_8));
		// Note: the internal buffer of the BufferedReader is set
		// to 1 (the smallest possible) so that it won't buffer up
		// to EOF, thereby confusing OPR 19.
		inputReader = new BufferedReader(pushBack, 1);

		currentException = 0;
	}

	/**
	 * Execute the instructions in the machine's code memory. The instructions
	 * are implemented in accordance with the specification in
	 * {@code doc/PAL.tex}, which is this project's copy of the handout the
	 * machine was written against; the University of Adelaide URL it used to
	 * cite has long since gone.
	 *
	 * @return how the program finished
	 */
	ExitStatus execute() {
		try {
			return run();
		} catch (MachineFault fault) {
			error(currentInstruction, fault.getMessage());
			return ExitStatus.ABNORMAL;
		}
	}

	/**
	 * Run the loaded program, leaving any {@link MachineFault} to
	 * {@link Machine#execute()} to report.
	 *
	 * @return how the program finished
	 */
	private ExitStatus run() {
		// Initialise program counter.
		pc = 0;

		Instruction currInst;

		while (pc < program.instructions().size()) {
			currInst = program.instructions().get(pc);
			currentInstruction = currInst;

			// Bump the program counter.
			pc++;

			Datum tos, ntos, returnPoint, loadedVal;

			switch (currInst.mnemonic()) {
			case Mnemonic.CAL:
				// Procedure/function call.

				// Set return point field in stack mark.
				dataStack.set(dataStack.getTop() - currInst.level() - 2,
						new IntValue(pc));

				// Set new frame base.
				dataStack.setBase(dataStack.getTop() - currInst.level());

				// Jump to procedure/function code. Note that while
				// the PAL instructions start from 1, our code store
				// is indexed from 0.
				pc = currInst.intOperand() - 1;

				break;
			case Mnemonic.INC:
				// Push space onto the stack.

				dataStack.incTop(currInst.intOperand());
				break;
			case Mnemonic.JIF:
				// Jump if false.


				tos = dataStack.pop();

				if (!(tos instanceof BoolValue)) {
					dataStack.push(tos);
					error(currInst, "JIF - top of stack not a boolean.");
					return ExitStatus.ABNORMAL;
				}

				if (!((BoolValue) tos).value()) {
					int destination = currInst.intOperand();

					if (destination < 1 || destination > program.instructions().size()) {
						dataStack.push(tos);
						error(currInst, "JIF - attempt to jump outside code.");
						return ExitStatus.ABNORMAL;
					}

					// Our code store uses zero-based indexing. For
					// compatibility reasons, addresses start at 1.
					pc = destination - 1;
				}

				break;
			case Mnemonic.JMP:
				// Unconditional jump.


				int destination = currInst.intOperand();

				if (destination == 0) {
					// "JMP 0 0" signifies program termination.
					return ExitStatus.NORMAL;
				}

				if (destination < 1 || destination > program.instructions().size()) {
					error(currInst, "JMP - attempt to jump outside code.");
					return ExitStatus.ABNORMAL;
				}

				// Our code store uses zero-based indexing. For
				// compatibility reasons, addresses start at 1.
				pc = destination - 1;

				break;
			case Mnemonic.LCI:
				// Load an integer constant onto the stack.

				dataStack.push(new IntValue(currInst.intOperand()));
				break;
			case Mnemonic.LCR:
				// Load a real constant onto the stack.

				dataStack.push(new RealValue(currInst.realOperand()));
				break;
			case Mnemonic.LCS:
				// Load a string constant onto the stack.

				dataStack.push(new StringValue(currInst.stringOperand()));
				break;
			case Mnemonic.LDA:
				// Load the address of a stack location onto the top
				// of the stack.


				int address = dataStack.getAddress(currInst.level(),
						currInst.intOperand());

				dataStack.push(new IntValue(address));

				break;
			case Mnemonic.LDI:
				// Load the value addressed by the top of stack.

				tos = dataStack.pop();

				if (!(tos instanceof IntValue)) {
					dataStack.push(tos);
					error(currInst, "LDI - top of stack must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				address = ((IntValue) tos).value();

				loadedVal = dataStack.get(address);

				dataStack.push(loadedVal);

				break;
			case Mnemonic.LDV:
				// Load a value from elsewhere in the stack onto the
				// top.


				loadedVal = dataStack.get(currInst.level(),
						currInst.intOperand());

				dataStack.push(loadedVal);

				break;
			case Mnemonic.LDU:
				// Load an uninitialised value onto the top of the
				// stack.

				dataStack.push(Undef.INSTANCE);

				break;
			case Mnemonic.MST:
				// Mark the stack in preparation for a
				// procedure/function call.

				int staticLink = dataStack.getAddress(currInst.level(), 0);
				int dynamicLink = dataStack.getAddress(0, 0);

				dataStack.markStack(staticLink, dynamicLink);

				break;
			case Mnemonic.OPR:
				ExitStatus status = doOperation(currInst);
				if (status == ExitStatus.ABNORMAL) {
					return ExitStatus.ABNORMAL;
				}
				break;
			case Mnemonic.RDI:
				// Read an integer from stdin.

				String intLine = "";
				try {
					intLine = inputReader.readLine();
					if (intLine == null) {
						// EOF reached.
						currentException = reachedEOF;
						ExitStatus exceptionStatus = raiseException(currInst);
						if (exceptionStatus == ExitStatus.ABNORMAL) {
							return ExitStatus.ABNORMAL;
						}
						break;
					}
					int intVal = Integer.parseInt(intLine);
					// Put the val in the stack.
					dataStack.set(dataStack.getAddress(currInst.level(),
							currInst.intOperand()), new IntValue(intVal));
				} catch (IOException e1) {
					err.println(e1);
				} catch (NumberFormatException e2) {
					currentException = typeMismatch;
					ExitStatus exceptionStatus = raiseException(currInst);
					if (exceptionStatus == ExitStatus.ABNORMAL) {
						return ExitStatus.ABNORMAL;
					}
				}
				break;
			case Mnemonic.RDR:
				// Read a real from stdin.

				String realLine = "";
				try {
					realLine = inputReader.readLine();
					if (realLine == null) {
						// EOF reached.
						currentException = reachedEOF;
						ExitStatus exceptionStatus = raiseException(currInst);
						if (exceptionStatus == ExitStatus.ABNORMAL) {
							return ExitStatus.ABNORMAL;
						}
						break;
					}
					float realVal = Float.parseFloat(realLine);
					// Put the val in the stack.
					dataStack.set(dataStack.getAddress(currInst.level(),
							currInst.intOperand()), new RealValue(realVal));
				} catch (IOException e1) {
					err.println(e1);
				} catch (NumberFormatException e2) {
					currentException = typeMismatch;
					ExitStatus exceptionStatus = raiseException(currInst);
					if (exceptionStatus == ExitStatus.ABNORMAL) {
						return ExitStatus.ABNORMAL;
					}
				}
				break;
			case Mnemonic.REH:
				// Register an exception handler with the current
				// stack mark.


				// Get the location of the exception handler pointer
				// in the highest stack mark.
				dataStack.set(dataStack.getAddress(0, -1),
						new IntValue(currInst.intOperand()));

				break;
			case Mnemonic.SIG:
				// If the argument is 0 (the predefined "re-raise"
				// code), re-raise the current exception. Otherwise,
				// raise the exception specified by the argument.


				int excType = currInst.intOperand();
				if (excType != reraise) {
					currentException = excType;
				} else {
					// Re-raise the current exception. SIG 0 0 is
					// typically called by an exception handler when
					// it can't handle the current exception type. We
					// don't want to run that same handler again! A
					// simple way to achieve this is to nullify the
					// current exception handler pointer.
					dataStack.set(dataStack.getAddress(0, -1), new IntValue(0));
				}

				// Raise the exception...
				ExitStatus exceptionStatus = raiseException(currInst);
				if (exceptionStatus == ExitStatus.ABNORMAL) {
					return ExitStatus.ABNORMAL;
				}

				break;
			case Mnemonic.STI:
				// Store the value in the top-of-stack - 1 in the
				// address specified by the number in top-of-stack.

				tos = dataStack.pop();

				if (!(tos instanceof IntValue)) {
					dataStack.push(tos);
					error(currInst, "STI - top of stack must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				ntos = dataStack.pop();
				int loadAddress = ((IntValue) tos).value();

				dataStack.set(loadAddress, ntos);

				break;
			case Mnemonic.STO:
				// Store the value on top of the stack in the location
				// indicated.


				tos = dataStack.pop();

				dataStack.set(dataStack.getAddress(currInst.level(),
						currInst.intOperand()), tos);

				break;
			}
		}

		err.println("Program failed to execute a termination"
				+ " instruction (JMP 0 0).");
		return ExitStatus.ABNORMAL;
	}

	/**
	 * Perform the operation referenced by an {@code OPR} instruction, which
	 * {@link Machine#run()} dispatches here.
	 *
	 * @param currInst
	 *            The current {@link Instruction} to be executed. If it reaches
	 *            here, that instruction carries an {@code OPR} mnemonic.
	 * @return how the operation finished
	 */
	private ExitStatus doOperation(Instruction currInst) {
		Optional<Operation> resolved = Operation.fromCode(currInst.intOperand());
		if (resolved.isEmpty()) {
			error(currInst, "Argument to OPR must be in range 0-31.");
			return ExitStatus.ABNORMAL;
		}

		Operation operation = resolved.get();

		return switch (operation) {
		case PROCEDURE_RETURN -> procedureReturn(currInst);
		case FUNCTION_RETURN -> functionReturn(currInst);
		case NEGATE -> negate(currInst);
		case ADD, SUBTRACT, MULTIPLY, DIVIDE -> arithmetic(currInst, operation);
		case POWER -> power(currInst);
		case CONCATENATE -> concatenate(currInst);
		case ODD -> odd(currInst);
		case EQUAL, NOT_EQUAL, LESS, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL ->
			comparison(currInst, operation);
		case NOT -> logicalNot(currInst);
		case TRUE -> pushTrue(currInst);
		case FALSE -> pushFalse(currInst);
		case AT_EOF -> atEof(currInst);
		case PRINT -> print(currInst);
		case NEWLINE -> newline(currInst);
		case SWAP -> swap(currInst);
		case DUPLICATE -> duplicate(currInst);
		case DISCARD -> discard(currInst);
		case INT_TO_REAL -> intToReal(currInst);
		case REAL_TO_INT -> realToInt(currInst);
		case INT_TO_STRING -> intToString(currInst);
		case REAL_TO_STRING -> realToString(currInst);
		case AND -> logicalAnd(currInst);
		case OR -> logicalOr(currInst);
		case TEST_EXCEPTION -> testException(currInst);
		};
	}

	/**
	 * Performs {@code procedure return}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus procedureReturn(Instruction currInst) {
		Datum returnPoint, dynamicLink;

		// Procedure return.

		// Set program counter.
		returnPoint = dataStack.get(0, -2);
		pc = ((IntValue) returnPoint).value();

		// Remember the dynamic link.
		dynamicLink = dataStack.get(0, -3);

		// Discard this frame, mark and all.
		dataStack.unwind(dataStack.getAddress(0, -4));

		// Set the new frame base using the remembered dynamic
		// link.
		dataStack.setBase(((IntValue) dynamicLink).value());

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code function return}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus functionReturn(Instruction currInst) {
		Datum returnPoint, tos, dynamicLink;

		// Function return.

		tos = dataStack.pop();

		// Set program counter.
		returnPoint = dataStack.get(0, -2);
		pc = ((IntValue) returnPoint).value();

		// Remember the dynamic link.
		dynamicLink = dataStack.get(0, -3);

		// Discard this frame, mark and all.
		dataStack.unwind(dataStack.getAddress(0, -4));

		// Set the new frame base using the remembered dynamic
		// link.
		dataStack.setBase(((IntValue) dynamicLink).value());

		// Leave the return value on top of the stack.
		dataStack.push(tos);

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code negate}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus negate(Instruction currInst) {
		Datum tos;

		// Negate the value on TOS if it is an integer or real.

		tos = dataStack.peek();
		if (tos instanceof IntValue intValue) {
			dataStack.set(dataStack.getTop() - 1,
					new IntValue(-intValue.value()));
		} else if (tos instanceof RealValue realValue) {
			dataStack.set(dataStack.getTop() - 1,
					new RealValue(-realValue.value()));
		} else {
			error(currInst, Operation.NEGATE,
					"cannot negate a boolean, string or undefined value.");
			return ExitStatus.ABNORMAL;
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code add, subtract, multiply, divide}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @param operation
	 *            which operation to perform
	 * @return how the operation finished
	 */
	private ExitStatus arithmetic(Instruction currInst, Operation operation) {
		Datum tos, ntos;

		// Pop values at TOS and TOS-1,
		// add/subtract/multiply/divide them (depending on the
		// opcode) and push result onto TOS.

		tos = dataStack.pop();
		ntos = dataStack.pop();
		if (ntos.getClass() != tos.getClass()) {
			dataStack.push(ntos);
			dataStack.push(tos);
			error(currInst, operation, "operands must be of the same type.");
			return ExitStatus.ABNORMAL;
		} else {
			if (!(tos instanceof IntValue) && !(tos instanceof RealValue)) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, operation, "operands must be integer or real.");
				return ExitStatus.ABNORMAL;
			}
			if (tos instanceof IntValue) {
				int int1 = ((IntValue) ntos).value();
				int int2 = ((IntValue) tos).value();
				switch (operation) {
				case ADD:
					dataStack.push(new IntValue(int1
							+ int2));
					break;
				case SUBTRACT:
					dataStack.push(new IntValue(int1
							- int2));
					break;
				case MULTIPLY:
					dataStack.push(new IntValue(int1
							* int2));
					break;
				case DIVIDE:
					if (int2 == 0) {
						dataStack.push(ntos);
						dataStack.push(tos);
						error(currInst, operation, "attempt to divide by zero.");
						return ExitStatus.ABNORMAL;
					}

					dataStack.push(new IntValue(int1
							/ int2));
					break;
				default:
				}
			} else {
				float flt1 = ((RealValue) ntos).value();
				float flt2 = ((RealValue) tos).value();
				switch (operation) {
				case ADD:
					dataStack.push(new RealValue(flt1
							+ flt2));
					break;
				case SUBTRACT:
					dataStack.push(new RealValue(flt1
							- flt2));
					break;
				case MULTIPLY:
					dataStack.push(new RealValue(flt1
							* flt2));
					break;
				case DIVIDE:
					if (flt2 == 0) {
						dataStack.push(ntos);
						dataStack.push(tos);
						error(currInst, operation, "attempt to divide by zero.");
						return ExitStatus.ABNORMAL;
					}

					dataStack.push(new RealValue(flt1
							/ flt2));
					break;
				default:
				}
			}
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code power}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus power(Instruction currInst) {
		Datum tos, ntos;

		// Raise the value at TOS-1 to the power of the value at
		// TOS, pop both and push the result.

		if (!(dataStack.peek() instanceof IntValue)) {
			error(currInst, Operation.POWER, "exponent must be an integer.");
			return ExitStatus.ABNORMAL;
		}
		tos = dataStack.pop();
		int exponent = ((IntValue) tos).value();

		if (!(dataStack.peek() instanceof IntValue)
				&& !(dataStack.peek() instanceof RealValue)) {
			error(currInst, Operation.POWER, "base must be an integer or real.");
			return ExitStatus.ABNORMAL;
		}
		ntos = dataStack.pop();
		if (ntos instanceof IntValue) {
			int base = ((IntValue) ntos).value();
			int intAnswer = (int) Math.pow(base, exponent);
			dataStack.push(new IntValue(intAnswer));
		} else {
			float base = ((RealValue) ntos).value();
			float floatAnswer = (float) Math.pow(base, exponent);
			dataStack.push(new RealValue(floatAnswer));
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code concatenate}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus concatenate(Instruction currInst) {
		Datum tos, ntos;

		// String concatenation.

		tos = dataStack.pop();
		ntos = dataStack.pop();
		if (!(tos instanceof StringValue) || !(ntos instanceof StringValue)) {
			dataStack.push(ntos);
			dataStack.push(tos);
			error(currInst, Operation.CONCATENATE,
					"both operands must be strings.");
			return ExitStatus.ABNORMAL;
		}
		String sResult = ((StringValue) ntos).value();
		sResult += ((StringValue) tos).value();
		dataStack.push(new StringValue(sResult));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code odd}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus odd(Instruction currInst) {
		Datum tos;

		// Test if TOS is an odd integer.

		if (!(dataStack.peek() instanceof IntValue)) {
			error(currInst, Operation.ODD, "operand must be an integer.");
			return ExitStatus.ABNORMAL;
		} else {
			tos = dataStack.pop();
			// NB the % operator will give a negative for a
			// negative number.
			if (Math.abs(((IntValue) tos).value() % 2) == 1) {
				dataStack.push(new BoolValue(true));
			} else {
				dataStack.push(new BoolValue(false));
			}
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code equal, not equal, less, greater or equal, greater, less or equal}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @param operation
	 *            which operation to perform
	 * @return how the operation finished
	 */
	private ExitStatus comparison(Instruction currInst, Operation operation) {
		Datum tos, ntos;

		// Pop values at TOS and TOS-1, compare them (depending on
		// the opcode) and push result onto TOS.

		tos = dataStack.pop();
		ntos = dataStack.pop();

		if (ntos.getClass() != tos.getClass()) {
			dataStack.push(ntos);
			dataStack.push(tos);
			error(currInst, operation, "operands must be of the same type.");
			return ExitStatus.ABNORMAL;
		} else {
			if (!(tos instanceof IntValue) && !(tos instanceof RealValue)) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, operation, "operands must be integer or real.");
				return ExitStatus.ABNORMAL;
			}
			if (tos instanceof IntValue) {
				int int1 = ((IntValue) ntos).value();
				int int2 = ((IntValue) tos).value();
				switch (operation) {
				case EQUAL:
					dataStack.push(new BoolValue(int1 == int2));
					break;
				case NOT_EQUAL:
					dataStack.push(new BoolValue(int1 != int2));
					break;
				case LESS:
					dataStack.push(new BoolValue(int1 < int2));
					break;
				case GREATER_OR_EQUAL:
					dataStack.push(new BoolValue(int1 >= int2));
					break;
				case GREATER:
					dataStack.push(new BoolValue(int1 > int2));
					break;
				case LESS_OR_EQUAL:
					dataStack.push(new BoolValue(int1 <= int2));
					break;
				default:
				}
			} else {
				float flt1 = ((RealValue) ntos).value();
				float flt2 = ((RealValue) tos).value();
				switch (operation) {
				case EQUAL:
					dataStack.push(new BoolValue(flt1 == flt2));
					break;
				case NOT_EQUAL:
					dataStack.push(new BoolValue(flt1 != flt2));
					break;
				case LESS:
					dataStack.push(new BoolValue(flt1 < flt2));
					break;
				case GREATER_OR_EQUAL:
					dataStack.push(new BoolValue(flt1 >= flt2));
					break;
				case GREATER:
					dataStack.push(new BoolValue(flt1 > flt2));
					break;
				case LESS_OR_EQUAL:
					dataStack.push(new BoolValue(flt1 <= flt2));
					break;
				default:
				}
			}
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code not}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus logicalNot(Instruction currInst) {
		Datum tos;

		// Logical complement the top element of the stack.

		tos = dataStack.pop();

		if (!(tos instanceof BoolValue)) {
			dataStack.push(tos);
			error(currInst, Operation.NOT, "operand must be a boolean.");
			return ExitStatus.ABNORMAL;
		}

		boolean bResult = !((BoolValue) tos).value();
		dataStack.push(new BoolValue(bResult));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code true}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus pushTrue(Instruction currInst) {
		// Push boolean true on TOS.

		dataStack.push(new BoolValue(true));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code false}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus pushFalse(Instruction currInst) {
		// Push boolean false on TOS

		dataStack.push(new BoolValue(false));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code at eof}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus atEof(Instruction currInst) {
		// Test for EOF.

		try {
			int nextByte = pushBack.read();
			if (nextByte == -1) {
				dataStack.push(new BoolValue(true));
			} else {
				dataStack.push(new BoolValue(false));
				pushBack.unread(nextByte);
			}
		} catch (IOException e) {
			err.println(e);
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code print}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus print(Instruction currInst) {
		Datum tos;

		// Pop value on TOS and print it.

		if (dataStack.peek() instanceof BoolValue
				|| dataStack.peek() instanceof Undef) {
			error(currInst, Operation.PRINT,
					"can only print an integer, real or string.");
			return ExitStatus.ABNORMAL;
		} else {
			tos = dataStack.pop();
			out.print(tos);
		}

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code newline}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus newline(Instruction currInst) {
		// Print a newline.

		out.println();

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code swap}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus swap(Instruction currInst) {
		Datum tos, ntos;

		// Swap the top two elements on the stack.

		tos = dataStack.pop();
		ntos = dataStack.pop();
		dataStack.push(tos);
		dataStack.push(ntos);

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code duplicate}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus duplicate(Instruction currInst) {
		Datum tos;

		// Duplicate the element at the top of the stack.

		tos = dataStack.peek();
		dataStack.push(tos);

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code discard}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus discard(Instruction currInst) {
		// Discard the element at the top of the stack.

		dataStack.pop();

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code int to real}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus intToReal(Instruction currInst) {
		// Convert the integer at TOS to a real.

		if (!(dataStack.peek() instanceof IntValue)) {
			error(currInst, Operation.INT_TO_REAL, "operand must be an integer.");
			return ExitStatus.ABNORMAL;
		}
		float fAns = (float) ((IntValue) dataStack.pop()).value();
		dataStack.push(new RealValue(fAns));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code real to int}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus realToInt(Instruction currInst) {
		// Convert the real at TOS to an integer.

		if (!(dataStack.peek() instanceof RealValue)) {
			error(currInst, Operation.REAL_TO_INT, "operand must be a real.");
			return ExitStatus.ABNORMAL;
		}
		int iResult = (int) ((RealValue) dataStack.pop()).value();
		dataStack.push(new IntValue(iResult));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code int to string}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus intToString(Instruction currInst) {
		// Convert the integer at TOS to a string.

		if (!(dataStack.peek() instanceof IntValue)) {
			error(currInst, Operation.INT_TO_STRING, "operand must be an integer.");
			return ExitStatus.ABNORMAL;
		}
		dataStack.push(new StringValue(dataStack.pop().toString()));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code real to string}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus realToString(Instruction currInst) {
		// Convert the real at TOS to a string.

		if (!(dataStack.peek() instanceof RealValue)) {
			error(currInst, Operation.REAL_TO_STRING, "operand must be a real.");
			return ExitStatus.ABNORMAL;
		}
		dataStack.push(new StringValue(dataStack.pop().toString()));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code and}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus logicalAnd(Instruction currInst) {
		Datum tos, ntos;

		// Logical and of two booleans.

		tos = dataStack.pop();
		ntos = dataStack.pop();
		if (!(tos instanceof BoolValue) || !(ntos instanceof BoolValue)) {
			dataStack.push(ntos);
			dataStack.push(tos);
			error(currInst, Operation.AND, "both operands must be booleans.");
			return ExitStatus.ABNORMAL;
		}
		boolean bool1 = ((BoolValue) tos).value();
		boolean bool2 = ((BoolValue) ntos).value();
		dataStack.push(new BoolValue(bool1 && bool2));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code or}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus logicalOr(Instruction currInst) {
		Datum tos, ntos;

		// Logical or of two booleans.

		tos = dataStack.pop();
		ntos = dataStack.pop();
		if (!(tos instanceof BoolValue) || !(ntos instanceof BoolValue)) {
			dataStack.push(ntos);
			dataStack.push(tos);
			error(currInst, Operation.OR, "both operands must be booleans.");
			return ExitStatus.ABNORMAL;
		}
		boolean bool1 = ((BoolValue) tos).value();
		boolean bool2 = ((BoolValue) ntos).value();
		dataStack.push(new BoolValue(bool1 || bool2));

		return ExitStatus.NORMAL;
	}

	/**
	 * Performs {@code test exception}.
	 *
	 * @param currInst
	 *            the instruction being executed
	 * @return how the operation finished
	 */
	private ExitStatus testException(Instruction currInst) {
		Datum tos;

		// Test whether the current exception code is the same as
		// the integer on TOS.

		tos = dataStack.pop();
		if (!(tos instanceof IntValue)) {
			dataStack.push(tos);
			error(currInst, Operation.TEST_EXCEPTION,
					"operand must be an integer.");
			return ExitStatus.ABNORMAL;
		}

		int testValue = ((IntValue) tos).value();
		boolean pushValue = testValue == currentException;

		dataStack.push(new BoolValue(pushValue));

		return ExitStatus.NORMAL;
	}

	/**
	 * Raise an exception - look down through stack frames for an exception
	 * handler.
	 * 
	 * This method uses {@link Machine#currentException} to determine which
	 * exception to raise. Exception 1 (Program Abort) cannot be caught, so the
	 * program just terminates. All other exceptions are treated equally.
	 *
	 * @param currInst
	 *            The {@link Instruction} which caused the exception. Used to
	 *            add information to error messages.
	 * @return how the raise finished
	 */
	private ExitStatus raiseException(Instruction currInst) {
		// The Program Abort signal cannot be caught.
		if (currentException == programAbort) {
			error(currInst, "A Program Abort signal was raised.");
			return ExitStatus.ABNORMAL;
		}

		Datum handlerLocation, dynamicLink;
		int handlerAddress;

		boolean moreFrames = true;

		while (true) {
			handlerLocation = dataStack.get(0, -1);

			if (!(handlerLocation instanceof IntValue)) {
				error(currInst, "Exception handler address must be an integer.");
				return ExitStatus.ABNORMAL;
			}

			handlerAddress = ((IntValue) handlerLocation).value();

			if (handlerAddress < 0 || handlerAddress > program.instructions().size()) {
				error(currInst, "Exception handler address out of code range.");
				return ExitStatus.ABNORMAL;
			}

			if (handlerAddress == 0) {
				// An address of 0 means no handler - throw away this
				// frame and keep searching.

				// First, check if this is the lowest frame - the
				// lowest frame's stack mark begins at address 0.
				if (dataStack.getAddress(0, -4) == 0) {
					moreFrames = false;
					break;
				}

				// Remember the dynamic link.
				dynamicLink = dataStack.get(0, -3);

				// Discard this frame, mark and all.
				dataStack.unwind(dataStack.getAddress(0, -4));

				// Set the new frame base using the remembered dynamic
				// link.
				int baseAddr = ((IntValue) dynamicLink).value();
				dataStack.setBase(baseAddr);
			} else {
				// There is an exception handler.
				pc = handlerAddress - 1;

				// Stop throwing out frames.
				break;
			}
		}

		if (!moreFrames) {
			// No handler was found.
			error(currInst, "Exception #" + currentException
					+ " never handled!");
			return ExitStatus.ABNORMAL;
		}
		return ExitStatus.NORMAL;
	}

	/**
	 * Print an error against an {@code OPR} operation, naming the
	 * operation. The instruction below the message says {@code OPR 0 20};
	 * this says which operation that is.
	 *
	 * @param currInst
	 *            The offending {@code Instruction} object.
	 * @param operation
	 *            The operation that failed.
	 * @param message
	 *            A context-dependent error message to be printed.
	 */
	private void error(Instruction currInst, Operation operation, String message) {
		error(currInst, operation.description() + ": " + message);
	}

	/**
	 * Print an error. Errors are almost invariably unrecoverable, so this
	 * method announces the error, prints the offending instruction and dumps
	 * the stack.
	 * 
	 * @param currInst
	 *            The offending {@code Instruction} object.
	 * @param s
	 *            A context-dependent error message to be printed.
	 */
	private void error(Instruction currInst, String s) {
		// Ensure the error is always started on a new line.
		err.println();
		err.println("Runtime Error:");
		err.println(program.name() + ":" + currInst.lineno() + ":" + s);
		err.println(currInst);
		err.println("\nStack dump:");
		err.println("----------");
		err.print(dataStack);
	}
}
