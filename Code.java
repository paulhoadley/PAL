/* Code.java */

/* $Id$ */

/**
 * A class to represent an instruction and its arguments or operands.
 */
public class Code {
    /** The three-letter mnemonic for this instruction. */
    private String mnemonic;

    /** The first argument to the instruction. */
    private int first;

    /** The second argument to the instruction. */
    private Object second;

    /** The line number from the source file. */
    private int lineno;

    /**
     * Constructor.
     */
    public Code(String mnemonic, int first, Object second, int lineno) {
	this.mnemonic = mnemonic;
	this.first = first;
	this.second = second;
	this.lineno = lineno;
	return;
    }

    /**
     * Returns the three letter menmonic for this instruction.
     */
    public String getMnemonic() {
	return mnemonic;
    }

    /**
     * Returns the first argument to this instruction.
     */
    public int getFirst() {
	return first;
    }

    /**
     * Returns the second argument to this instruction.
     */
    public Object getSecond() {
	return second;
    }

    public String toString() {
	return (lineno + ": " + mnemonic + " " + first + " " + second);
    }
}
