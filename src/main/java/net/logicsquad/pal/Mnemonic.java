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
	 * {@code CAL}
	 */
	CAL(Kind.INTEGER),

	/**
	 * {@code INC}
	 */
	INC(Kind.INTEGER),

	/**
	 * {@code JIF}
	 */
	JIF(Kind.INTEGER),

	/**
	 * {@code JMP}
	 */
	JMP(Kind.INTEGER),

	/**
	 * {@code LCI}
	 */
	LCI(Kind.INTEGER),

	/**
	 * {@code LCR}
	 */
	LCR(Kind.REAL),

	/**
	 * {@code LCS}
	 */
	LCS(Kind.STRING),

	/**
	 * {@code LDA}
	 */
	LDA(Kind.INTEGER),

	/**
	 * {@code LDI}
	 */
	LDI(Kind.INTEGER),

	/**
	 * {@code LDU}
	 */
	LDU(Kind.INTEGER),

	/**
	 * {@code LDV}
	 */
	LDV(Kind.INTEGER),

	/**
	 * {@code MST}
	 */
	MST(Kind.INTEGER),

	/**
	 * {@code OPR}
	 */
	OPR(Kind.INTEGER),

	/**
	 * {@code RDI}
	 */
	RDI(Kind.INTEGER),

	/**
	 * {@code RDR}
	 */
	RDR(Kind.INTEGER),

	/**
	 * {@code REH}
	 */
	REH(Kind.INTEGER),

	/**
	 * {@code SIG}
	 */
	SIG(Kind.INTEGER),

	/**
	 * {@code STI}
	 */
	STI(Kind.INTEGER),

	/**
	 * {@code STO}
	 */
	STO(Kind.INTEGER);

	/**
	 * The kind of operand a mnemonic takes.
	 *
	 * <p>
	 * Every mnemonic takes an integer but two. {@code LCS} takes a string,
	 * and {@code LCR} a real, though it accepts an integer literal and
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
	 * Returns the {@code Mnemonic} named by {@code token}, if there
	 * is one. Unlike {@link #valueOf(String)}, an unrecognised token is not an
	 * exceptional condition: object files are user input, so the caller is
	 * expected to report the problem against the offending line.
	 *
	 * @param token
	 *            a candidate mnemonic, as it appears in an object file
	 * @return the matching {@code Mnemonic}, or an empty
	 *         {@code Optional} if {@code token} does not name one
	 */
	static Optional<Mnemonic> from(String token) {
		return Optional.ofNullable(LOOKUP.get(token));
	}
}
