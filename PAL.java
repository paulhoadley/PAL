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

    /** Memory for the instructions. */
    private ArrayList codeMem;

    /** Stack for data. */
    private Stack dataStack;

    /**
     * Main method for command line operation.
     */
    public static void main(String[] args) {
	// For now, accept only a single command line argument: a
	// filename.
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
	codeMem = new ArrayList();
	dataStack = new Stack();

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
		    second = makeObject(st.nextToken());
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

	    switch (Mnemonic.mnemonicToInt(nextInst.getMnemonic())) {
	    case Mnemonic.LCS:
		Object o = nextInst.getSecond();
		if (!(o instanceof String)) {
		    error(nextInst, "Argument to LCS must be a string.");
		    die(1);
		} else {
		    dataStack.push(o);
		}
		break;
	    case Mnemonic.OPR:
		doOperation(nextInst);
		break;
	    default:
		System.out.println(nextInst.getMnemonic() + ": not implemented.");
	    }

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
	case 20:
	    Object tos = dataStack.peek();
	    System.out.print(tos);
	    break;
	case 21:
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
	    output = new Integer(Integer.parseInt(input));
	    return output;
	} catch (NumberFormatException e1) {
	    try {
		output = new Float(Float.parseFloat(input));
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
