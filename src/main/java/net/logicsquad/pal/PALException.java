package net.logicsquad.pal;

/**
 * Base class for the faults a PAL program can provoke.
 *
 * <p>
 * The distinction this hierarchy draws is between a fault in the program being
 * run and a bug in the simulator running it. A <code>PALException</code> always
 * means the former: the object file was malformed, or the program did something
 * the machine forbids. Anything else propagating out of the machine is the
 * latter, and is left alone so that it surfaces as the stack trace it is,
 * rather than being reported to the user as though their program were at fault.
 *
 * <p>
 * These are unchecked because every one of them is fatal to the run. There is
 * no point at which the machine can sensibly carry on after raising one, so
 * there is nothing to be gained by making every frame between the fault and
 * {@link PAL#main(String[])} declare it.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
abstract sealed class PALException extends RuntimeException
		permits LoadException, MachineFault {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message
	 *            a diagnostic addressed to whoever wrote the program
	 */
	PALException(String message) {
		super(message);
		return;
	}
}
