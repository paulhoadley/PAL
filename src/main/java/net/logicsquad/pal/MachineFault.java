package net.logicsquad.pal;

/**
 * Something the running program asked the machine to do that it cannot: run
 * off the end of the data stack, pop a value that is not there, or read an
 * address outside the stack.
 *
 * <p>
 * The fault does not carry the offending instruction. It is raised in
 * {@link DataStack}, which has no idea which instruction is executing;
 * {@link PAL#execute()} catches it and renders it against the instruction it
 * was running at the time, in the same format as every other runtime
 * diagnostic.
 *
 * <p>
 * The type mismatches an instruction can provoke are not represented here. They
 * are still reported inline by <code>PAL.error()</code>, because #33 moves
 * operand checking to load time and will remove most of those sites rather than
 * convert them.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class MachineFault extends PALException {
	private static final long serialVersionUID = 1L;

	/**
	 * The kinds of fault the data stack can raise. Carried separately from the
	 * message so that a caller can tell them apart without matching on prose.
	 */
	enum Kind {
		/** The data stack has no room for another value. */
		STACK_OVERFLOW,

		/** A pop would have taken the current frame below its base. */
		STACK_UNDERFLOW,

		/** An address outside the occupied part of the data stack. */
		BAD_ADDRESS
	}

	/** What kind of fault this is. */
	private final Kind kind;

	/**
	 * Constructor.
	 *
	 * @param kind
	 *            what kind of fault this is
	 * @param message
	 *            a diagnostic addressed to whoever wrote the program
	 */
	private MachineFault(Kind kind, String message) {
		super(message);
		this.kind = kind;
		return;
	}

	/**
	 * Returns the kind of fault this is.
	 *
	 * @return the kind of fault
	 */
	Kind kind() {
		return kind;
	}

	/**
	 * A fault for a data stack with no room left.
	 *
	 * @param limit
	 *            the number of words the stack provides
	 * @return the fault to throw
	 */
	static MachineFault stackOverflow(int limit) {
		return new MachineFault(Kind.STACK_OVERFLOW,
				"Data stack exhausted: the limit is " + limit + " words.");
	}

	/**
	 * A fault for a pop with nothing on the current frame to pop.
	 *
	 * @return the fault to throw
	 */
	static MachineFault stackUnderflow() {
		return new MachineFault(Kind.STACK_UNDERFLOW,
				"Stack underflow: the current frame has no value to pop.");
	}

	/**
	 * A fault for an address outside the data stack.
	 *
	 * @param address
	 *            the address that was asked for
	 * @return the fault to throw
	 */
	static MachineFault badAddress(int address) {
		return new MachineFault(Kind.BAD_ADDRESS,
				"Address " + address + " is outside the data stack.");
	}
}
