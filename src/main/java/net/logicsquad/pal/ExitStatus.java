package net.logicsquad.pal;

/**
 * How a run finished, and what the process should exit with.
 *
 * <p>
 * The line between {@link ExitStatus#ABNORMAL} and
 * {@link ExitStatus#COULD_NOT_START} is whose fault it was. A non-zero
 * {@code 1} says the program is wrong: it was malformed, or it did
 * something the machine forbids. A {@code 2} says the invocation is
 * wrong, and no program was run at all. A build script can tell the two apart
 * without reading the diagnostic.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
enum ExitStatus {
	/** The program executed a termination instruction. */
	NORMAL(0),

	/** It did not, whether through a fault or by running off the end. */
	ABNORMAL(1),

	/**
	 * There was no run: the command line was wrong, or a file it named could
	 * not be opened.
	 */
	COULD_NOT_START(2);

	/** The process exit code for this status. */
	private final int exitCode;

	/**
	 * Constructor.
	 *
	 * @param exitCode
	 *            the process exit code for this status
	 */
	ExitStatus(int exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * Returns the process exit code corresponding to this status.
	 *
	 * @return the process exit code
	 */
	int exitCode() {
		return exitCode;
	}
}
