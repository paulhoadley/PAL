package net.logicsquad.pal;

/**
 * A command line that cannot be acted on.
 *
 * <p>
 * Deliberately not a {@link PALException}: that hierarchy means the program
 * being run is at fault, and here there is no program, only a mistaken
 * invocation. The two exit the process with different codes for the same
 * reason.
 *
 * <p>
 * Unchecked, because the only caller that can do anything useful with one is
 * {@link PAL#main(String[])}.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class UsageException extends RuntimeException {
	/** Serialisation version. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message
	 *            a diagnostic addressed to whoever typed the command
	 */
	UsageException(String message) {
		super(message);
	}
}
