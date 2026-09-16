package net.logicsquad.pal;

/**
 * A malformed object file: the loader could not turn some line of it into an
 * instruction.
 *
 * <p>
 * Carries the source line number, which is the only context worth reporting for
 * a fault found before the program starts. Note that this is the line in the
 * file, counting blank and comment lines, and so is not generally the same as
 * the instruction address a jump would use.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class LoadException extends PALException {
	/** Serialisation version. */
	private static final long serialVersionUID = 1L;

	/** The source line carrying the fault. */
	private final int lineno;

	/**
	 * Constructor.
	 *
	 * @param lineno
	 *            the source line carrying the fault
	 * @param message
	 *            a diagnostic addressed to whoever wrote the program
	 */
	LoadException(int lineno, String message) {
		super(message);
		this.lineno = lineno;
	}

	/**
	 * Returns the source line carrying the fault.
	 *
	 * @return the source line number
	 */
	int lineno() {
		return lineno;
	}
}
