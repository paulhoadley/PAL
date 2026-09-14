package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.logicsquad.pal.Machines.Run;

/**
 * Tests on the object file parsing done when a machine is constructed. Only
 * covers files that load: the loader still calls <code>System.exit()</code>
 * when it rejects one, so malformed files are covered by
 * {@link NegativeFixtureTest} instead.
 *
 * <p>
 * The grammar is in <code>doc/PAL.tex</code>: three whitespace-separated
 * fields, an optional trailing comment, strings delimited by single quotes.
 *
 * @author paulh
 */
public class LoaderTest {
	@Test
	public void blankLinesAreAllowed() {
		Run run = Machines.run("\n\nLCS 0 'x'\n\nOPR 0 20\n\n\nJMP 0 0\n");
		assertEquals("x", run.output());
		assertTrue(run.normal());
	}

	@Test
	public void addressesCountInstructionsNotSourceLines() {
		// Five instructions spread over seven lines. Jumping to address 4
		// must land on OPR 0 20, skipping the second string, which only holds
		// if blank lines are excluded from the instruction store.
		Run run = Machines.run("""
				LCS 0 'a'

				JMP 0 4

				LCS 0 'b'
				OPR 0 20
				JMP 0 0
				""");
		assertEquals("a", run.output());
		assertTrue(run.normal());
	}

	@Test
	public void errorsReportTheSourceLineIncludingBlanks() {
		// The offending instruction is the fourth line of the file, though
		// only the second instruction.
		Run run = Machines.run("\n\nLCS 0 'x'\nOPR 0 2\nJMP 0 0\n");
		assertTrue(run.output().contains(":4:"),
				"expected the source line number in: " + run.output());
	}

	@Test
	public void fieldsMayBeSeparatedByTabs() {
		Run run = Machines.run("LCS\t0\t'tabbed'\nOPR\t0\t20\nJMP\t0\t0\n");
		assertEquals("tabbed", run.output());
	}

	@Test
	public void trailingCommentsAreIgnored() {
		Run run = Machines.run("""
				LCI 0 2 --push two
				LCI 0 3 this is also a comment
				OPR 0 3 --add them
				OPR 0 20
				JMP 0 0 --done
				""");
		assertEquals("5", run.output());
		assertTrue(run.normal());
	}

	@Test
	public void anApostropheInACommentIsNotAString() {
		Run run = Machines.run("LCI 0 1 don't stop\nOPR 0 20\nJMP 0 0\n");
		assertEquals("1", run.output());
	}

	@Test
	public void stringsMayContainSpaces() {
		Run run = Machines.run("LCS 0 'a b   c'\nOPR 0 20\nJMP 0 0\n");
		assertEquals("a b   c", run.output());
	}

	@Test
	public void aCommentAfterAStringMayContainQuotes() {
		Run run = Machines.run("LCS 0 'x' and 'this' is a comment\nOPR 0 20\nJMP 0 0\n");
		assertEquals("x", run.output());
	}

	@Test
	public void emptyStringsAreAllowed() {
		Run run = Machines.run("LCS 0 ''\nOPR 0 20\nLCS 0 'after'\nOPR 0 20\nJMP 0 0\n");
		assertEquals("after", run.output());
	}

	@Test
	public void negativeIntegerOperandsAreAllowed() {
		Run run = Machines.run("LCI 0 -5\nOPR 0 20\nJMP 0 0\n");
		assertEquals("-5", run.output());
	}

	@Test
	public void loadingARealForLCRAcceptsAnIntegerLiteral() {
		// The manual allows LCR to take an integer, promoting it to a real.
		Run run = Machines.run("LCR 0 3\nOPR 0 20\nJMP 0 0\n");
		assertEquals("3.0", run.output());
	}

	@Test
	public void anEmptyProgramTerminatesAbnormally() {
		Run run = Machines.run("");
		assertTrue(run.output().contains("failed to execute a termination"),
				"expected a complaint about the missing JMP 0 0: " + run.output());
		assertTrue(!run.normal());
	}

	@Test
	public void fallingOffTheEndIsNotNormalTermination() {
		Run run = Machines.run("LCS 0 'x'\nOPR 0 20\n");
		// OPR 0 20 prints without a newline, so the complaint runs straight
		// on from the program's own output.
		assertEquals("x"
				+ "Program failed to execute a termination instruction (JMP 0 0)."
				+ System.lineSeparator(), run.output());
		assertTrue(!run.normal());
	}
}
