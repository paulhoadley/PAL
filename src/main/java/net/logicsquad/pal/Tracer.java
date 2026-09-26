package net.logicsquad.pal;

import java.io.PrintStream;
import java.util.Optional;

/**
 * Reports each instruction as the machine reaches it.
 *
 * <p>
 * Formatting lives here rather than in {@link Machine} so that the
 * interpreter's loop is not carrying a diagnostic's presentation around with
 * it, and so that what a trace looks like can be tested without running a
 * program.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
interface Tracer {
	/** What to show for the top of stack when the frame is empty. */
	String EMPTY = "-";

	/**
	 * Reports an instruction the machine is about to execute.
	 *
	 * <p>
	 * Called before the instruction runs, so the reported stack is what the
	 * instruction is about to work on rather than what it left behind. The
	 * effect shows up in the following line.
	 *
	 * @param address
	 *            the instruction's address, counting from one, which is what
	 *            {@code CAL}, {@code JMP} and {@code JIF} operands
	 *            refer to
	 * @param instruction
	 *            the instruction
	 * @param top
	 *            the value on top of the stack, or empty if the current frame
	 *            holds none
	 */
	void trace(int address, Instruction instruction, Optional<Datum> top);

	/**
	 * Whether this tracer reports anything.
	 *
	 * <p>
	 * Lets the machine skip the work of tracing rather than handing it to
	 * something that will throw it away. That work is not free: keeping the
	 * trace in step with the program's output means flushing it first, and a
	 * flush for every instruction of an untraced run would be a syscall for
	 * every instruction of an untraced run.
	 *
	 * @return {@code true} unless this tracer discards everything
	 */
	default boolean reports() {
		return true;
	}

	/**
	 * Returns a {@code Tracer} that reports nothing, for the usual case
	 * of a program run without tracing.
	 *
	 * @return a tracer that does nothing
	 */
	static Tracer none() {
		return new Tracer() {
			@Override
			public void trace(int address, Instruction instruction,
					Optional<Datum> top) {
				// Nothing to do: this is the "not tracing" case.
			}

			@Override
			public boolean reports() {
				return false;
			}
		};
	}

	/**
	 * Returns a {@code Tracer} writing one line per instruction to
	 * {@code stream}.
	 *
	 * <p>
	 * Both the address and the source line number are reported, because they
	 * are not the same thing: addresses count instructions, while a line
	 * number counts lines of the object file, blanks included. A jump operand
	 * is an address, and an error message cites a line number, so a trace that
	 * gave only one of them would be unreadable against the other.
	 *
	 * @param stream
	 *            where to write the trace
	 * @return a tracer writing to {@code stream}
	 */
	static Tracer to(PrintStream stream) {
		return (address, instruction, top) -> stream.println("trace: " + address
				+ ":" + instruction.lineno() + " tos="
				+ top.map(Object::toString).orElse(EMPTY) + " " + instruction);
	}
}
