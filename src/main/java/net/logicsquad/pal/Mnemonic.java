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
	CAL,

	/**
	 * <code>INC</code>
	 */
	INC,

	/**
	 * <code>JIF</code>
	 */
	JIF,

	/**
	 * <code>JMP</code>
	 */
	JMP,

	/**
	 * <code>LCI</code>
	 */
	LCI,

	/**
	 * <code>LCR</code>
	 */
	LCR,

	/**
	 * <code>LCS</code>
	 */
	LCS,

	/**
	 * <code>LDA</code>
	 */
	LDA,

	/**
	 * <code>LDI</code>
	 */
	LDI,

	/**
	 * <code>LDU</code>
	 */
	LDU,

	/**
	 * <code>LDV</code>
	 */
	LDV,

	/**
	 * <code>MST</code>
	 */
	MST,

	/**
	 * <code>OPR</code>
	 */
	OPR,

	/**
	 * <code>RDI</code>
	 */
	RDI,

	/**
	 * <code>RDR</code>
	 */
	RDR,

	/**
	 * <code>REH</code>
	 */
	REH,

	/**
	 * <code>SIG</code>
	 */
	SIG,

	/**
	 * <code>STI</code>
	 */
	STI,

	/**
	 * <code>STO</code>
	 */
	STO;

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
