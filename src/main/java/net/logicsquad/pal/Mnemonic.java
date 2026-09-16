package net.logicsquad.pal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the set of instruction mnemonics.
 * 
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
enum Mnemonic {
	/**
	 * <code>CAL</code>
	 */
	CAL(Kind.INTEGER),

	/**
	 * <code>INC</code>
	 */
	INC(Kind.INTEGER),

	/**
	 * <code>JIF</code>
	 */
	JIF(Kind.INTEGER),

	/**
	 * <code>JMP</code>
	 */
	JMP(Kind.INTEGER),

	/**
	 * <code>LCI</code>
	 */
	LCI(Kind.INTEGER),

	/**
	 * <code>LCR</code>
	 */
	LCR(Kind.REAL),

	/**
	 * <code>LCS</code>
	 */
	LCS(Kind.STRING),

	/**
	 * <code>LDA</code>
	 */
	LDA(Kind.INTEGER),

	/**
	 * <code>LDI</code>
	 */
	LDI(Kind.INTEGER),

	/**
	 * <code>LDU</code>
	 */
	LDU(Kind.INTEGER),

	/**
	 * <code>LDV</code>
	 */
	LDV(Kind.INTEGER),

	/**
	 * <code>MST</code>
	 */
	MST(Kind.INTEGER),

	/**
	 * <code>OPR</code>
	 */
	OPR(Kind.INTEGER),

	/**
	 * <code>RDI</code>
	 */
	RDI(Kind.INTEGER),

	/**
	 * <code>RDR</code>
	 */
	RDR(Kind.INTEGER),

	/**
	 * <code>REH</code>
	 */
	REH(Kind.INTEGER),

	/**
	 * <code>SIG</code>
	 */
	SIG(Kind.INTEGER),

	/**
	 * <code>STI</code>
	 */
	STI(Kind.INTEGER),

	/**
	 * <code>STO</code>
	 */
	STO(Kind.INTEGER);

	/**
	 * The kind of operand a mnemonic takes.
	 *
	 * <p>
	 * Every mnemonic takes an integer but two. <code>LCS</code> takes a string,
	 * and <code>LCR</code> a real, though it accepts an integer literal and
	 * promotes it.
	 *
	 * <p>
	 * The jump and call instructions take what the manual calls an address.
	 * That is an integer here, not a kind of its own: an address is only
	 * checkable against the size of the code store, which is not known until
	 * loading finishes, so those stay the run time checks they have always
	 * been.
	 */
	enum Kind {
		/** An integer. */
		INTEGER,

		/** A real, or an integer literal to promote. */
		REAL,

		/** A string, delimited by apostrophes. */
		STRING
	}

	/** The kind of operand this mnemonic takes. */
	private final Kind kind;

	/**
	 * Constructor.
	 *
	 * @param kind
	 *            the kind of operand this mnemonic takes
	 */
	Mnemonic(Kind kind) {
		this.kind = kind;
		return;
	}

	/**
	 * Returns the kind of operand this mnemonic takes.
	 *
	 * @return the kind of operand this mnemonic takes
	 */
	Kind kind() {
		return kind;
	}

	/**
	 * Mnemonics by name, supporting {@link #from(String)}.
	 */
	private static final Map<String, Mnemonic> LOOKUP = new HashMap<>();

	static {
		for (Mnemonic each : values()) {
			LOOKUP.put(each.name(), each);
		}
	}

	/**
	 * Returns the <code>Mnemonic</code> named by <code>token</code>, if there
	 * is one. Unlike {@link #valueOf(String)}, an unrecognised token is not an
	 * exceptional condition: object files are user input, so the caller is
	 * expected to report the problem against the offending line.
	 *
	 * @param token
	 *            a candidate mnemonic, as it appears in an object file
	 * @return the matching <code>Mnemonic</code>, or an empty
	 *         <code>Optional</code> if <code>token</code> does not name one
	 */
	static Optional<Mnemonic> from(String token) {
		return Optional.ofNullable(LOOKUP.get(token));
	}
}
