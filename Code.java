/* Code.java */

/* $Id$ */

/**
 * A class to represent an instruction and its arguments or operands.
 *
 * @version $Revision$
 * @author Philip Roberts &lt;philip.roberts@student.adelaide.edu.au&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class Code {
    /** 
     * The three-letter mnemonic for this instruction.
     * @see Mnemonic
     */
    private String mnemonic;

    /** The first argument to the instruction. */
    private int first;

    /** The second argument to the instruction. */
    private Object second;

    /** The line number from the source file. */
    private int lineno;

    /**
     * Constructor.
     *
     * @param mnemonic A <code>String</code> containing the three
     * letter mnemonic for this <code>Code</code> object.
     * @param first An <code>int</code> representing the first
     * argument to the instruction.  If there is no first argument,
     * this value should be zero.
     * @param second An <code>Object</code> representing the second
     * argument to the instruction.  This argument is represented as
     * an <code>Object</code> because the actual type can be integer,
     * real or string.  These types are represented by Java
     * <code>Integer</code>s, <code>Float</code>s and
     * <code>String</code>s respectively.
     * @param lineno the line number in the source file where this
     * instruction originated.
     */
    public Code(String mnemonic, int first, Object second, int lineno) {
	this.mnemonic = mnemonic;
	this.first = first;
	this.second = second;
	this.lineno = lineno;
	return;
    }

    /**
     * Returns the three letter mnemonic for this instruction.
     *
     * @return A <code>String</code> containing the three letter
     * mnemonic for this instruction.
     */
    public String getMnemonic() {
	return mnemonic;
    }

    /**
     * Returns the first argument to this instruction.
     *
     * @return An <code>int</code> representing the first argument to
     * this instruction.
     */
    public int getFirst() {
	return first;
    }

    /**
     * Returns the second argument to this instruction.
     *
     * @return An <code>Object</code> representing the second argument
     * to this instruction.
     * @see Code#Code
     */
    public Object getSecond() {
	return second;
    }

    /**
     * Returns the line number in the source file where this
     * instruction originated.
     *
     * @return The line number in the source file.
     */
    public int getLineNo() {
	return lineno;
    }

    /**
     * Returns a <code>String</code> representation of this object.
     *
     * @return A <code>String</code> representation of this object.
     */
    public String toString() {
	return (mnemonic + " " + first + " " + second);
    }
}
