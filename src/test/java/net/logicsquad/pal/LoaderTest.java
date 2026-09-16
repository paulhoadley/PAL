package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.logicsquad.pal.Machines.Run;

/**
 * Tests on the object file parsing done when a machine is constructed,
 * including the operand checking #33 moved here from run time. Since #29 the
 * loader throws rather than calling <code>System.exit()</code>, so a file it
 * rejects can be examined in process; {@link NegativeFixtureTest} still covers
 * what such a rejection looks like from the command line.
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
	public void anOperandOfTheWrongKindIsRefusedBeforeTheProgramRuns() {
		LoadException e = assertThrows(LoadException.class,
				() -> Machines.run("LCI 0 1.5\nJMP 0 0\n"));
		assertEquals(1, e.lineno());
		assertEquals("LCI takes an integer operand.", e.getMessage());
	}

	@Test
	public void eachMnemonicNamesTheOperandItWants() {
		assertEquals("LCS takes a string operand.",
				assertThrows(LoadException.class,
						() -> Machines.run("LCS 0 5\nJMP 0 0\n")).getMessage());
		assertEquals("LCR takes a real operand.",
				assertThrows(LoadException.class,
						() -> Machines.run("LCR 0 'x'\nJMP 0 0\n")).getMessage());
		assertEquals("OPR takes an integer operand.",
				assertThrows(LoadException.class,
						() -> Machines.run("OPR 0 'x'\nJMP 0 0\n")).getMessage());
	}

	@Test
	public void aStringOperandLosesItsApostrophesOnceNotOnEveryExecution() {
		// The delimiters used to survive into the machine and be stripped by
		// LCS each time it ran.
		Run run = Machines.run("LCS 0 'it''s'\nOPR 0 20\nJMP 0 0\n");
		assertEquals("it", run.output(), "the string ends at the second apostrophe");
		assertTrue(run.normal());
	}

	@Test
	public void aFaultNamesTheSourceLineAsWritten() {
		// Not a reconstruction: the comment and the spacing are the author's.
		Run run = Machines.run("INC 0 1\nOPR 0 24\nOPR   0   24  -- one pop too many\nJMP 0 0\n");
		assertTrue(run.output().contains("OPR   0   24  -- one pop too many"),
				"expected the source line verbatim: " + run.output());
		assertTrue(!run.normal());
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
