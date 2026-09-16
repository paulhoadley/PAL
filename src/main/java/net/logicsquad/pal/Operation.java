package net.logicsquad.pal;

import java.util.Optional;

/**
 * The operations {@code OPR} can perform, which the manual identifies
 * only by number.
 *
 * <p>
 * Naming them does two things. A {@code switch} over this enum is
 * exhaustive, so the machine can no longer quietly reach a case it does not
 * handle, and a diagnostic can say which operation failed rather than leaving
 * the reader to look the number up.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
enum Operation {
	/** {@code OPR 0 0}: return from a procedure. */
	PROCEDURE_RETURN(0, "procedure return"),

	/** {@code OPR 0 1}: return from a function, leaving its value. */
	FUNCTION_RETURN(1, "function return"),

	/** {@code OPR 0 2}: negate the value on top of the stack. */
	NEGATE(2, "negate"),

	/** {@code OPR 0 3}: addition. */
	ADD(3, "add"),

	/** {@code OPR 0 4}: subtraction. */
	SUBTRACT(4, "subtract"),

	/** {@code OPR 0 5}: multiplication. */
	MULTIPLY(5, "multiply"),

	/** {@code OPR 0 6}: division. */
	DIVIDE(6, "divide"),

	/** {@code OPR 0 7}: exponentiation. */
	POWER(7, "power"),

	/** {@code OPR 0 8}: string concatenation. */
	CONCATENATE(8, "concatenate"),

	/** {@code OPR 0 9}: test whether an integer is odd. */
	ODD(9, "odd"),

	/** {@code OPR 0 10}: equality. */
	EQUAL(10, "equality"),

	/** {@code OPR 0 11}: inequality. */
	NOT_EQUAL(11, "inequality"),

	/** {@code OPR 0 12}: less than. */
	LESS(12, "less than"),

	/** {@code OPR 0 13}: greater than or equal to. */
	GREATER_OR_EQUAL(13, "greater than or equal"),

	/** {@code OPR 0 14}: greater than. */
	GREATER(14, "greater than"),

	/** {@code OPR 0 15}: less than or equal to. */
	LESS_OR_EQUAL(15, "less than or equal"),

	/** {@code OPR 0 16}: logical complement. */
	NOT(16, "logical not"),

	/** {@code OPR 0 17}: push true. */
	TRUE(17, "push true"),

	/** {@code OPR 0 18}: push false. */
	FALSE(18, "push false"),

	/** {@code OPR 0 19}: test for end of input. */
	AT_EOF(19, "test for end of file"),

	/** {@code OPR 0 20}: print the value on top of the stack. */
	PRINT(20, "print"),

	/** {@code OPR 0 21}: print a newline. */
	NEWLINE(21, "print newline"),

	/** {@code OPR 0 22}: swap the top two values. */
	SWAP(22, "swap"),

	/** {@code OPR 0 23}: duplicate the value on top of the stack. */
	DUPLICATE(23, "duplicate"),

	/** {@code OPR 0 24}: discard the value on top of the stack. */
	DISCARD(24, "discard"),

	/** {@code OPR 0 25}: convert an integer to a real. */
	INT_TO_REAL(25, "integer to real"),

	/** {@code OPR 0 26}: convert a real to an integer. */
	REAL_TO_INT(26, "real to integer"),

	/** {@code OPR 0 27}: convert an integer to a string. */
	INT_TO_STRING(27, "integer to string"),

	/** {@code OPR 0 28}: convert a real to a string. */
	REAL_TO_STRING(28, "real to string"),

	/** {@code OPR 0 29}: logical and. */
	AND(29, "logical and"),

	/** {@code OPR 0 30}: logical or. */
	OR(30, "logical or"),

	/** {@code OPR 0 31}: test the current exception code. */
	TEST_EXCEPTION(31, "test exception");

	/** Operations by code, supporting {@link #fromCode(int)}. */
	private static final Operation[] BY_CODE = new Operation[values().length];

	static {
		for (Operation each : values()) {
			BY_CODE[each.code] = each;
		}
	}

	/** The number the manual gives this operation. */
	private final int code;

	/** What to call this operation in a diagnostic. */
	private final String description;

	/**
	 * Constructor.
	 *
	 * @param code
	 *            the number the manual gives this operation
	 * @param description
	 *            what to call it in a diagnostic
	 */
	Operation(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Returns the number the manual gives this operation.
	 *
	 * @return the operation's code
	 */
	int code() {
		return code;
	}

	/**
	 * Returns what to call this operation in a diagnostic.
	 *
	 * @return a description of the operation
	 */
	String description() {
		return description;
	}

	/**
	 * Returns the {@code Operation} numbered {@code code}, if there
	 * is one. An out of range code is not an exceptional condition: object
	 * files are user input, so the caller is expected to report the problem
	 * against the offending instruction.
	 *
	 * @param code
	 *            a candidate operation number
	 * @return the matching {@code Operation}, or an empty
	 *         {@code Optional} if {@code code} does not name one
	 */
	static Optional<Operation> fromCode(int code) {
		if (code < 0 || code >= BY_CODE.length) {
			return Optional.empty();
		}

		return Optional.of(BY_CODE[code]);
	}
}
