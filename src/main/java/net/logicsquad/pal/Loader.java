package net.logicsquad.pal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Turns the text of an object file into a {@link Program}.
 *
 * <p>
 * Text in, instructions out, and nothing else: no streams, no machine state,
 * and no way to stop the JVM. A file it will not accept is refused by throwing
 * {@link LoadException}, which carries the offending line.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class Loader {
	/** How many instructions the code store holds unless told otherwise. */
	static final int CODESIZE = 1000;

	/** No instances. */
	private Loader() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Reads an object file.
	 *
	 * @param is
	 *            the object file
	 * @param name
	 *            what to call it in a diagnostic
	 * @return the loaded program
	 * @throws IOException
	 *             if the object file cannot be read
	 * @throws LoadException
	 *             if it is not a valid object file
	 */
	static Program load(InputStream is, String name) throws IOException {
		return load(is, name, CODESIZE);
	}

	/**
	 * Reads an object file into a code store of a given size.
	 *
	 * @param is
	 *            the object file
	 * @param name
	 *            what to call it in a diagnostic
	 * @param codeSize
	 *            how many instructions the code store holds
	 * @return the loaded program
	 * @throws IOException
	 *             if the object file cannot be read
	 * @throws LoadException
	 *             if it is not a valid object file
	 */
	static Program load(InputStream is, String name, int codeSize)
			throws IOException {
		List<Instruction> instructions = new ArrayList<>();

		BufferedReader br = new BufferedReader(
				new InputStreamReader(is, StandardCharsets.UTF_8));
		int lineno = 1;
		String line = br.readLine();
		StringTokenizer st;

		while (line != null) {
			st = new StringTokenizer(line);

			// It seems reasonable to allow blank lines in the
			// source.
			if (!(st.hasMoreTokens())) {
				line = br.readLine();
				lineno++;
				continue;
			}

			Mnemonic mnemonic;
			int level;
			String operandText;

			// May not come in groups of three, in which case,
			// catch the error.
			try {
				String token = st.nextToken();
				Optional<Mnemonic> parsed = Mnemonic.from(token);
				if (parsed.isEmpty()) {
					throw new LoadException(lineno,
							"Unknown mnemonic '" + token + "'.");
				}
				mnemonic = parsed.get();
				level = Integer.parseInt(st.nextToken());
				operandText = st.nextToken();
			} catch (NoSuchElementException e) {
				throw new LoadException(lineno, "Not enough tokens.");
			} catch (NumberFormatException e) {
				throw new LoadException(lineno,
						"First operand non-integer.");
			}

			Operand operand = operand(mnemonic, operandText, line, lineno);

			if (instructions.size() >= codeSize) {
				throw new LoadException(lineno,
						"Exceeded code storage limit.");
			}
			instructions.add(
					new Instruction(mnemonic, level, operand, lineno, line));
			line = br.readLine();
			lineno++;
		}

		return new Program(name, List.copyOf(instructions));
	}

	/**
	 * Parses the third field of an instruction as the kind of operand its
	 * mnemonic takes.
	 *
	 * @param mnemonic
	 *            the instruction's mnemonic, which decides what to parse
	 * @param token
	 *            the third whitespace-separated field
	 * @param line
	 *            the whole source line, needed because a string operand may
	 *            contain spaces and so span several tokens
	 * @param lineno
	 *            the source line number, for the diagnostic
	 * @return the parsed operand
	 * @throws LoadException
	 *             if the operand is not of the kind the mnemonic takes
	 */
	private static Operand operand(Mnemonic mnemonic, String token, String line,
			int lineno) {
		return switch (mnemonic.kind()) {
		case STRING -> {
			if (!token.startsWith("'")) {
				throw new LoadException(lineno,
						mnemonic + " takes a string operand.");
			}
			int start = line.indexOf('\'');
			int end = line.indexOf('\'', start + 1);
			if (end < 0) {
				throw new LoadException(lineno, "Unterminated string literal.");
			}
			yield new StringOperand(line.substring(start + 1, end));
		}
		case REAL -> {
			try {
				yield new RealOperand(Float.parseFloat(token));
			} catch (NumberFormatException e) {
				throw new LoadException(lineno,
						mnemonic + " takes a real operand.");
			}
		}
		case INTEGER -> {
			try {
				yield new IntOperand(Integer.parseInt(token));
			} catch (NumberFormatException e) {
				throw new LoadException(lineno,
						mnemonic + " takes an integer operand.");
			}
		}
		};
	}
}
