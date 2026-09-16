package net.logicsquad.pal;

/**
 * One instruction, as the loader parsed it.
 *
 * <p>
 * The operand is already the type its mnemonic calls for, so the interpreter
 * takes it with {@link #intOperand()}, {@link #realOperand()} or
 * {@link #stringOperand()} rather than opening every instruction by checking
 * what it got.
 *
 * <p>
 * The source line is kept as written. Reporting a fault against a
 * reconstruction of the instruction meant showing something the author never
 * typed: a real operand appeared as the parsed <code>float</code> rather than
 * as written, comments vanished, and whitespace was normalised.
 *
 * @param mnemonic
 *            what the instruction does
 * @param level
 *            the first argument, a level difference for the instructions that
 *            address another frame and zero for the rest
 * @param operand
 *            the second argument
 * @param lineno
 *            the line of the object file this came from, counting blanks and
 *            comments, and so not generally the address a jump would use
 * @param source
 *            that line, as written
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record Instruction(Mnemonic mnemonic, int level, Operand operand, int lineno,
		String source) {
	/**
	 * Returns the operand as an integer.
	 *
	 * @return the operand's value
	 * @throws IllegalStateException
	 *             if the operand is not an integer, which the loader should
	 *             have made impossible
	 */
	int intOperand() {
		if (operand instanceof IntOperand value) {
			return value.value();
		}

		throw new IllegalStateException(
				mnemonic + " reached the machine with a " + operand);
	}

	/**
	 * Returns the operand as a real.
	 *
	 * @return the operand's value
	 * @throws IllegalStateException
	 *             if the operand is not a real, which the loader should have
	 *             made impossible
	 */
	float realOperand() {
		if (operand instanceof RealOperand value) {
			return value.value();
		}

		throw new IllegalStateException(
				mnemonic + " reached the machine with a " + operand);
	}

	/**
	 * Returns the operand as a string, without its delimiting apostrophes.
	 *
	 * @return the operand's value
	 * @throws IllegalStateException
	 *             if the operand is not a string, which the loader should have
	 *             made impossible
	 */
	String stringOperand() {
		if (operand instanceof StringOperand value) {
			return value.value();
		}

		throw new IllegalStateException(
				mnemonic + " reached the machine with a " + operand);
	}

	/**
	 * Returns the instruction as it was written.
	 *
	 * @return the source line
	 */
	@Override
	public String toString() {
		return source;
	}
}
