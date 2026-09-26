package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Tracer}.
 *
 * @author paulh
 */
public class TracerTest {
	/**
	 * An instruction at address three but on line seven, which is what an
	 * object file with blank lines in it looks like.
	 */
	private static final Instruction INSTRUCTION = new Instruction(Mnemonic.OPR, 0,
			new IntOperand(20), 7, "OPR 0 20 --print it");

	@Test
	public void aTraceReportsAddressLineValueAndSource() {
		assertEquals("trace: 3:7 tos=42 OPR 0 20 --print it\n",
				traced(Optional.of(new IntValue(42))));
	}

	@Test
	public void anEmptyFrameIsReportedRatherThanOmitted() {
		assertEquals("trace: 3:7 tos=- OPR 0 20 --print it\n",
				traced(Optional.empty()));
	}

	@Test
	public void theAddressAndTheLineNumberAreBothShownBecauseTheyDiffer() {
		String line = traced(Optional.empty());

		// Jump operands count instructions; a diagnostic cites a line of the
		// object file, blanks included. Showing one without the other would
		// leave the reader unable to match the trace to either.
		assertEquals(7, INSTRUCTION.lineno(),
				"the fixture's line number is not its address");
		assertTrue(line.contains("3:7"), "both appear: " + line);
	}

	@Test
	public void theSourceIsShownAsItWasWritten() {
		assertTrue(traced(Optional.empty()).contains("--print it"),
				"the comment survives, as it does in an error message");
	}

	@Test
	public void noneReportsNothingAndSaysSo() {
		Tracer none = Tracer.none();

		assertFalse(none.reports(), "so the machine can skip the work");
		// Still has to be safe to call.
		none.trace(1, INSTRUCTION, Optional.empty());
	}

	@Test
	public void aWritingTracerSaysItReports() {
		assertTrue(Tracer.to(new PrintStream(new ByteArrayOutputStream())).reports());
	}

	/**
	 * Traces {@link TracerTest#INSTRUCTION} at address three.
	 *
	 * @param top
	 *            what to report as the top of the stack
	 * @return what the tracer wrote
	 */
	private static String traced(Optional<Datum> top) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		Tracer.to(new PrintStream(buffer, true, StandardCharsets.UTF_8))
				.trace(3, INSTRUCTION, top);

		return buffer.toString(StandardCharsets.UTF_8);
	}
}
