/* PAL.java */

/* $Id$ */

import java.io.*;
import java.util.*;

/**
 * The PAL abstract machine simulator.
 *
 * @version $Revision$
 * @author Philip Roberts &lt;philip.roberts@student.adelaide.edu.au&gt;
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
    private ArrayList codeMem;

    /** Stack for data. */
    private DataStack dataStack;

    /** The program counter. */
    private int pc;

    /** Input reader. */
    private BufferedReader inputReader;

    /** Wrapper to enable pushback of bytes into the input stream, for
     * OPR 19. */
    private PushbackReader pushBack;

    /** The number of the present exception. */
    private int currentException;

    /** Constants representing the predefined exception types. */
    private static final int reraise = 0;
    private static final int programAbort = 1;
    private static final int typeMismatch = 3;
    private static final int reachedEOF = 4;

    /**
     * Main method for command line operation.
     *
     * @param args Command line options are limited to a single
     * filename.
     */
    public static void main(String[] args) {
	if (args.length > 1) {
	    usage();
	    System.exit(1);
	} else if (args.length == 1) {
	    filename = args[0];
	}

	// Make a machine and load the code.
	PAL machine = new PAL();

	// Execute.
	try {
            machine.execute();
        } catch (OutOfMemoryError e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IndexOutOfBoundsException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

	return;
    }

    /**
     * Constructor.  Reads all of the statements in {@link
     * PAL#filename <code>filename</code>} into {@link Code
     * <code>Code</code>} objects, and stores these objects in {@link
     * PAL#codeMem <code>codeMem</code>}.  The lexical analysis of the
     * source file is quite rigid.  Any deviation from the prescribed
     * format for source files causes the machine to stop.
     */
    public PAL() {
	// Create the code memory.
	codeMem = new ArrayList(CODESIZE);
	dataStack = new DataStack(DATASIZE);

	try {
	    BufferedReader br = new BufferedReader(new FileReader(filename));
	    int lineno = 1;
	    String line = br.readLine();
	    String mnemonic = "";
	    int first = 0;
	    Object second = null;
	    StringTokenizer st;
	    
	    while (line != null) {
		if (lineno > CODESIZE) {
		    System.err.println("Exceeded code storage limit at line "
                                       + lineno);
		    die(1);
		}
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
		    mnemonic = st.nextToken();
		    first = Integer.parseInt(st.nextToken());
		    String s = st.nextToken();
		    if (s.startsWith("'")) {
			int start = line.indexOf('\'');
			int end = line.indexOf('\'', start + 1);
			second = line.substring(start, end + 1);
		    } else {
			second = makeObject(s);
                        if (second instanceof String) {
                            System.err.println("Unrecognised second operand"
                                               + " on line " + lineno);
                            die(1);
                        }
		    }
		} catch (NoSuchElementException e) {
		    System.err.println("Not enough tokens on line " + lineno);
		    die(1);
		} catch (NumberFormatException e) {
                    System.err.println("First operand non-integer on line "
                                       + lineno);
                    die(1);
                }
		codeMem.add(new Code(mnemonic, first, second, lineno));
		line = br.readLine();
		lineno++;
	    }

            // Set up the input reader.
	    pushBack = new PushbackReader(new InputStreamReader(System.in));
            // Note: the internal buffer of the BufferedReader is set
            // to 1 (the smallest possible) so that it won't buffer up
            // to EOF, thereby confusing OPR 19.
            inputReader = new BufferedReader(pushBack, 1);
	} catch (FileNotFoundException e) {
	    usage();
	    System.exit(1);
	} catch (IOException e) {
	    System.err.println(e);
	    System.exit(1);
	}

        currentException = 0;

	return;
    }

    /**
     * Execute the instructions in the machine's code memory.  The
     * instructions are implemented in accordance with the
     * specification found here: <a
     * href="http://www.cs.adelaide.edu.au/users/third/cc/handouts/pal.pdf">The
     * PAL Machine</a>.
     */
    private void execute() {
        // Initialise program counter.
        pc = 0;

	Code currInst;

	while (pc < codeMem.size()) {
	    currInst = (Code) codeMem.get(pc);

	    // Bump the program counter.
	    pc++;

	    // Object to pull out of currInst.second.
	    Object o = currInst.getSecond();

            Data tos, ntos, returnPoint, loadedVal;

	    switch (Mnemonic.mnemonicToInt(currInst.getMnemonic())) {
            case Mnemonic.CAL:
                // Procedure/function call.

                // Set return point field in stack mark.
                returnPoint = dataStack.get(dataStack.getTop()
                                            - currInst.getFirst() - 2);
                returnPoint.setType(Data.INT);
                returnPoint.setValue(new Integer(pc));

                // Set new frame base.
                dataStack.setBase(dataStack.getTop() - currInst.getFirst());

                // Jump to procedure/function code.  Note that while
                // the PAL instructions start from 1, our code store
                // is indexed from 0.
                pc = ((Integer) currInst.getSecond()).intValue() - 1;

                break;
	    case Mnemonic.INC:
                // Push space onto the stack.

		if (!(o instanceof Integer)) {
		    error(currInst, "Argument to INC must be an integer.");
                    die(1);
                } else {
                    dataStack.incTop(((Integer) o).intValue());
                }
                break;
            case Mnemonic.JIF:
                // Jump if false.

                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to JIF must be an integer.");
                    die(1);
                }

                tos = dataStack.pop();

                if (tos.getType() != Data.BOOL) {
                    dataStack.push(tos);
                    error(currInst, "JIF - top of stack not a boolean.");
                    die(1);
                }

                if (!((Boolean) tos.getValue()).booleanValue()) {
                    int destination = ((Integer) o).intValue();

                    if (destination < 1 || destination > codeMem.size()) {
                        dataStack.push(tos);
                        error(currInst, "JIF - attempt to jump outside code.");
                        die(1);
                    }

                    // Our code store uses zero-based indexing.  For
                    // compatibility reasons, addresses start at 1.
                    pc = destination - 1;
                }

                break;
            case Mnemonic.JMP:
                // Unconditional jump.

                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to JMP must be an integer.");
                    die(1);
                }

                int destination = ((Integer) o).intValue();

                if (destination == 0) {
                    // "JMP 0 0" signifies program termination.
                    die(0);
                }

                if (destination < 1 || destination > codeMem.size()) {
                    error(currInst, "JMP - attempt to jump outside code.");
                    die(1);
                }

                // Our code store uses zero-based indexing.  For
                // compatibility reasons, addresses start at 1.
                pc = destination - 1;

                break;
	    case Mnemonic.LCI:
                // Load an integer constant onto the stack.

		if (!(o instanceof Integer)) {
		    error(currInst, "Argument to LCI must be an integer.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.INT, o));
                }
                break;
	    case Mnemonic.LCR:
                // Load a real constant onto the stack.

                if (o instanceof Integer) {
                    o = new Float(((Integer) o).floatValue());
                }

		if (!(o instanceof Float)) {
		    error(currInst, "Argument to LCR must be a real.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.REAL, o));
                }
                break;
	    case Mnemonic.LCS:
                // Load a string constant onto the stack.

		if (!(o instanceof String)) {
		    error(currInst, "Argument to LCS must be a string.");
		    die(1);
		} else {
		    if (!(((String) o).startsWith("'")
                          && (((String) o).endsWith("'")))) {
			error(currInst,
                              "String must be delimited by single-quotes.");
			die(1);
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
                    die(1);
                }

                int address = dataStack.getAddress(currInst.getFirst(),
                                                   ((Integer) o).intValue());

                dataStack.push(new Data(Data.INT, new Integer(address)));

                break;
            case Mnemonic.LDI:
                // Load the value addressed by the top of stack.

                tos = dataStack.pop();

                if (tos.getType() != Data.INT) {
                    dataStack.push(tos);
                    error(currInst, "LDI - top of stack must be an integer.");
                    die(1);
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
                    die(1);
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
		doOperation(currInst);
		break;
	    case Mnemonic.RDI:
		// Read an integer from stdin. 

		String intLine = "";
		try {
		    intLine = inputReader.readLine();
		    if (intLine == null) {
			// EOF reached.
			currentException = reachedEOF;
                        raiseException(currInst);
		    }
		    int intVal = Integer.parseInt(intLine);
		    // Put the val in the stack.
                    loadedVal = dataStack.get(currInst.getFirst(),
                                              ((Integer) o).intValue());
		    loadedVal.setType(Data.INT);
                    loadedVal.setValue(new Integer(intVal));
                } catch (IOException e1) {
                    System.err.println(e1);
                } catch (NumberFormatException e2) {
                    currentException = typeMismatch;
                    raiseException(currInst);
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
                        raiseException(currInst);
		    }
		    float realVal = Float.parseFloat(realLine);
		    // Put the val in the stack.
                    loadedVal = dataStack.get(currInst.getFirst(),
                                              ((Integer) o).intValue());
                    loadedVal.setType(Data.REAL);
                    loadedVal.setValue(new Float(realVal));
		} catch (IOException e1) {
		    System.err.println(e1);
		} catch (NumberFormatException e2) {
                    currentException = typeMismatch;
                    raiseException(currInst);
		}
		break;
            case Mnemonic.REH:
                // Register an exception handler with the current
                // stack mark.

                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to REH must be an integer.");
                    die(1);
                }

                // Get the location of the exception handler pointer
                // in the highest stack mark.
                loadedVal = dataStack.get(0, -1);

                loadedVal.setType(Data.INT);
                loadedVal.setValue(o);

                break;
            case Mnemonic.SIG:
                // If the argument is 0 (the predefined "re-raise"
                // code), re-raise the current exception.  Otherwise,
                // raise the exception specified by the argument.

                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to SIG must be an integer.");
                    die(1);
                }

                int excType = ((Integer) o).intValue();
                if (excType != reraise) {
                    currentException = excType;
                } else {
                    // Re-raise the current exception.  SIG 0 0 is
                    // typically called by an exception handler when
                    // it can't handle the current exception type.  We
                    // don't want to run that same handler again!  A
                    // simple way to achieve this is to nullify the
                    // current exception handler pointer.
                    Data handlerLocation = dataStack.get(0, -1);
                    handlerLocation.setValue(new Integer(0));
                }

                // Raise the exception...
                raiseException(currInst);

                break;
            case Mnemonic.STI:
                // Store the value in the top-of-stack - 1 in the
                // address specified by the number in top-of-stack.

                tos = dataStack.pop();

                if (tos.getType() != Data.INT) {
                    dataStack.push(tos);
                    error(currInst, "STI - top of stack must be an integer.");
                    die(1);
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
                    die(1);
                }

                tos = dataStack.pop();
                loadedVal = dataStack.get(currInst.getFirst(),
                                          ((Integer) o).intValue());

                loadedVal.setType(tos.getType());
                loadedVal.setValue(tos.getValue());

                break;
	    default:
		System.out.println(currInst.getMnemonic()
                                   + ": not implemented.");
	    }
	}

	System.err.println("Program failed to execute a termination"
                           + " instruction (JMP 0 0).");
        die(1);
    }

    /**
     * Perform the operation referenced by an <code>OPR</code>
     * instruction.  This method is provided separately to {@link
     * PAL#execute <code>execute</code>} to avoid placing the rather
     * lengthy <code>switch</code> statement in that method.
     *
     * @param currInst The current <code>Code</code> object to be
     * executed.  If it reaches here, that object contains an
     * <code>OPR</code> mnemonic.
     */
    public void doOperation(Code currInst) {
	Object o = currInst.getSecond();
	int opr;
	if (!(o instanceof Integer)) {
	    error(currInst, "Argument to OPR must be an integer.");
	    die(1);
	}
	opr = ((Integer) o).intValue();
	if (opr < 0 || opr > 31) {
	    error(currInst, "Argument to OPR must be in range 0-31.");
	    die(1);
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

            // Pop data from the stack back down to the last frame.
            int popCount = dataStack.getTop() - dataStack.getAddress(0, -4);

            for (int i = 0;i < popCount;i++) {
                dataStack.pop();
            }

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

            // Pop data from the stack back down to the last frame.
            popCount = dataStack.getTop() - dataStack.getAddress(0, -4);

            for (int i = 0;i < popCount;i++) {
                dataStack.pop();
            }

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
		tos.setValue(new Integer(-oldValue));
	    } else if (tos.getType() == Data.REAL) {
                float oldValue = ((Float) tos.getValue()).floatValue();
		tos.setValue(new Float(-oldValue));
	    } else {
                error(currInst,
                      "Cannot negate boolean, string or UNDEF value.");
                die(1);
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
		error(currInst,
                      "Values for arithmetic operations must be"
                      + " of same type.");
		die(1);
	    } else {
		int type = tos.getType();
		if (type != Data.INT && type != Data.REAL) {
		    dataStack.push(ntos);
		    dataStack.push(tos);
		    error(currInst,
                          "Values for arithmetic operations must be"
                          + " of type integer or real.");
		    die(1);
		}
		if (type == Data.INT) {
		    int int1 = ((Integer) ntos.getValue()).intValue();
		    int int2 = ((Integer) tos.getValue()).intValue();
		    switch (opr) {
		    case 3:
			dataStack.push(new Data(Data.INT,
                                                new Integer(int1 + int2)));
			break;
		    case 4:
			dataStack.push(new Data(Data.INT,
                                                new Integer(int1 - int2)));
			break;
		    case 5:
			dataStack.push(new Data(Data.INT,
                                                new Integer(int1 * int2)));
			break;
		    case 6:
                        if (int2 == 0) {
                            dataStack.push(ntos);
                            dataStack.push(tos);
                            error(currInst, "Attempt to divide by zero.");
                            die(1);
                        }

			dataStack.push(new Data(Data.INT,
                                                new Integer(int1 / int2)));
			break;
		    default:
		    }
		} else {
		    float flt1 = ((Float) ntos.getValue()).floatValue();
		    float flt2 = ((Float) tos.getValue()).floatValue();
		    switch (opr) {
		    case 3:
			dataStack.push(new Data(Data.REAL,
                                                new Float(flt1 + flt2)));
			break;
		    case 4:
			dataStack.push(new Data(Data.REAL,
                                                new Float(flt1 - flt2)));
			break;
		    case 5:
			dataStack.push(new Data(Data.REAL,
                                                new Float(flt1 * flt2)));
			break;
		    case 6:
                        if (flt2 == 0) {
                            dataStack.push(ntos);
                            dataStack.push(tos);
                            error(currInst, "Attempt to divide by zero.");
                            die(1);
                        }

			dataStack.push(new Data(Data.REAL,
                                                new Float(flt1 / flt2)));
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
		die(1);
	    }
	    tos = dataStack.pop();
            int exponent = ((Integer) tos.getValue()).intValue();

	    int baseType = dataStack.peek().getType();
	    if (baseType != Data.INT && baseType != Data.REAL) {
		error(currInst, "Base must be of type integer or real.");
		die(1);
	    }
	    ntos = dataStack.pop();
	    if (baseType == Data.INT) {
                int base = ((Integer) ntos.getValue()).intValue();
		int intAnswer = (int) Math.pow(base, exponent);
		dataStack.push(new Data(Data.INT, new Integer(intAnswer)));
	    } else {
                float base = ((Float) ntos.getValue()).floatValue();
		float floatAnswer = (float) Math.pow(base, exponent);
		dataStack.push(new Data(Data.REAL, new Float(floatAnswer)));
	    }
	    break;
	case 8:
            // String concatenation.

	    tos = dataStack.pop();
	    ntos = dataStack.pop();
	    if (tos.getType() != Data.STRING
                || ntos.getType() != Data.STRING) {
		dataStack.push(ntos);
		dataStack.push(tos);
		error(currInst,
                      "Both arguments to OPR 8 must be of type string.");
		die(1);
	    }
            String sResult = (String) ntos.getValue();
            sResult += (String) tos.getValue();
	    dataStack.push(new Data(Data.STRING, sResult));
	    break;
	case 9:
	    // Test if TOS is an odd integer.

	    if (dataStack.peek().getType() != Data.INT) {
		error(currInst, "Argument to OPR 9 must be of type integer.");
		die(1);
	    } else {
		tos = dataStack.pop();
		// NB the % operator will give a negative for a
		// negative number.
		if (Math.abs(((Integer) tos.getValue()).intValue() % 2) == 1) {
		    dataStack.push(new Data(Data.BOOL, new Boolean(true)));
		} else {
		    dataStack.push(new Data(Data.BOOL, new Boolean(false)));
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
		error(currInst, "Values for arithmetic operations must be"
                      + " of same type.");
		die(1);
	    } else {
		int type = tos.getType();
		if (type != Data.INT && type != Data.REAL) {
		    dataStack.push(ntos);
		    dataStack.push(tos);
		    error(currInst, "Values for arithmetic operations must be"
                          + " of type integer or real.");
		    die(1);
		}
		if (type == Data.INT) {
		    int int1 = ((Integer) ntos.getValue()).intValue();
		    int int2 = ((Integer) tos.getValue()).intValue();
		    switch (opr) {
		    case 10:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 == int2)));
			break;
		    case 11:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 != int2)));
			break;
		    case 12:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 < int2)));
			break;
		    case 13:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 >= int2)));
			break;
		    case 14:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 > int2)));
			break;
		    case 15:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(int1 <= int2)));
			break;
		    default:
		    }
		} else {
		    float flt1 = ((Float) ntos.getValue()).floatValue();
		    float flt2 = ((Float) tos.getValue()).floatValue();
		    switch (opr) {
		    case 10:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 == flt2)));
			break;
		    case 11:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 != flt2)));
			break;
		    case 12:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 < flt2)));
			break;
		    case 13:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 >= flt2)));
			break;
		    case 14:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 > flt2)));
			break;
		    case 15:
			dataStack.push(new Data(Data.BOOL,
                                                new Boolean(flt1 <= flt2)));
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
                die(1);
            }

            boolean bResult = !((Boolean) tos.getValue()).booleanValue();
            dataStack.push(new Data(Data.BOOL, new Boolean(bResult)));
            break;
	case 17:
	    // Push boolean true on TOS.

	    dataStack.push(new Data(Data.BOOL, new Boolean(true)));
	    break;
	case 18:
	    // Push boolean false on TOS

	    dataStack.push(new Data(Data.BOOL, new Boolean(false)));
	    break;
	case 19:
	    // Test for EOF.

	    try {
		int nextByte = pushBack.read();
		if (nextByte == -1) {
		    dataStack.push(new Data(Data.BOOL, new Boolean(true)));
		} else {
		    dataStack.push(new Data(Data.BOOL, new Boolean(false)));
		    pushBack.unread(nextByte);
		}
	    } catch (IOException e) {
		System.err.println(e);
	    }
	    break;
	case 20:
	    // Pop value on TOS and print it.

	    if (dataStack.peek().getType() == Data.BOOL ||
		dataStack.peek().getType() == Data.UNDEF) {
		error(currInst, "OPR 20 can only print values"
                      + " of type integer, real or string.");
		die(1);
	    } else {
		tos = dataStack.pop();
		System.out.print(tos);
	    }
	    break;
	case 21:
	    // Print a newline.

	    System.out.println();
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
		die(1);
	    }
            float fAns = ((Integer) dataStack.pop().getValue()).floatValue();
	    dataStack.push(new Data(Data.REAL, new Float(fAns)));
	    break;
	case 26:
	    // Convert the real at TOS to an integer.

	    if (dataStack.peek().getType() != Data.REAL) {
		error(currInst, "Real to integer conversion can only be"
                      + " performed on a value of type real.");
		die(1);
	    }
            int iResult = ((Float) dataStack.pop().getValue()).intValue();
	    dataStack.push(new Data(Data.INT, new Integer(iResult)));
	    break;
	case 27:
	    // Convert the integer at TOS to a string.

	    if (dataStack.peek().getType() != Data.INT) {
		error(currInst, "Integer to string conversion can only be"
                      + " performed on a value of type integer.");
		die(1);
	    }
	    dataStack.push(new Data(Data.STRING,
                                    dataStack.pop().getValue().toString()));
	    break;
	case 28:
	    // Convert the real at TOS to a string.

	    if (dataStack.peek().getType() != Data.REAL) {
		error(currInst, "Real to string conversion can only be"
                      + " performed on value of type real.");
		die(1);
	    }
	    dataStack.push(new Data(Data.STRING,
                                    dataStack.pop().getValue().toString()));
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
		die(1);
	    }
            boolean bool1 = ((Boolean) tos.getValue()).booleanValue();
            boolean bool2 = ((Boolean) ntos.getValue()).booleanValue();
	    dataStack.push(new Data(Data.BOOL, new Boolean(bool1 && bool2)));
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
		die(1);
	    }
            bool1 = ((Boolean) tos.getValue()).booleanValue();
            bool2 = ((Boolean) ntos.getValue()).booleanValue();
	    dataStack.push(new Data(Data.BOOL, new Boolean(bool1 || bool2)));
	    break;
        case 31:
            // Test whether the current exception code is the same as
            // the integer on TOS.

            tos = dataStack.pop();
            if (tos.getType() != Data.INT) {
                dataStack.push(tos);
                error(currInst, "OPR 0 31 expects an integer value"
                      + "on top of the stack.");
                die(1);
            }

            int testValue = ((Integer) tos.getValue()).intValue();
            boolean pushValue = testValue == currentException;

            dataStack.push(new Data(Data.BOOL, new Boolean(pushValue)));
            break;
	default:
	    System.out.println("OPR " + opr + ": not implemented.");
	}
	return;
    }

    /**
     * Make an <code>Object</code> from a <code>String</code>.
     * Because the type of the third field in a single instruction is
     * not pre-defined, we need to be able to expect an
     * <code>int</code>, a <code>float</code> or a
     * <code>String</code>.  To simplify the storage, we handle each
     * of them as an <code>Object</code> anyway, so <code>int</code>s
     * and <code>float</code>s are wrapped by <code>Integer</code> and
     * <code>Float</code> respectively.
     *
     * @param input A <code>String</code>.
     * @return An <code>Object</code> which is either a
     * <code>String,</code> <code>Integer</code> or
     * <code>Float</code>.
     */
    private Object makeObject(String input) {
	// We are expecting an integer, real or string.
	Object output;
	try {
	    output = new Integer(input);
	    return output;
	} catch (NumberFormatException e1) {
	    try {
		output = new Float(input);
		return output;
	    } catch (NumberFormatException e2) {
		return input;
	    }
	}
    }

    /**
     * Raise an exception - look down through stack frames for an
     * exception handler.
     *
     * This method uses the {@link PAL#currentException
     * <code>currentException</code>} variable to determine which
     * exception to raise.  Exception 1 (Program Abort) cannot be
     * caught, so the program just terminates.  All other exceptions
     * are treated equally.
     * @param currInst The <code>Code</code> object which caused the
     * exception.  Used to add information to error messages.
     */
    private void raiseException(Code currInst) {
        // The Program Abort signal cannot be caught.
        if (currentException == programAbort) {
            error(currInst, "A Program Abort signal was raised.");
            die(1);
        }

        Data handlerLocation, dynamicLink;
        int handlerAddress;

        boolean moreFrames = true;

        while (true) {
            handlerLocation = dataStack.get(0, -1);

            if (handlerLocation.getType() != Data.INT) {
                error(currInst,
                      "Exception handler address must be an integer.");
                die(1);
            }

            handlerAddress = ((Integer) handlerLocation.getValue()).intValue();

            if (handlerAddress < 0 || handlerAddress > codeMem.size()) {
                error(currInst,
                      "Exception handler address out of code range.");
                die(1);
            }

            if (handlerAddress == 0) {
                // An address of 0 means no handler - throw away this
                // frame and keep searching.

                // First, check if this is the lowest frame - the
                // lowest frame's stack mark begins at address 0.
                if (dataStack.getAddress(0,-4) == 0) {
                    moreFrames = false;
                    break;
                }

                // Remember the dynamic link.
                dynamicLink = dataStack.get(0, -3);

                // Pop data from the stack back down to the previous
                // frame.
                int pops = dataStack.getTop() - dataStack.getAddress(0, -4);

                for (int i = 0;i < pops;i++) {
                    dataStack.pop();
                }

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
            error(currInst,
                  "Exception #" + currentException + " never handled!");
            die(1);
        }
    }

    /**
     * Print an error.  Errors are almost invariably unrecoverable, so
     * this method announces the error, prints the offending
     * instruction and dumps the stack.
     *
     * @param currInst The offending <code>Code</code> object.
     * @param s A context-dependent error message to be printed.  */
    private void error(Code currInst, String s) {
	// Ensure the error is always started on a new line.
	System.err.println();
	System.err.println("Runtime Error:");
	System.err.println(filename + ":" + currInst.getLineNo() + ":" + s);
	System.err.println(currInst);
	System.err.println("\nStack dump:");
	System.err.println("----------");
	System.err.print(dataStack);
	return;
    }

    /**
     * Die due to an error.
     *
     * @param err An arbitrary error code.  This integer is returned
     * to the operating system via the <code>System.exit</code>
     * method.
     */
    private void die(int err) {
	System.exit(err);
    }

    /**
     * Simple usage information.
     */
    private static void usage() {
	System.out.println("usage: java -jar PAL.jar [filename]");
	return;
    }
}
