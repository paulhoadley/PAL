/* PAL.java */

/* $Id$ */

import java.io.*;
import java.util.*;

/**
 * The PAL abstract machine simulator.
 *
 * @version $Revision$
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
	machine.execute();

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
		    System.err.println("Exceeded code storage limit at line " + lineno);
		    die(1);
		}
		st = new StringTokenizer(line);
		
		// It seems reasonable to allow blank lines in the
		// source.
		if (!(st.hasMoreTokens())) {
		    line = br.readLine();
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
			int end = line.substring(start+1).indexOf('\'');
			second = line.substring(start - 1, start + end + 2).trim();
		    } else {
			second = makeObject(s);
		    }
		} catch (NoSuchElementException e) {
		    System.err.println("Not enough tokens on line " + lineno);
		    die(1);
		}
		codeMem.add(new Code(mnemonic, first, second, lineno));
		line = br.readLine();
		lineno++;
	    }

            //Set up the input reader.
            inputReader = new BufferedReader(new InputStreamReader(System.in));
	} catch (IOException e) {
	    System.err.println(e);
	}
	return;
    }

    /**
     * Execute the instructions in the machine's code memory.  The
     * instructions are implemented in accordance with the
     * specification found here: <a
     * href="http://www.adelaide.edu.au/users/third/cc/handouts/pal.pdf">The
     * PAL Machine</a>.
     */
    private void execute() {
        //Initialise program counter.
        pc = 0;

	Code currInst;

	while (pc < codeMem.size()) {
	    currInst = (Code)codeMem.get(pc);
	    //System.out.println("Current instruction: " + currInst);

	    // Bump the program counter.
	    pc++;

	    // Object to pull out of currInst.second.
	    Object o = currInst.getSecond();

	    switch (Mnemonic.mnemonicToInt(currInst.getMnemonic())) {
            case Mnemonic.CAL:
                //Set return point field in stack mark.
                Data returnPoint = dataStack.get(dataStack.getTop() - currInst.getFirst() - 2);
                returnPoint.setType(Data.INT);
                returnPoint.setValue(new Integer(pc));

                //Set new frame base.
                dataStack.setBase(dataStack.getTop() - currInst.getFirst());

                pc = ((Integer)currInst.getSecond()).intValue() - 1;

                break;
	    case Mnemonic.INC:
		if (!(o instanceof Integer)) {
		    error(currInst, "Argument to INC must be an integer.");
                    die(1);
                } else {
                    dataStack.incTop(((Integer)o).intValue());
                }
                break;
            case Mnemonic.JIF:
                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to JIF must be an integer.");
                    die(1);
                }

                Data condition = dataStack.pop();

                if(condition.getType() != Data.BOOL) {
                    dataStack.push(condition);
                    error(currInst, "JIF - top of stack not a boolean.");
                    die(1);
                }

                if(!((Boolean)condition.getValue()).booleanValue()) {
                    int destination = ((Integer)o).intValue();

                    if(destination < 1 || destination > codeMem.size()) {
                        dataStack.push(condition);
                        error(currInst, "JIF - attempt to jump outside code.");
                        die(1);
                    }

                    //Our code store uses zero-based indexing.
                    //For compatibility reasons, addresses start at 1.
                    pc = destination - 1;
                }

                break;
            case Mnemonic.JMP:
                if (!(o instanceof Integer)) {
                    error(currInst, "Argument to JMP must be an integer.");
                    die(1);
                }

                int destination = ((Integer)o).intValue();

                if(destination == 0) {
                    System.out.println("\n### Execution of PAL-machine simulator complete\n");
                    die(0);
                }

                if(destination < 1 || destination > codeMem.size()) {
                    error(currInst, "JMP - attempt to jump outside code.");
                    die(1);
                }

                //Our code store uses zero-based indexing.
                //For compatibility reasons, addresses start at 1.
                pc = destination - 1;

                break;
	    case Mnemonic.LCI:
		if (!(o instanceof Integer)) {
		    error(currInst, "Argument to LCI must be an integer.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.INT, o));
                }
                break;
	    case Mnemonic.LCR:
                if(o instanceof Integer) {
                    o = new Float(((Integer)o).floatValue());
                }

		if (!(o instanceof Float)) {
		    error(currInst, "Argument to LCR must be a real.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.REAL, o));
                }
                break;
	    case Mnemonic.LCS:
		if (!(o instanceof String)) {
		    error(currInst, "Argument to LCS must be a string.");
		    die(1);
		} else {
		    if (!(((String)o).startsWith("'") && (((String)o).endsWith("'")))) {
			error(currInst, "String must be delimited by single-quotes.");
			die(1);
		    } else {
			dataStack.push(new Data(Data.STRING, ((String)o).substring(1, ((String)o).length() - 1)));
		    }
		}
		break;
            case Mnemonic.LDA:
                if(!(o instanceof Integer)) {
                    error(currInst, "Argument to LDA must be an integer.");
                    die(1);
                }

                int address = dataStack.getAddress(currInst.getFirst(), ((Integer)o).intValue());

                dataStack.push(new Data(Data.INT, new Integer(address)));

                break;
            case Mnemonic.LDI:
                Data tos = dataStack.pop();

                if(tos.getType() != Data.INT) {
                    dataStack.push(tos);
                    error(currInst, "LDI - top of stack must be an integer.");
                    die(1);
                }

                address = ((Integer)tos.getValue()).intValue();

                Data loadedVal = dataStack.get(address);

                dataStack.push((Data)loadedVal.clone());

                break;
            case Mnemonic.LDV:
                if(!(o instanceof Integer)) {
                    error(currInst, "Argument to LDV must be an integer");
                    die(1);
                }

                loadedVal = dataStack.get(currInst.getFirst(), ((Integer)o).intValue());

                dataStack.push((Data)loadedVal.clone());

                break;
            case Mnemonic.LDU:
                dataStack.push(new Data(Data.UNDEF, null));

                break;
            case Mnemonic.MST:
                int staticLink = dataStack.getAddress(currInst.getFirst(), 0);
                int dynamicLink = dataStack.getAddress(0, 0);

                dataStack.markStack(staticLink, dynamicLink);

                break;
	    case Mnemonic.OPR:
		doOperation(currInst);
		break;
	    case Mnemonic.RDI:
		// Read an integer from stdin
		String intLine = "";
		try {
		    intLine = inputReader.readLine();
		    if (intLine == null) {
			// EOF reached
			error(currInst, "EOF reached during integer read.");
			die(1);
		    }
		    int intVal = Integer.parseInt(intLine);
		    // Put the val in the stack
                    Data storeLocation = dataStack.get(currInst.getFirst(),  ((Integer)currInst.getSecond()).intValue());
		    storeLocation.setType(Data.INT);
                    storeLocation.setValue(new Integer(intVal));
                } catch (IOException e1) {
                    System.err.println(e1);
                } catch (NumberFormatException e2) {
                    error(currInst, "RDI: expecting integer, got " + intLine);
		    die(1);
		}
		break;
	    case Mnemonic.RDR:
		// Read a real from stdin
		String realLine = "";
		try {
		    realLine = inputReader.readLine();
		    if (realLine == null) {
			// EOF reached
			error(currInst, "EOF reached during real read.");
			die(1);
		    }
		    float realVal = Float.parseFloat(realLine);
		    // Put the val in the stack
                    Data storeLocation = dataStack.get(currInst.getFirst(), ((Integer)currInst.getSecond()).intValue());
                    storeLocation.setType(Data.REAL);
                    storeLocation.setValue(new Float(realVal));
		} catch (IOException e1) {
		    System.err.println(e1);
		} catch (NumberFormatException e2) {
		    error(currInst, "RDR: expecting real, got " + realLine);
		    die(1);
		}
		break;
            case Mnemonic.REH:
                if(!(o instanceof Integer)) {
                    error(currInst, "Argument to REH must be an integer.");
                    die(1);
                }

                // Get the location of the nearest exception handler pointer.
                address = dataStack.getAddress(0, -1);
                Data storeLocation = dataStack.get(address);

                storeLocation.setType(Data.INT);
                storeLocation.setValue(o);

                break;
            case Mnemonic.STI:
                tos = dataStack.pop();

                if(tos.getType() != Data.INT) {
                    dataStack.push(tos);
                    error(currInst, "STI - top of stack must be an integer.");
                    die(1);
                }

                Data storeVal = dataStack.pop();
                storeLocation = dataStack.get(((Integer)tos.getValue()).intValue());

                storeLocation.setType(storeVal.getType());
                storeLocation.setValue(storeVal.getValue());

                break;
            case Mnemonic.STO:
                if(!(o instanceof Integer)) {
                    error(currInst, "Argument to STO must be an integer.");
                    die(1);
                }

                storeVal = dataStack.pop();
                storeLocation = dataStack.get(currInst.getFirst(), ((Integer)o).intValue());

                storeLocation.setType(storeVal.getType());
                storeLocation.setValue(storeVal.getValue());

                break;
	    default:
		System.out.println(currInst.getMnemonic() + ": not implemented.");
	    }
	}

	return;
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
	opr = ((Integer)o).intValue();
	if (opr < 0 || opr > 31) {
	    error(currInst, "Argument to OPR must be in range 0-31.");
	    die(1);
	}
	switch (opr) {
        case 0:
            //Procedure return.

            //Set program counter.
            Data returnPoint = dataStack.get(0, -2);
            pc = ((Integer)returnPoint.getValue()).intValue();

            //Remember the dynamic link.
            Data dynamicLink = dataStack.get(0, -3);

            //Pop data from the stack back down to the last frame.
            int popCount = dataStack.getTop() - dataStack.getAddress(0, -4);

            for(int i = 0;i < popCount;i++) {
                dataStack.pop();
            }

            //Set the new frame base using the remembered dynamic link.
            dataStack.setBase(((Integer)dynamicLink.getValue()).intValue());

            break;
        case 1:
            //Function return.
            Data returnValue = dataStack.pop();

            //Set program counter.
            returnPoint = dataStack.get(0, -2);
            pc = ((Integer)returnPoint.getValue()).intValue();

            //Remember the dynamic link.
            dynamicLink = dataStack.get(0, -3);

            //Pop data from the stack back down to the last frame.
            popCount = dataStack.getTop() - dataStack.getAddress(0, -4);

            for(int i = 0;i < popCount;i++) {
                dataStack.pop();
            }

            //Set the new frame base using the remebered dynamic link.
            dataStack.setBase(((Integer)dynamicLink.getValue()).intValue());

            //Leave the return value on top of the stack.
            dataStack.push(returnValue);

            break;
	case 2:
	    // Negate the value on TOS if it is an integer or real.
	    Data d = dataStack.pop();
	    if (d.getType() == Data.INT) {
		d.setValue(new Integer(-(((Integer)d.getValue()).intValue())));
		dataStack.push(d);
	    } else if (d.getType() == Data.REAL) {
		d.setValue(new Float(-(((Float)d.getValue()).floatValue())));
		dataStack.push(d);
	    } else {
		dataStack.push(d);
	    }
	    break;
	case 3:
	case 4:
	case 5:
	case 6:
	    // Pop values at TOS and TOS-1,
	    // add/subtract/multiply/divide them (depending on the
	    // opcode) and push result onto TOS.
	    Data secondArg = dataStack.pop();
	    Data firstArg = dataStack.pop();
	    if (firstArg.getType() != secondArg.getType()) {
		dataStack.push(firstArg);
		dataStack.push(secondArg);
		error(currInst, "Values for arithmetic operations must be of same type.");
		die(1);
	    } else {
		int type = secondArg.getType();
		if (type != Data.INT && type != Data.REAL) {
		    dataStack.push(firstArg);
		    dataStack.push(secondArg);
		    error(currInst, "Values for arithmetic operations must be of type integer or real.");
		    die(1);
		}
		if (type == Data.INT) {
		    int int1 = ((Integer)firstArg.getValue()).intValue();
		    int int2 = ((Integer)secondArg.getValue()).intValue();
		    switch (opr) {
		    case 3:
			dataStack.push(new Data(Data.INT, new Integer(int1 + int2)));
			break;
		    case 4:
			dataStack.push(new Data(Data.INT, new Integer(int1 - int2)));
			break;
		    case 5:
			dataStack.push(new Data(Data.INT, new Integer(int1 * int2)));
			break;
		    case 6:
			dataStack.push(new Data(Data.INT, new Integer(int1 / int2)));
			break;
		    default:
		    }
		} else {
		    float flt1 = ((Float)firstArg.getValue()).floatValue();
		    float flt2 = ((Float)secondArg.getValue()).floatValue();
		    switch (opr) {
		    case 3:
			dataStack.push(new Data(Data.REAL, new Float(flt1 + flt2)));
			break;
		    case 4:
			dataStack.push(new Data(Data.REAL, new Float(flt1 - flt2)));
			break;
		    case 5:
			dataStack.push(new Data(Data.REAL, new Float(flt1 * flt2)));
			break;
		    case 6:
			dataStack.push(new Data(Data.REAL, new Float(flt1 / flt2)));
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
	    Data exp = dataStack.pop();
	    int baseType = dataStack.peek().getType();
	    if (baseType != Data.INT && baseType != Data.REAL) {
		error(currInst, "Base must be of type integer or real.");
		die(1);
	    }
	    Data base = dataStack.pop();
	    if (baseType == Data.INT) {
		int intAnswer = (int)Math.pow(((Integer)base.getValue()).intValue(), ((Integer)exp.getValue()).intValue());
		dataStack.push(new Data(Data.INT, new Integer(intAnswer)));
	    } else {
		float floatAnswer = (float)Math.pow(((Float)base.getValue()).floatValue(), ((Integer)exp.getValue()).intValue());
		dataStack.push(new Data(Data.REAL, new Float(floatAnswer)));
	    }
	    break;
	case 8:
	    Data str1 = dataStack.pop();
	    Data str2 = dataStack.pop();
	    if (str1.getType() != Data.STRING || str2.getType() != Data.STRING) {
		dataStack.push(str1);
		dataStack.push(str2);
		error(currInst, "Both arguments to OPR 8 must be of type string.");
		die(1);
	    }
	    dataStack.push(new Data(Data.STRING, (String)(str2.getValue()) + (String)(str1.getValue())));
	    break;
	case 9:
	    // Test if TOS is an odd integer.
	    if (dataStack.peek().getType() != Data.INT) {
		error(currInst, "Argument to OPR 9 must be of type integer.");
		die(1);
	    } else {
		Data e = dataStack.pop();
		// NB the % operator will give a negative for a
		// negative number.
		if (Math.abs(((Integer)e.getValue()).intValue() % 2) == 1) {
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
            secondArg = dataStack.pop();
            firstArg = dataStack.pop();

            if (firstArg.getType() != secondArg.getType()) {
		dataStack.push(firstArg);
		dataStack.push(secondArg);
		error(currInst, "Values for arithmetic operations must be of same type.");
		die(1);
	    } else {
		int type = secondArg.getType();
		if (type != Data.INT && type != Data.REAL) {
		    dataStack.push(firstArg);
		    dataStack.push(secondArg);
		    error(currInst, "Values for arithmetic operations must be of type integer or real.");
		    die(1);
		}
		if (type == Data.INT) {
		    int int1 = ((Integer)firstArg.getValue()).intValue();
		    int int2 = ((Integer)secondArg.getValue()).intValue();
		    switch (opr) {
		    case 10:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 == int2)));
			break;
		    case 11:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 != int2)));
			break;
		    case 12:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 < int2)));
			break;
		    case 13:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 >= int2)));
			break;
		    case 14:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 > int2)));
			break;
		    case 15:
			dataStack.push(new Data(Data.BOOL, new Boolean(int1 <= int2)));
			break;
		    default:
		    }
		} else {
		    float flt1 = ((Float)firstArg.getValue()).floatValue();
		    float flt2 = ((Float)secondArg.getValue()).floatValue();
		    switch (opr) {
		    case 10:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 == flt2)));
			break;
		    case 11:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 != flt2)));
			break;
		    case 12:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 < flt2)));
			break;
		    case 13:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 >= flt2)));
			break;
		    case 14:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 > flt2)));
			break;
		    case 15:
			dataStack.push(new Data(Data.BOOL, new Boolean(flt1 <= flt2)));
			break;
		    default:
		    }
		}
	    }
	    break;
        case 16:
            // Logical complement the top element of the stack.
            Data tos = dataStack.pop();

            if(tos.getType() != Data.BOOL) {
                dataStack.push(tos);
                error(currInst, "OPR 0 16 - top of stack not a boolean.");
                die(1);
            }

            dataStack.push(new Data(Data.BOOL, new Boolean(!((Boolean)tos.getValue()).booleanValue())));

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
		PushbackInputStream pb = new PushbackInputStream(System.in);
		int nextByte = pb.read();
		if (nextByte == -1) {
		    dataStack.push(new Data(Data.BOOL, new Boolean(true)));
		} else {
		    dataStack.push(new Data(Data.BOOL, new Boolean(false)));
		    pb.unread(nextByte);
		}
	    } catch (IOException e) {
		System.err.println(e);
	    }
	    break;
	case 20:
	    // Pop value on TOS and print it.
	    if (dataStack.peek().getType() == Data.BOOL ||
		dataStack.peek().getType() == Data.UNDEF) {
		error(currInst, "OPR 20 can only print values of type integer, real or string.");
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
	    Data tmp1 = dataStack.pop();
	    Data tmp2 = dataStack.pop();
	    dataStack.push(tmp1);
	    dataStack.push(tmp2);
	    break;
	case 23:
	    // Duplicate the element at the top of the stack.
            Data target = dataStack.peek();
	    dataStack.push((Data)target.clone());
	    break;
	case 24:
	    // Discard the element at the top of the stack.
	    dataStack.pop();
	    break;
	case 25:
	    // Convert the integer at TOS to a real.
	    if (dataStack.peek().getType() != Data.INT) {
		error(currInst, "Integer to real conversion can only be performed on value of type integer.");
		die(1);
	    }
	    dataStack.push(new Data(Data.REAL, new Float((float)(((Integer)(dataStack.pop().getValue())).intValue()))));
	    break;
	case 26:
	    // Convert the real at TOS to an integer.
	    if (dataStack.peek().getType() != Data.REAL) {
		error(currInst, "Real to integer conversion can only be performed on value of type real.");
		die(1);
	    }
	    dataStack.push(new Data(Data.INT, new Integer((int)(((Float)(dataStack.pop().getValue())).floatValue()))));
	    break;
	case 27:
	    // Convert the integer at TOS to a string.
	    if (dataStack.peek().getType() != Data.INT) {
		error(currInst, "Integer to string conversion can only be performed on value of type integer.");
		die(1);
	    }
	    dataStack.push(new Data(Data.STRING, ((Integer)(dataStack.pop().getValue())).toString()));
	    break;
	case 28:
	    // Convert the real at TOS to a string.
	    if (dataStack.peek().getType() != Data.REAL) {
		error(currInst, "Real to string conversion can only be performed on value of type real.");
		die(1);
	    }
	    dataStack.push(new Data(Data.STRING, ((Float)(dataStack.pop().getValue())).toString()));
	    break;
	case 29:
	    // Logical and of two booleans.
	    Data bool1 = dataStack.pop();
	    Data bool2 = dataStack.pop();
	    if (bool1.getType() != Data.BOOL || bool2.getType() != Data.BOOL) {
		error(currInst, "Logical and can only be performed on values of type boolean.");
		die(1);
	    }
	    dataStack.push(new Data(Data.BOOL, new Boolean(((Boolean)(bool1.getValue())).booleanValue() && ((Boolean)(bool2.getValue())).booleanValue())));
	    break;
	case 30:
	    // Logical or of two booleans.
	    bool1 = dataStack.pop();
	    bool2 = dataStack.pop();
	    if (bool1.getType() != Data.BOOL || bool2.getType() != Data.BOOL) {
		error(currInst, "Logical or can only be performed on values of type boolean.");
		die(1);
	    }
	    dataStack.push(new Data(Data.BOOL, new Boolean(((Boolean)(bool1.getValue())).booleanValue() || ((Boolean)(bool2.getValue())).booleanValue())));
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
     * Print an error.  Errors are almost invariably unrecoverable, so
     * this method announces the error, prints the offending
     * instruction and dumps the stack.
     *
     * @param currInst The offending <code>Code</code> object.
     * @param s A context-dependent error message to be printed.
     */
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
	System.out.println("usage: [java] PAL [filename]");
	return;
    }
}
