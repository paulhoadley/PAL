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
		System.out.println("line " + lineno + ": " + mnemonic + " " + first + " " + second);
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
     * Simple usage information.
     */
    private static void usage() {
	System.out.println("usage: [java] PAL [filename]");
	return;
    }
}
