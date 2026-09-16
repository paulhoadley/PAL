package net.logicsquad.pal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * The PAL abstract machine simulator.
 * 
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class PAL {
	/** The filename containing the program. */
	private static String filename = "CODE";

	/** A constant for code memory limit. */
	private final int CODESIZE = 1000;

	/** A constant for data stack size limit. */
	private final int DATASIZE = 500;

	/** Memory for the instructions. */
	private ArrayList<Code> codeMem;

	/** Stack for data. */
	private DataStack dataStack;

	/** The program counter. */
	private int pc;

	/**
	 * The instruction currently executing. Held so that a {@link MachineFault}
	 * raised in {@link DataStack}, which has no idea what the machine is doing,
	 * can still be reported against the instruction that provoked it.
	 */
	private Code currentInstruction;

	/** Input reader. */
	private BufferedReader inputReader;

	/** Stream for program output. */
	private final PrintStream out;

	/** Stream for diagnostics. */
	private final PrintStream err;

	/**
	 * Wrapper to enable pushback of bytes into the input stream, for OPR 19.
	 */
	private PushbackReader pushBack;

	/** The number of the present exception. */
	private int currentException;

	/** Constants representing the predefined exception types. */
	private static final int reraise = 0;
	private static final int programAbort = 1;
	private static final int typeMismatch = 3;
	private static final int reachedEOF = 4;

	enum ExitStatus {
		NORMAL(0),
		ABNORMAL(1);

		private final int exitCode;

		ExitStatus(int exitCode) {
			this.exitCode = exitCode;
			return;
		}

		/**
		 * Returns the process exit code corresponding to this status.
		 *
		 * @return the process exit code
		 */
		int exitCode() {
			return exitCode;
		}
	}

	/**
	 * Main method for command line operation.
	 * 
	 * @param args
	 *            Command line options are limited to a single filename.
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			usage(System.err);
			System.exit(ExitStatus.ABNORMAL.exitCode());
		} else if (args.length == 1) {
			filename = args[0];
		}

		// Make a machine and load the code. Anything that stops us
		// getting as far as a termination instruction is abnormal.
		ExitStatus status = ExitStatus.ABNORMAL;
		try (InputStream is = new FileInputStream(filename)) {
			PAL machine = new PAL(is);
			status = machine.execute();
		} catch (LoadException e) {
			System.err.println(filename + ":" + e.lineno() + ": "
					+ e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open " + filename + ".");
			usage(System.err);
		} catch (IOException e) {
			System.err.println("Error reading " + filename + ": "
					+ e.getMessage());
		}
		System.exit(status.exitCode());
	}

	/**
	 * Constructor. Reads all of the statements in {@link PAL#filename
	 * <code>filename</code>} into {@link Code <code>Code</code>} objects, and
	 * stores these objects in {@link PAL#codeMem <code>codeMem</code>}. The
	 * lexical analysis of the source file is quite rigid. Any deviation from
	 * the prescribed format for source files causes the machine to stop.
	 */
	PAL(InputStream is) throws IOException {
		this(is, System.in, System.out, System.err);
	}

	/**
	 * Constructor. Behaves as {@link PAL#PAL(InputStream)}, but reads console
	 * input from <code>in</code> and writes program output and diagnostics to
	 * <code>out</code> and <code>err</code> respectively. This allows a machine
	 * to be run without disturbing the system streams, which is what the tests
	 * rely on.
	 *
	 * @param is
	 *            the object file to load
	 * @param in
	 *            stream the running program reads from
	 * @param out
	 *            stream the running program writes to
	 * @param err
	 *            stream for load errors and runtime diagnostics
	 */
	PAL(InputStream is, InputStream in, PrintStream out, PrintStream err)
			throws IOException {
		this.out = out;
		this.err = err;

		// Create the code memory.
		codeMem = new ArrayList<Code>(CODESIZE);
		dataStack = new DataStack(DATASIZE);

		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		int lineno = 1;
		String line = br.readLine();
		Mnemonic mnemonic = null;
		int first = 0;
		Object second = null;
		StringTokenizer st;

		while (line != null) {
			st = new StringTokenizer(line);

			// It seems reasonable to allow blank lines in the
			// source.
			if (!(st.hasMoreTokens())) {
				line = br.readLine();
				lineno++;
				continue;
			}

			// May not come in groups of three, in which case,
			// catch the error.
			try {
				String token = st.nextToken();
				Optional<Mnemonic> parsed = Mnemonic.from(token);
				if (parsed.isEmpty()) {
					throw new LoadException(lineno,
							"Unknown mnemonic '" + token + "'.");
				}
				mnemonic = parsed.get();
				first = Integer.parseInt(st.nextToken());
				String s = st.nextToken();
				if (s.startsWith("'")) {
					int start = line.indexOf('\'');
					int end = line.indexOf('\'', start + 1);
					if (end < 0) {
						throw new LoadException(lineno,
								"Unterminated string literal.");
					}
					second = line.substring(start, end + 1);
				} else {
					second = makeObject(s);
					if (second instanceof String) {
						throw new LoadException(lineno,
								"Unrecognised second operand.");
					}
				}
			} catch (NoSuchElementException e) {
				throw new LoadException(lineno, "Not enough tokens.");
			} catch (NumberFormatException e) {
				throw new LoadException(lineno,
						"First operand non-integer.");
			}
			if (codeMem.size() >= CODESIZE) {
				throw new LoadException(lineno,
						"Exceeded code storage limit.");
			}
			codeMem.add(new Code(mnemonic, first, second, lineno));
			line = br.readLine();
			lineno++;
		}

		// Set up the input reader.
		pushBack = new PushbackReader(new InputStreamReader(in));
		// Note: the internal buffer of the BufferedReader is set
		// to 1 (the smallest possible) so that it won't buffer up
		// to EOF, thereby confusing OPR 19.
		inputReader = new BufferedReader(pushBack, 1);

		currentException = 0;

		return;
	}

	/**
	 * Execute the instructions in the machine's code memory. The instructions
	 * are implemented in accordance with the specification found here: <a
	 * href="http://www.cs.adelaide.edu.au/users/third/cc/handouts/pal.pdf">The
	 * PAL Machine</a>.
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
	 * {@link PAL#execute()} to report.
	 *
	 * @return how the program finished
	 */
	private ExitStatus run() {
		// Initialise program counter.
		pc = 0;

		Code currInst;

		while (pc < codeMem.size()) {
			currInst = codeMem.get(pc);
			currentInstruction = currInst;

			// Bump the program counter.
			pc++;

			// Object to pull out of currInst.second.
			Object o = currInst.getSecond();

			Data tos, ntos, returnPoint, loadedVal;

			switch (currInst.getMnemonic()) {
			case Mnemonic.CAL:
				// Procedure/function call.

				// Set return point field in stack mark.
				returnPoint = dataStack.get(dataStack.getTop()
						- currInst.getFirst() - 2);
				returnPoint.setType(Data.INT);
				returnPoint.setValue(Integer.valueOf(pc));

				// Set new frame base.
				dataStack.setBase(dataStack.getTop() - currInst.getFirst());

				// Jump to procedure/function code. Note that while
				// the PAL instructions start from 1, our code store
				// is indexed from 0.
				pc = ((Integer) currInst.getSecond()).intValue() - 1;

				break;
			case Mnemonic.INC:
				// Push space onto the stack.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to INC must be an integer.");
					return ExitStatus.ABNORMAL;
				} else {
					dataStack.incTop(((Integer) o).intValue());
				}
				break;
			case Mnemonic.JIF:
				// Jump if false.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to JIF must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				tos = dataStack.pop();

				if (tos.getType() != Data.BOOL) {
					dataStack.push(tos);
					error(currInst, "JIF - top of stack not a boolean.");
					return ExitStatus.ABNORMAL;
				}

				if (!((Boolean) tos.getValue()).booleanValue()) {
					int destination = ((Integer) o).intValue();

					if (destination < 1 || destination > codeMem.size()) {
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

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to JMP must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				int destination = ((Integer) o).intValue();

				if (destination == 0) {
					// "JMP 0 0" signifies program termination.
					return ExitStatus.NORMAL;
				}

				if (destination < 1 || destination > codeMem.size()) {
					error(currInst, "JMP - attempt to jump outside code.");
					return ExitStatus.ABNORMAL;
				}

				// Our code store uses zero-based indexing. For
				// compatibility reasons, addresses start at 1.
				pc = destination - 1;

				break;
			case Mnemonic.LCI:
				// Load an integer constant onto the stack.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to LCI must be an integer.");
					return ExitStatus.ABNORMAL;
				} else {
					dataStack.push(new Data(Data.INT, o));
				}
				break;
			case Mnemonic.LCR:
				// Load a real constant onto the stack.

				if (o instanceof Integer) {
					o = Float.valueOf(((Integer) o).floatValue());
				}

				if (!(o instanceof Float)) {
					error(currInst, "Argument to LCR must be a real.");
					return ExitStatus.ABNORMAL;
				} else {
					dataStack.push(new Data(Data.REAL, o));
				}
				break;
			case Mnemonic.LCS:
				// Load a string constant onto the stack.

				if (!(o instanceof String)) {
					error(currInst, "Argument to LCS must be a string.");
					return ExitStatus.ABNORMAL;
				} else {
					if (!(((String) o).startsWith("'") && (((String) o)
							.endsWith("'")))) {
						error(currInst,
								"String must be delimited by single-quotes.");
						return ExitStatus.ABNORMAL;
					} else {
						String oS = (String) o;
						oS = oS.substring(1, oS.length() - 1);
						dataStack.push(new Data(Data.STRING, oS));
					}
				}
				break;
			case Mnemonic.LDA:
				// Load the address of a stack location onto the top
				// of the stack.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to LDA must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				int address = dataStack.getAddress(currInst.getFirst(),
						((Integer) o).intValue());

				dataStack.push(new Data(Data.INT, Integer.valueOf(address)));

				break;
			case Mnemonic.LDI:
				// Load the value addressed by the top of stack.

				tos = dataStack.pop();

				if (tos.getType() != Data.INT) {
					dataStack.push(tos);
					error(currInst, "LDI - top of stack must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				address = ((Integer) tos.getValue()).intValue();

				loadedVal = dataStack.get(address);

				dataStack.push((Data) loadedVal.clone());

				break;
			case Mnemonic.LDV:
				// Load a value from elsewhere in the stack onto the
				// top.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to LDV must be an integer");
					return ExitStatus.ABNORMAL;
				}

				loadedVal = dataStack.get(currInst.getFirst(),
						((Integer) o).intValue());

				dataStack.push((Data) loadedVal.clone());

				break;
			case Mnemonic.LDU:
				// Load an uninitialised value onto the top of the
				// stack.

				dataStack.push(new Data(Data.UNDEF, null));

				break;
			case Mnemonic.MST:
				// Mark the stack in preparation for a
				// procedure/function call.

				int staticLink = dataStack.getAddress(currInst.getFirst(), 0);
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
					loadedVal = dataStack.get(currInst.getFirst(),
							((Integer) o).intValue());
					loadedVal.setType(Data.INT);
					loadedVal.setValue(Integer.valueOf(intVal));
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
					loadedVal = dataStack.get(currInst.getFirst(),
							((Integer) o).intValue());
					loadedVal.setType(Data.REAL);
					loadedVal.setValue(Float.valueOf(realVal));
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

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to REH must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				// Get the location of the exception handler pointer
				// in the highest stack mark.
				loadedVal = dataStack.get(0, -1);

				loadedVal.setType(Data.INT);
				loadedVal.setValue(o);

				break;
			case Mnemonic.SIG:
				// If the argument is 0 (the predefined "re-raise"
				// code), re-raise the current exception. Otherwise,
				// raise the exception specified by the argument.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to SIG must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				int excType = ((Integer) o).intValue();
				if (excType != reraise) {
					currentException = excType;
				} else {
					// Re-raise the current exception. SIG 0 0 is
					// typically called by an exception handler when
					// it can't handle the current exception type. We
					// don't want to run that same handler again! A
					// simple way to achieve this is to nullify the
					// current exception handler pointer.
					Data handlerLocation = dataStack.get(0, -1);
					handlerLocation.setValue(Integer.valueOf(0));
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

				if (tos.getType() != Data.INT) {
					dataStack.push(tos);
					error(currInst, "STI - top of stack must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				ntos = dataStack.pop();
				int loadAddress = ((Integer) tos.getValue()).intValue();
				loadedVal = dataStack.get(loadAddress);

				loadedVal.setType(ntos.getType());
				loadedVal.setValue(ntos.getValue());

				break;
			case Mnemonic.STO:
				// Store the value on top of the stack in the location
				// indicated.

				if (!(o instanceof Integer)) {
					error(currInst, "Argument to STO must be an integer.");
					return ExitStatus.ABNORMAL;
				}

				tos = dataStack.pop();
				loadedVal = dataStack.get(currInst.getFirst(),
						((Integer) o).intValue());

				loadedVal.setType(tos.getType());
				loadedVal.setValue(tos.getValue());

				break;
			}
		}

		err.println("Program failed to execute a termination"
				+ " instruction (JMP 0 0).");
		return ExitStatus.ABNORMAL;
	}

	/**
	 * Perform the operation referenced by an <code>OPR</code> instruction. This
	 * method is provided separately to {@link PAL#execute <code>execute</code>}
	 * to avoid placing the rather lengthy <code>switch</code> statement in that
	 * method.
	 * 
	 * @param currInst
	 *            The current <code>Code</code> object to be executed. If it
	 *            reaches here, that object contains an <code>OPR</code>
	 *            mnemonic.
	 */
	public ExitStatus doOperation(Code currInst) {
		Object o = currInst.getSecond();
		int opr;
		if (!(o instanceof Integer)) {
			error(currInst, "Argument to OPR must be an integer.");
			return ExitStatus.ABNORMAL;
		}
		opr = ((Integer) o).intValue();
		if (opr < 0 || opr > 31) {
			error(currInst, "Argument to OPR must be in range 0-31.");
			return ExitStatus.ABNORMAL;
		}

		Data returnPoint, tos, ntos, dynamicLink;

		switch (opr) {
		case 0:
			// Procedure return.

			// Set program counter.
			returnPoint = dataStack.get(0, -2);
			pc = ((Integer) returnPoint.getValue()).intValue();

			// Remember the dynamic link.
			dynamicLink = dataStack.get(0, -3);

			// Discard this frame, mark and all.
			dataStack.unwind(dataStack.getAddress(0, -4));

			// Set the new frame base using the remembered dynamic
			// link.
			dataStack.setBase(((Integer) dynamicLink.getValue()).intValue());

			break;
		case 1:
			// Function return.

			tos = dataStack.pop();

			// Set program counter.
			returnPoint = dataStack.get(0, -2);
			pc = ((Integer) returnPoint.getValue()).intValue();

			// Remember the dynamic link.
			dynamicLink = dataStack.get(0, -3);

			// Discard this frame, mark and all.
			dataStack.unwind(dataStack.getAddress(0, -4));

			// Set the new frame base using the remembered dynamic
			// link.
			dataStack.setBase(((Integer) dynamicLink.getValue()).intValue());

			// Leave the return value on top of the stack.
			dataStack.push(tos);

			break;
		case 2:
			// Negate the value on TOS if it is an integer or real.

			tos = dataStack.peek();
			if (tos.getType() == Data.INT) {
				int oldValue = ((Integer) tos.getValue()).intValue();
				tos.setValue(Integer.valueOf(-oldValue));
			} else if (tos.getType() == Data.REAL) {
				float oldValue = ((Float) tos.getValue()).floatValue();
				tos.setValue(Float.valueOf(-oldValue));
			} else {
				error(currInst, "Cannot negate boolean, string or UNDEF value.");
				return ExitStatus.ABNORMAL;
			}
			break;
		case 3:
		case 4:
		case 5:
		case 6:
			// Pop values at TOS and TOS-1,
			// add/subtract/multiply/divide them (depending on the
			// opcode) and push result onto TOS.

			tos = dataStack.pop();
			ntos = dataStack.pop();
			if (ntos.getType() != tos.getType()) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, "Values for arithmetic operations must be"
						+ " of same type.");
				return ExitStatus.ABNORMAL;
			} else {
				int type = tos.getType();
				if (type != Data.INT && type != Data.REAL) {
					dataStack.push(ntos);
					dataStack.push(tos);
					error(currInst, "Values for arithmetic operations must be"
							+ " of type integer or real.");
					return ExitStatus.ABNORMAL;
				}
				if (type == Data.INT) {
					int int1 = ((Integer) ntos.getValue()).intValue();
					int int2 = ((Integer) tos.getValue()).intValue();
					switch (opr) {
					case 3:
						dataStack.push(new Data(Data.INT, Integer.valueOf(int1
								+ int2)));
						break;
					case 4:
						dataStack.push(new Data(Data.INT, Integer.valueOf(int1
								- int2)));
						break;
					case 5:
						dataStack.push(new Data(Data.INT, Integer.valueOf(int1
								* int2)));
						break;
					case 6:
						if (int2 == 0) {
							dataStack.push(ntos);
							dataStack.push(tos);
							error(currInst, "Attempt to divide by zero.");
							return ExitStatus.ABNORMAL;
						}

						dataStack.push(new Data(Data.INT, Integer.valueOf(int1
								/ int2)));
						break;
					default:
					}
				} else {
					float flt1 = ((Float) ntos.getValue()).floatValue();
					float flt2 = ((Float) tos.getValue()).floatValue();
					switch (opr) {
					case 3:
						dataStack.push(new Data(Data.REAL, Float.valueOf(flt1
								+ flt2)));
						break;
					case 4:
						dataStack.push(new Data(Data.REAL, Float.valueOf(flt1
								- flt2)));
						break;
					case 5:
						dataStack.push(new Data(Data.REAL, Float.valueOf(flt1
								* flt2)));
						break;
					case 6:
						if (flt2 == 0) {
							dataStack.push(ntos);
							dataStack.push(tos);
							error(currInst, "Attempt to divide by zero.");
							return ExitStatus.ABNORMAL;
						}

						dataStack.push(new Data(Data.REAL, Float.valueOf(flt1
								/ flt2)));
						break;
					default:
					}
				}
			}
			break;
		case 7:
			// Raise the value at TOS-1 to the power of the value at
			// TOS, pop both and push the result.

			if (dataStack.peek().getType() != Data.INT) {
				error(currInst, "Exponent must be of type integer.");
				return ExitStatus.ABNORMAL;
			}
			tos = dataStack.pop();
			int exponent = ((Integer) tos.getValue()).intValue();

			int baseType = dataStack.peek().getType();
			if (baseType != Data.INT && baseType != Data.REAL) {
				error(currInst, "Base must be of type integer or real.");
				return ExitStatus.ABNORMAL;
			}
			ntos = dataStack.pop();
			if (baseType == Data.INT) {
				int base = ((Integer) ntos.getValue()).intValue();
				int intAnswer = (int) Math.pow(base, exponent);
				dataStack.push(new Data(Data.INT, Integer.valueOf(intAnswer)));
			} else {
				float base = ((Float) ntos.getValue()).floatValue();
				float floatAnswer = (float) Math.pow(base, exponent);
				dataStack.push(new Data(Data.REAL, Float.valueOf(floatAnswer)));
			}
			break;
		case 8:
			// String concatenation.

			tos = dataStack.pop();
			ntos = dataStack.pop();
			if (tos.getType() != Data.STRING || ntos.getType() != Data.STRING) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst,
						"Both arguments to OPR 8 must be of type string.");
				return ExitStatus.ABNORMAL;
			}
			String sResult = (String) ntos.getValue();
			sResult += (String) tos.getValue();
			dataStack.push(new Data(Data.STRING, sResult));
			break;
		case 9:
			// Test if TOS is an odd integer.

			if (dataStack.peek().getType() != Data.INT) {
				error(currInst, "Argument to OPR 9 must be of type integer.");
				return ExitStatus.ABNORMAL;
			} else {
				tos = dataStack.pop();
				// NB the % operator will give a negative for a
				// negative number.
				if (Math.abs(((Integer) tos.getValue()).intValue() % 2) == 1) {
					dataStack.push(new Data(Data.BOOL, Boolean.valueOf(true)));
				} else {
					dataStack.push(new Data(Data.BOOL, Boolean.valueOf(false)));
				}
			}
			break;
		case 10:
		case 11:
		case 12:
		case 13:
		case 14:
		case 15:
			// Pop values at TOS and TOS-1, compare them (depending on
			// the opcode) and push result onto TOS.

			tos = dataStack.pop();
			ntos = dataStack.pop();

			if (ntos.getType() != tos.getType()) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, "Values for comparison operations must be"
						+ " of same type.");
				return ExitStatus.ABNORMAL;
			} else {
				int type = tos.getType();
				if (type != Data.INT && type != Data.REAL) {
					dataStack.push(ntos);
					dataStack.push(tos);
					error(currInst, "Values for comparison operations must be"
							+ " of type integer or real.");
					return ExitStatus.ABNORMAL;
				}
				if (type == Data.INT) {
					int int1 = ((Integer) ntos.getValue()).intValue();
					int int2 = ((Integer) tos.getValue()).intValue();
					switch (opr) {
					case 10:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 == int2)));
						break;
					case 11:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 != int2)));
						break;
					case 12:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 < int2)));
						break;
					case 13:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 >= int2)));
						break;
					case 14:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 > int2)));
						break;
					case 15:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								int1 <= int2)));
						break;
					default:
					}
				} else {
					float flt1 = ((Float) ntos.getValue()).floatValue();
					float flt2 = ((Float) tos.getValue()).floatValue();
					switch (opr) {
					case 10:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 == flt2)));
						break;
					case 11:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 != flt2)));
						break;
					case 12:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 < flt2)));
						break;
					case 13:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 >= flt2)));
						break;
					case 14:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 > flt2)));
						break;
					case 15:
						dataStack.push(new Data(Data.BOOL, Boolean.valueOf(
								flt1 <= flt2)));
						break;
					default:
					}
				}
			}
			break;
		case 16:
			// Logical complement the top element of the stack.

			tos = dataStack.pop();

			if (tos.getType() != Data.BOOL) {
				dataStack.push(tos);
				error(currInst, "Top of stack must be a boolean.");
				return ExitStatus.ABNORMAL;
			}

			boolean bResult = !((Boolean) tos.getValue()).booleanValue();
			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(bResult)));
			break;
		case 17:
			// Push boolean true on TOS.

			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(true)));
			break;
		case 18:
			// Push boolean false on TOS

			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(false)));
			break;
		case 19:
			// Test for EOF.

			try {
				int nextByte = pushBack.read();
				if (nextByte == -1) {
					dataStack.push(new Data(Data.BOOL, Boolean.valueOf(true)));
				} else {
					dataStack.push(new Data(Data.BOOL, Boolean.valueOf(false)));
					pushBack.unread(nextByte);
				}
			} catch (IOException e) {
				err.println(e);
			}
			break;
		case 20:
			// Pop value on TOS and print it.

			if (dataStack.peek().getType() == Data.BOOL
					|| dataStack.peek().getType() == Data.UNDEF) {
				error(currInst, "OPR 20 can only print values"
						+ " of type integer, real or string.");
				return ExitStatus.ABNORMAL;
			} else {
				tos = dataStack.pop();
				out.print(tos);
			}
			break;
		case 21:
			// Print a newline.

			out.println();
			break;
		case 22:
			// Swap the top two elements on the stack.

			tos = dataStack.pop();
			ntos = dataStack.pop();
			dataStack.push(tos);
			dataStack.push(ntos);
			break;
		case 23:
			// Duplicate the element at the top of the stack.

			tos = dataStack.peek();
			dataStack.push((Data) tos.clone());
			break;
		case 24:
			// Discard the element at the top of the stack.

			dataStack.pop();
			break;
		case 25:
			// Convert the integer at TOS to a real.

			if (dataStack.peek().getType() != Data.INT) {
				error(currInst, "Integer to real conversion can only be"
						+ " performed on a value of type integer.");
				return ExitStatus.ABNORMAL;
			}
			float fAns = ((Integer) dataStack.pop().getValue()).floatValue();
			dataStack.push(new Data(Data.REAL, Float.valueOf(fAns)));
			break;
		case 26:
			// Convert the real at TOS to an integer.

			if (dataStack.peek().getType() != Data.REAL) {
				error(currInst, "Real to integer conversion can only be"
						+ " performed on a value of type real.");
				return ExitStatus.ABNORMAL;
			}
			int iResult = ((Float) dataStack.pop().getValue()).intValue();
			dataStack.push(new Data(Data.INT, Integer.valueOf(iResult)));
			break;
		case 27:
			// Convert the integer at TOS to a string.

			if (dataStack.peek().getType() != Data.INT) {
				error(currInst, "Integer to string conversion can only be"
						+ " performed on a value of type integer.");
				return ExitStatus.ABNORMAL;
			}
			dataStack.push(new Data(Data.STRING, dataStack.pop().getValue()
					.toString()));
			break;
		case 28:
			// Convert the real at TOS to a string.

			if (dataStack.peek().getType() != Data.REAL) {
				error(currInst, "Real to string conversion can only be"
						+ " performed on value of type real.");
				return ExitStatus.ABNORMAL;
			}
			dataStack.push(new Data(Data.STRING, dataStack.pop().getValue()
					.toString()));
			break;
		case 29:
			// Logical and of two booleans.

			tos = dataStack.pop();
			ntos = dataStack.pop();
			if (tos.getType() != Data.BOOL || ntos.getType() != Data.BOOL) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, "Logical and can only be"
						+ " performed on values of type boolean.");
				return ExitStatus.ABNORMAL;
			}
			boolean bool1 = ((Boolean) tos.getValue()).booleanValue();
			boolean bool2 = ((Boolean) ntos.getValue()).booleanValue();
			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(bool1 && bool2)));
			break;
		case 30:
			// Logical or of two booleans.

			tos = dataStack.pop();
			ntos = dataStack.pop();
			if (tos.getType() != Data.BOOL || ntos.getType() != Data.BOOL) {
				dataStack.push(ntos);
				dataStack.push(tos);
				error(currInst, "Logical or can only be"
						+ " performed on values of type boolean.");
				return ExitStatus.ABNORMAL;
			}
			bool1 = ((Boolean) tos.getValue()).booleanValue();
			bool2 = ((Boolean) ntos.getValue()).booleanValue();
			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(bool1 || bool2)));
			break;
		case 31:
			// Test whether the current exception code is the same as
			// the integer on TOS.

			tos = dataStack.pop();
			if (tos.getType() != Data.INT) {
				dataStack.push(tos);
				error(currInst, "OPR 0 31 expects an integer value "
						+ "on top of the stack.");
				return ExitStatus.ABNORMAL;
			}

			int testValue = ((Integer) tos.getValue()).intValue();
			boolean pushValue = testValue == currentException;

			dataStack.push(new Data(Data.BOOL, Boolean.valueOf(pushValue)));
			break;
		default:
			// Unreachable: opr is range checked above, and every operation
			// from 0 to 31 has a case. Reaching here would be a bug in the
			// machine rather than in the program, so say so rather than
			// printing a note and carrying on as though nothing happened.
			throw new IllegalStateException("No case for OPR " + opr + ".");
		}
		return ExitStatus.NORMAL;
	}

	/**
	 * Make an <code>Object</code> from a <code>String</code>. Because the type
	 * of the third field in a single instruction is not pre-defined, we need to
	 * be able to expect an <code>int</code>, a <code>float</code> or a
	 * <code>String</code>. To simplify the storage, we handle each of them as
	 * an <code>Object</code> anyway, so <code>int</code>s and
	 * <code>float</code>s are wrapped by <code>Integer</code> and
	 * <code>Float</code> respectively.
	 * 
	 * @param input
	 *            A <code>String</code>.
	 * @return An <code>Object</code> which is either a <code>String,</code>
	 *         <code>Integer</code> or <code>Float</code>.
	 */
	private Object makeObject(String input) {
		// We are expecting an integer, real or string.
		Object output;
		try {
			output = Integer.valueOf(input);
			return output;
		} catch (NumberFormatException e1) {
			try {
				output = Float.valueOf(input);
				return output;
			} catch (NumberFormatException e2) {
				return input;
			}
		}
	}

	/**
	 * Raise an exception - look down through stack frames for an exception
	 * handler.
	 * 
	 * This method uses the {@link PAL#currentException
	 * <code>currentException</code>} variable to determine which exception to
	 * raise. Exception 1 (Program Abort) cannot be caught, so the program just
	 * terminates. All other exceptions are treated equally.
	 * 
	 * @param currInst
	 *            The <code>Code</code> object which caused the exception. Used
	 *            to add information to error messages.
	 */
	private ExitStatus raiseException(Code currInst) {
		// The Program Abort signal cannot be caught.
		if (currentException == programAbort) {
			error(currInst, "A Program Abort signal was raised.");
			return ExitStatus.ABNORMAL;
		}

		Data handlerLocation, dynamicLink;
		int handlerAddress;

		boolean moreFrames = true;

		while (true) {
			handlerLocation = dataStack.get(0, -1);

			if (handlerLocation.getType() != Data.INT) {
				error(currInst, "Exception handler address must be an integer.");
				return ExitStatus.ABNORMAL;
			}

			handlerAddress = ((Integer) handlerLocation.getValue()).intValue();

			if (handlerAddress < 0 || handlerAddress > codeMem.size()) {
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
				int baseAddr = ((Integer) dynamicLink.getValue()).intValue();
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
	 * Print an error. Errors are almost invariably unrecoverable, so this
	 * method announces the error, prints the offending instruction and dumps
	 * the stack.
	 * 
	 * @param currInst
	 *            The offending <code>Code</code> object.
	 * @param s
	 *            A context-dependent error message to be printed.
	 */
	private void error(Code currInst, String s) {
		// Ensure the error is always started on a new line.
		err.println();
		err.println("Runtime Error:");
		err.println(filename + ":" + currInst.getLineNo() + ":" + s);
		err.println(currInst);
		err.println("\nStack dump:");
		err.println("----------");
		err.print(dataStack);
		return;
	}

	/**
	 * Simple usage information.
	 *
	 * @param stream
	 *            stream to print the usage message to
	 */
	private static void usage(PrintStream stream) {
		stream.println("usage: java -jar PAL.jar [filename]");
		return;
	}
}
