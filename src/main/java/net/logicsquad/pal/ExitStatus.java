package net.logicsquad.pal;

/**
 * How a program finished, and what the process should exit with.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
enum ExitStatus {
	NORMAL(0),
	ABNORMAL(1);

	private final int exitCode;

	ExitStatus(int exitCode) {
		this.exitCode = exitCode;
		return;
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
