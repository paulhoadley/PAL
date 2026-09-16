package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import net.logicsquad.pal.Machines.Run;

/**
 * Covers every operation of the <code>OPR</code> instruction, codes 0 to 31,
 * plus the type checks that guard them. Expected values come from the manual
 * in <code>doc/PAL.tex</code>, not from observed behaviour.
 *
 * @author paulh
 */
public class OperationTest {
	/**
	 * A program and the output it should produce on normal termination.
	 *
	 * @param name
	 *            what is being tested
	 * @param program
	 *            object file text
	 * @param expected
	 *            expected output
	 */
	private record Case(String name, String program, String expected) {
	}

	/**
	 * A program that should be refused, and a phrase its complaint should
	 * contain.
	 *
	 * @param name
	 *            what is being tested
	 * @param program
	 *            object file text
	 * @param complaint
	 *            phrase expected in the error message
	 */
	private record Refusal(String name, String program, String complaint) {
	}

	private static final List<Case> CASES = List.of(
		new Case("OPR 0: procedure return", """
				JMP 0 5
				LCS 0 'proc'
				OPR 0 20
				OPR 0 0
				MST 0 0
				CAL 0 2
				LCS 0 '!'
				OPR 0 20
				JMP 0 0
				""", "proc!"),
		new Case("OPR 1: function return", """
				JMP 0 4
				LCI 0 42
				OPR 0 1
				MST 0 0
				CAL 0 2
				OPR 0 20
				JMP 0 0
				""", "42"),
		new Case("OPR 2: negation", "LCI 0 5\nOPR 0 2\nOPR 0 20\nJMP 0 0\n", "-5"),
		new Case("OPR 2: negation of a real", "LCR 0 1.5\nOPR 0 2\nOPR 0 20\nJMP 0 0\n", "-1.5"),
		new Case("OPR 3: addition", "LCI 0 2\nLCI 0 3\nOPR 0 3\nOPR 0 20\nJMP 0 0\n", "5"),
		new Case("OPR 4: subtraction", "LCI 0 5\nLCI 0 3\nOPR 0 4\nOPR 0 20\nJMP 0 0\n", "2"),
		new Case("OPR 5: multiplication", "LCI 0 2\nLCI 0 3\nOPR 0 5\nOPR 0 20\nJMP 0 0\n", "6"),
		new Case("OPR 6: division truncates toward zero",
				"LCI 0 7\nLCI 0 2\nOPR 0 6\nOPR 0 20\nJMP 0 0\n", "3"),
		new Case("OPR 6: real division", "LCR 0 3.0\nLCR 0 2.0\nOPR 0 6\nOPR 0 20\nJMP 0 0\n",
				"1.5"),
		new Case("OPR 7: exponentiation", "LCI 0 2\nLCI 0 3\nOPR 0 7\nOPR 0 20\nJMP 0 0\n", "8"),
		new Case("OPR 7: real base keeps its type",
				"LCR 0 2.0\nLCI 0 3\nOPR 0 7\nOPR 0 20\nJMP 0 0\n", "8.0"),
		new Case("OPR 8: string concatenation",
				"LCS 0 'ab'\nLCS 0 'cd'\nOPR 0 8\nOPR 0 20\nJMP 0 0\n", "abcd"),
		new Case("OPR 9: odd", Machines.printsBoolean("LCI 0 3", "OPR 0 9"), "true"),
		new Case("OPR 9: odd of an even number",
				Machines.printsBoolean("LCI 0 4", "OPR 0 9"), "false"),
		new Case("OPR 9: odd of a negative number",
				Machines.printsBoolean("LCI 0 3", "OPR 0 2", "OPR 0 9"), "true"),
		new Case("OPR 10: equality",
				Machines.printsBoolean("LCI 0 2", "LCI 0 2", "OPR 0 10"), "true"),
		new Case("OPR 11: inequality",
				Machines.printsBoolean("LCI 0 2", "LCI 0 2", "OPR 0 11"), "false"),
		new Case("OPR 12: less than",
				Machines.printsBoolean("LCI 0 1", "LCI 0 2", "OPR 0 12"), "true"),
		new Case("OPR 13: greater than or equal",
				Machines.printsBoolean("LCI 0 2", "LCI 0 2", "OPR 0 13"), "true"),
		new Case("OPR 14: greater than",
				Machines.printsBoolean("LCI 0 1", "LCI 0 2", "OPR 0 14"), "false"),
		new Case("OPR 15: less than or equal",
				Machines.printsBoolean("LCI 0 1", "LCI 0 2", "OPR 0 15"), "true"),
		new Case("OPR 15: comparison of reals",
				Machines.printsBoolean("LCR 0 1.5", "LCR 0 1.5", "OPR 0 15"), "true"),
		new Case("OPR 16: logical not", Machines.printsBoolean("OPR 0 17", "OPR 0 16"), "false"),
		new Case("OPR 17: true", Machines.printsBoolean("OPR 0 17"), "true"),
		new Case("OPR 18: false", Machines.printsBoolean("OPR 0 18"), "false"),
		new Case("OPR 19: end of file", Machines.printsBoolean("OPR 0 19"), "true"),
		new Case("OPR 20: write", "LCS 0 'x'\nOPR 0 20\nJMP 0 0\n", "x"),
		new Case("OPR 21: newline", "LCS 0 'a'\nOPR 0 20\nOPR 0 21\nJMP 0 0\n", "a\n"),
		new Case("OPR 22: swap", "LCI 0 1\nLCI 0 2\nOPR 0 22\nOPR 0 20\nOPR 0 20\nJMP 0 0\n",
				"12"),
		new Case("OPR 23: duplicate", "LCI 0 7\nOPR 0 23\nOPR 0 20\nOPR 0 20\nJMP 0 0\n", "77"),
		new Case("OPR 24: drop", "LCI 0 1\nLCI 0 2\nOPR 0 24\nOPR 0 20\nJMP 0 0\n", "1"),
		new Case("OPR 25: integer to real", "LCI 0 3\nOPR 0 25\nOPR 0 20\nJMP 0 0\n", "3.0"),
		new Case("OPR 26: real to integer", "LCR 0 3.7\nOPR 0 26\nOPR 0 20\nJMP 0 0\n", "3"),
		new Case("OPR 27: integer to string",
				"LCI 0 5\nOPR 0 27\nLCS 0 'x'\nOPR 0 8\nOPR 0 20\nJMP 0 0\n", "5x"),
		new Case("OPR 28: real to string",
				"LCR 0 1.5\nOPR 0 28\nLCS 0 'x'\nOPR 0 8\nOPR 0 20\nJMP 0 0\n", "1.5x"),
		new Case("OPR 29: logical and",
				Machines.printsBoolean("OPR 0 17", "OPR 0 18", "OPR 0 29"), "false"),
		new Case("OPR 29: logical and of two trues",
				Machines.printsBoolean("OPR 0 17", "OPR 0 17", "OPR 0 29"), "true"),
		new Case("OPR 30: logical or",
				Machines.printsBoolean("OPR 0 17", "OPR 0 18", "OPR 0 30"), "true"),
		new Case("OPR 31: test exception matches", """
				REH 0 4
				SIG 0 7
				JMP 0 0
				LCI 0 7
				OPR 0 31
				JIF 0 10
				LCS 0 'true'
				OPR 0 20
				JMP 0 0
				LCS 0 'false'
				OPR 0 20
				JMP 0 0
				""", "true"),
		new Case("OPR 31: test exception does not match", """
				REH 0 4
				SIG 0 7
				JMP 0 0
				LCI 0 3
				OPR 0 31
				JIF 0 10
				LCS 0 'true'
				OPR 0 20
				JMP 0 0
				LCS 0 'false'
				OPR 0 20
				JMP 0 0
				""", "false"));

	private static final List<Refusal> REFUSALS = List.of(
		new Refusal("OPR 2: cannot negate a string", "LCS 0 'x'\nOPR 0 2\nJMP 0 0\n",
				"negate: cannot negate a boolean, string or undefined value."),
		new Refusal("OPR 3: operands must match",
				"LCI 0 1\nLCR 0 1.0\nOPR 0 3\nJMP 0 0\n", "same type"),
		new Refusal("OPR 3: operands must be numeric",
				"LCS 0 'a'\nLCS 0 'b'\nOPR 0 3\nJMP 0 0\n", "integer or real"),
		new Refusal("OPR 6: division by zero", "LCI 0 1\nLCI 0 0\nOPR 0 6\nJMP 0 0\n",
				"divide by zero"),
		new Refusal("OPR 6: real division by zero",
				"LCR 0 1.0\nLCR 0 0.0\nOPR 0 6\nJMP 0 0\n", "divide by zero"),
		new Refusal("OPR 7: exponent must be an integer",
				"LCI 0 2\nLCR 0 2.0\nOPR 0 7\nJMP 0 0\n", "power: exponent must be an integer."),
		new Refusal("OPR 8: operands must be strings",
				"LCI 0 1\nLCI 0 2\nOPR 0 8\nJMP 0 0\n",
				"concatenate: both operands must be strings."),
		new Refusal("OPR 9: operand must be an integer",
				"LCS 0 'x'\nOPR 0 9\nJMP 0 0\n", "odd: operand must be an integer."),
		new Refusal("OPR 16: operand must be a boolean",
				"LCI 0 1\nOPR 0 16\nJMP 0 0\n", "must be a boolean"),
		new Refusal("OPR 20: will not print a boolean",
				"OPR 0 17\nOPR 0 20\nJMP 0 0\n", "can only print"),
		new Refusal("OPR 20: will not print an undefined value",
				"LDU 0 0\nOPR 0 20\nJMP 0 0\n", "can only print"),
		new Refusal("OPR 25: operand must be an integer",
				"LCR 0 1.0\nOPR 0 25\nJMP 0 0\n",
				"integer to real: operand must be an integer."),
		new Refusal("OPR 26: operand must be a real",
				"LCI 0 1\nOPR 0 26\nJMP 0 0\n",
				"real to integer: operand must be a real."),
		new Refusal("OPR 27: operand must be an integer",
				"LCR 0 1.0\nOPR 0 27\nJMP 0 0\n",
				"integer to string: operand must be an integer."),
		new Refusal("OPR 28: operand must be a real",
				"LCI 0 1\nOPR 0 28\nJMP 0 0\n",
				"real to string: operand must be a real."),
		new Refusal("OPR 29: operands must be booleans",
				"LCI 0 1\nLCI 0 2\nOPR 0 29\nJMP 0 0\n",
				"logical and: both operands must be booleans."),
		new Refusal("OPR 30: operands must be booleans",
				"LCI 0 1\nLCI 0 2\nOPR 0 30\nJMP 0 0\n",
				"logical or: both operands must be booleans."),
		new Refusal("OPR 31: operand must be an integer",
				"LCS 0 'x'\nOPR 0 31\nJMP 0 0\n",
				"test exception: operand must be an integer."),
		new Refusal("OPR 10: comparison operands must match",
				"LCI 0 1\nLCR 0 1.0\nOPR 0 10\nJMP 0 0\n",
				"equality: operands must be of the same type."),
		new Refusal("OPR 15: comparison operands must be numeric",
				"LCS 0 'a'\nLCS 0 'b'\nOPR 0 15\nJMP 0 0\n",
				"less than or equal: operands must be integer or real."));

	/**
	 * Every operation that should succeed.
	 *
	 * @return one test per case
	 */
	@TestFactory
	public Stream<DynamicTest> operations() {
		return CASES.stream().map(each -> DynamicTest.dynamicTest(each.name(), () -> {
			Run run = Machines.run(each.program());
			assertEquals(each.expected(), run.output(), each.name());
			assertTrue(run.normal(), each.name() + " should terminate normally");
		}));
	}

	/**
	 * Every operation that should be refused, with the machine stopping.
	 *
	 * @return one test per case
	 */
	@TestFactory
	public Stream<DynamicTest> refusals() {
		return REFUSALS.stream().map(each -> DynamicTest.dynamicTest(each.name(), () -> {
			Run run = Machines.run(each.program());
			assertFalse(run.normal(), each.name() + " should not terminate normally");
			assertTrue(run.output().contains(each.complaint()),
					each.name() + ": expected a complaint containing \"" + each.complaint()
							+ "\" but got:\n" + run.output());
		}));
	}

	/**
	 * <code>OPR 0 19</code> reports end of file only once the input really is
	 * exhausted, and must not consume the byte it peeks at.
	 */
	@TestFactory
	public Stream<DynamicTest> endOfFile() {
		return Stream.of(
			DynamicTest.dynamicTest("OPR 19: input remaining", () -> {
				Run run = Machines.run(Machines.printsBoolean("OPR 0 19"), "7\n");
				assertEquals("false", run.output());
			}),
			DynamicTest.dynamicTest("OPR 19: peek does not consume", () -> {
				// Test for end of file, then read the value anyway.
				Run run = Machines.run("""
						INC 0 1
						OPR 0 19
						OPR 0 24
						RDI 0 0
						LDV 0 0
						OPR 0 20
						JMP 0 0
						""", "7\n");
				assertEquals("7", run.output());
				assertTrue(run.normal());
			}));
	}
}
