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

    /**
     * Constructor.
     */
    public Code(String mnemonic, int first, Object second) {
	this.mnemonic = mnemonic;
	this.first = first;
	this.second = second;
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
}
