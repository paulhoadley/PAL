package net.logicsquad.pal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper for running a short program against a {@link PAL} machine in
 * process, without disturbing the system streams.
 *
 * <p>
 * Only useful for programs that load successfully. A program the loader
 * rejects calls <code>System.exit()</code> and would take the test JVM with
 * it; those belong in <code>src/test/resources/negative</code>, driven by
 * {@link NegativeFixtureTest}.
 *
 * @author paulh
 */
final class Machines {
	private Machines() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Runs a program with no console input.
	 *
	 * @param program
	 *            object file text
	 * @return what the machine wrote, and how it finished
	 */
	static Run run(String program) {
		return run(program, "");
	}

	/**
	 * Runs a program.
	 *
	 * @param program
	 *            object file text
	 * @param input
	 *            console input
	 * @return what the machine wrote, and how it finished
	 */
	static Run run(String program, String input) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8);
		PAL machine = new PAL(
				new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)),
				new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
				stream, stream);
		PAL.ExitStatus status = machine.execute();
		stream.flush();
		return new Run(buffer.toString(StandardCharsets.UTF_8), status);
	}

	/**
	 * Builds a program that evaluates to a boolean and prints
	 * <code>true</code> or <code>false</code>. Needed because
	 * <code>OPR 0 20</code> refuses to print a boolean, so the only way to
	 * observe one is to branch on it.
	 *
	 * @param setup
	 *            instructions leaving a single boolean on the stack
	 * @return a complete program
	 */
	static String printsBoolean(String... setup) {
		int lines = setup.length;
		StringBuilder program = new StringBuilder();
		for (String line : setup) {
			program.append(line).append('\n');
		}
		// Addresses are 1-based, so the "false" arm starts at lines + 5.
		program.append("JIF 0 ").append(lines + 5).append('\n');
		program.append("LCS 0 'true'\n");
		program.append("OPR 0 20\n");
		program.append("JMP 0 0\n");
		program.append("LCS 0 'false'\n");
		program.append("OPR 0 20\n");
		program.append("JMP 0 0\n");
		return program.toString();
	}

	/**
	 * The result of running a program.
	 *
	 * @param output
	 *            everything the machine wrote, program output and diagnostics
	 *            interleaved
	 * @param status
	 *            how the machine finished
	 */
	record Run(String output, PAL.ExitStatus status) {
		/**
		 * Did the machine terminate normally?
		 *
		 * @return <code>true</code> if it did
		 */
		boolean normal() {
			return status == PAL.ExitStatus.NORMAL;
		}
	}
}
