/* PAL.java */

/* $Id$ */

import java.io.*;
import java.util.*;

/**
 * The PAL abstract machine simulator.
 */
public class PAL {
    /** The filename containing the program. */
    private static String filename = "CODE";

    /** A constant for code and data memory limits. */
    private final int CODESIZE = 1000;
    private final int DATASIZE = 500;

    /** Memory for the instructions. */
    private ArrayList codeMem;

    /** Stack for data. */
    private DataStack dataStack;

    /**
     * Main method for command line operation.
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
     * Constructor.
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
		st = new StringTokenizer(line);

		// May not come in groups of three, in which case,
		// catch the error.
		try {
		    mnemonic = st.nextToken();
		    first = Integer.parseInt(st.nextToken());
		    String s = st.nextToken();
		    if (s.startsWith("'")) {
			int start = line.indexOf('\'');
			System.err.println(start);
			int end = line.substring(start+1).indexOf('\'');
			System.err.println(end);
			second = line.substring(start - 1, start + end + 2).trim();
		    } else {
			second = makeObject(s);
		    }
		} catch (NoSuchElementException e) {
		    System.err.println("Not enough tokens on line " + lineno);
		}
		codeMem.add(new Code(mnemonic, first, second, lineno));
		line = br.readLine();
		lineno++;
	    }
	} catch (IOException e) {
	    System.err.println(e);
	}
	return;
    }

    /**
     * Execute the instructions in the machine's code memory.
     */
    private void execute() {
	System.out.println("execute()");

	// Dump the code store.
	for (int i = 0; i < codeMem.size(); i++) {
	    System.out.println(codeMem.get(i));
	}

	// The program counter.
	int pc = 0;

	Code nextInst;

	while (pc < codeMem.size()) {
	    nextInst = (Code)codeMem.get(pc);
	    System.out.println("Current instruction: " + nextInst);

	    // Object to pull out of nextInst.second.
	    Object o = nextInst.getSecond();

	    switch (Mnemonic.mnemonicToInt(nextInst.getMnemonic())) {
	    case Mnemonic.INC:
		if (!(o instanceof Integer)) {
		    error(nextInst, "Argument to INC must be an integer.");
                    die(1);
                } else {
                    dataStack.incTop(((Integer)o).intValue());
                }
                break;
	    case Mnemonic.LCI:
		if (!(o instanceof Integer)) {
		    error(nextInst, "Argument to LCI must be an integer.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.INT, o));
                }
                break;
	    case Mnemonic.LCR:
		if (!(o instanceof Float)) {
		    error(nextInst, "Argument to LCR must be a real.");
                    die(1);
                } else {
                    dataStack.push(new Data(Data.REAL, o));
                }
                break;
	    case Mnemonic.LCS:
		if (!(o instanceof String)) {
		    error(nextInst, "Argument to LCS must be a string.");
		    die(1);
		} else {
		    if (!(((String)o).startsWith("'") && (((String)o).endsWith("'")))) {
			error(nextInst, "String must be delimited by single-quotes.");
			die(1);
		    } else {
			dataStack.push(new Data(Data.STRING, ((String)o).substring(1, ((String)o).length() - 1)));
		    }
		}
		break;
	    case Mnemonic.OPR:
		doOperation(nextInst);
		break;
	    default:
		System.out.println(nextInst.getMnemonic() + ": not implemented.");
	    }

	    // Bump the program counter/
	    pc++;
	}

	return;
    }

    public void doOperation(Code nextInst) {
	Object o = nextInst.getSecond();
	int opr;
	if (!(o instanceof Integer)) {
	    error(nextInst, "Argument to OPR must be an integer.");
	    die(1);
	}
	opr = ((Integer)o).intValue();
	if (opr < 0 || opr > 31) {
	    error(nextInst, "Argument to OPR must be in range 0-31.");
	    die(1);
	}
	switch (opr) {
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
	    // Pop values at TOS and TOS-1, add them and push result
	    // onto TOS.
	    Data val1 = dataStack.pop();
	    Data val2 = dataStack.pop();
	    if (val1.getType() != val2.getType()) {
		error(nextInst, "Values for arithmetic operations must be of same type.");
		die(1);
	    } else {
		int type = val1.getType();
		if (type != Data.INT && type != Data.REAL) {
		    error(nextInst, "Values for arithmetic operations must be of type integer or real.");
		    die(1);
		}
		if (type == Data.INT) {
		    int int1 = ((Integer)val1.getValue()).intValue();
		    int int2 = ((Integer)val2.getValue()).intValue();
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
		    float flt1 = ((Float)val1.getValue()).floatValue();
		    float flt2 = ((Float)val2.getValue()).floatValue();
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
	case 9:
	    // Test if TOS is an odd integer.
	    if (dataStack.peek().getType() != Data.INT) {
		error(nextInst, "Argument to OPR 9 must be of type integer.");
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
	case 17:
	    // Push boolean true on TOS.
	    dataStack.push(new Data(Data.BOOL, new Boolean(true)));
	    break;
	case 18:
	    // Push boolean false on TOS
	    dataStack.push(new Data(Data.BOOL, new Boolean(false)));
	    break;
	case 20:
	    // Pop value on TOS and print it.
	    Object tos = dataStack.pop();
	    System.out.print(((Data)tos).getValue());
	    break;
	case 21:
	    // Print a newline.
	    System.out.println();
	    break;
	default:
	    System.out.println("OPR " + opr + ": not implemented.");
	}
	return;
    }

    /**
     * Make an Object from a String.  Because the type of the third
     * field in a single instruction is not pre-defined, we need to be
     * able to expect an integer, a float or a string.  To simplify
     * the storage, we handle each of them as an Object anyway, so
     * ints and floats are wrapped by Integer and Float respectively.
     *
     * @param input A String.
     * @return An Object which is either a String, Integer or Float.
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
     * Print an error.
     */
    private void error(Code nextInst, String s) {
	System.err.println("Error:");
	System.err.println(nextInst);
	System.err.println(s);
	System.err.print(dataStack);
	return;
    }

    /**
     * Die due to an error.
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
