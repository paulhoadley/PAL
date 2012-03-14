/* Code.java */

/**
 * PAL Machine Simulator: An implementation in Java
 *
 * Copyright (c) 2002, 2003 Philip J. Roberts and Paul A. Hoadley
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the authors nor the names of any other
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * A class to represent an instruction and its arguments or operands.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
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
