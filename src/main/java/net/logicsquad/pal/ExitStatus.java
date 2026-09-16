package net.logicsquad.pal;

/**
 * How a program finished, and what the process should exit with.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
enum ExitStatus {
	/** The program executed a termination instruction. */
	NORMAL(0),

	/** It did not, whether through a fault or by running off the end. */
	ABNORMAL(1);

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
