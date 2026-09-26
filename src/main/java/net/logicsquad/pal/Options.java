package net.logicsquad.pal;

import java.util.Optional;

/**
 * A parsed command line.
 *
 * <p>
 * Parsing is separated from {@link PAL} so that nearly all of the command
 * line's behaviour can be tested in process, leaving only the exit codes and
 * what lands on which stream to the subprocess tests.
 *
 * <p>
 * Options taking a value take it with an equals sign, as
 * {@code --code-size=2000}. Accepting a separate word as well would mean
 * deciding what {@code --input} followed by nothing means, and there is
 * nothing to be gained by having two spellings of every option.
 *
 * @param mode
 *            whether to run a program, or just say something and stop
 * @param filename
 *            the object file to run
 * @param input
 *            a file to read program input from, or empty to read the console
 * @param codeSize
 *            how many instructions to allow
 * @param dataSize
 *            how many words of data stack to allow
 * @param trace
 *            whether to report each instruction as it executes
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record Options(Mode mode, String filename, Optional<String> input, int codeSize,
		int dataSize, boolean trace) {
	/** What the command line is asking for. */
	enum Mode {
		/** Run a program. */
		RUN,

		/** Print the usage message. */
		HELP,

		/** Print the version. */
		VERSION
	}

	/** The object file to run when none is named. */
	static final String DEFAULT_FILENAME = "CODE";

	/** Option requesting the usage message. */
	static final String OPTION_HELP = "--help";

	/** Short form of {@link Options#OPTION_HELP}. */
	static final String OPTION_HELP_SHORT = "-h";

	/** Option requesting the version. */
	static final String OPTION_VERSION = "--version";

	/** Option requesting a trace. */
	static final String OPTION_TRACE = "--trace";

	/** Option naming a file to read program input from. */
	static final String OPTION_INPUT = "--input";

	/** Option setting the instruction limit. */
	static final String OPTION_CODE_SIZE = "--code-size";

	/** Option setting the data stack limit. */
	static final String OPTION_DATA_SIZE = "--data-size";

	/**
	 * Parses a command line.
	 *
	 * @param args
	 *            the arguments as given to {@link PAL#main(String[])}
	 * @return what they ask for
	 * @throws UsageException
	 *             if they cannot be acted on
	 */
	static Options parse(String[] args) {
		// Asking for help is answered with help, whatever else is on the line
		// and whatever order it came in. Validating first would mean that
		// "--code-size=oops --help" reported the mistake instead of
		// explaining how to avoid it.
		for (String arg : args) {
			if (OPTION_HELP.equals(arg) || OPTION_HELP_SHORT.equals(arg)) {
				return of(Mode.HELP);
			}
		}

		for (String arg : args) {
			if (OPTION_VERSION.equals(arg)) {
				return of(Mode.VERSION);
			}
		}

		String filename = null;
		Optional<String> input = Optional.empty();
		int codeSize = Loader.CODESIZE;
		int dataSize = Machine.DATASIZE;
		boolean trace = false;

		for (String arg : args) {
			if (!arg.startsWith("-")) {
				if (filename != null) {
					throw new UsageException(
							"Only one object file can be run at a time.");
				}
				filename = arg;
				continue;
			}

			String name = name(arg);
			switch (name) {
			case OPTION_TRACE -> {
				requireNoValue(arg, name);
				trace = true;
			}
			case OPTION_INPUT -> input = Optional.of(value(arg, name));
			case OPTION_CODE_SIZE -> codeSize = size(arg, name);
			case OPTION_DATA_SIZE -> dataSize = size(arg, name);
			default -> throw new UsageException("Unknown option '" + name + "'.");
			}
		}

		return new Options(Mode.RUN,
				filename == null ? DEFAULT_FILENAME : filename, input, codeSize,
				dataSize, trace);
	}

	/**
	 * Returns an {@code Options} asking only for {@code mode},
	 * for the cases that print something and run nothing.
	 *
	 * @param mode
	 *            what to do
	 * @return options in that mode, with everything else defaulted
	 */
	private static Options of(Mode mode) {
		return new Options(mode, DEFAULT_FILENAME, Optional.empty(),
				Loader.CODESIZE, Machine.DATASIZE, false);
	}

	/**
	 * Returns the option name in {@code arg}, which is everything
	 * before the equals sign if there is one.
	 *
	 * @param arg
	 *            a command line argument beginning with a dash
	 * @return the option name
	 */
	private static String name(String arg) {
		int equals = arg.indexOf('=');

		return equals < 0 ? arg : arg.substring(0, equals);
	}

	/**
	 * Returns the value given to an option.
	 *
	 * @param arg
	 *            the whole argument
	 * @param name
	 *            the option name
	 * @return the text after the equals sign
	 * @throws UsageException
	 *             if there is no equals sign, or nothing after it
	 */
	private static String value(String arg, String name) {
		int equals = arg.indexOf('=');
		if (equals < 0 || equals == arg.length() - 1) {
			throw new UsageException(
					name + " needs a value, as " + name + "=VALUE.");
		}

		return arg.substring(equals + 1);
	}

	/**
	 * Returns a limit given to an option.
	 *
	 * <p>
	 * Zero and negative are refused rather than taken to mean "no limit",
	 * which is what {@link DataStack} makes of them internally. A program that
	 * recurses without end is better met with the machine's own diagnostic
	 * than with the JVM eating memory until it is killed.
	 *
	 * @param arg
	 *            the whole argument
	 * @param name
	 *            the option name
	 * @return the limit
	 * @throws UsageException
	 *             if it is not a positive integer
	 */
	private static int size(String arg, String name) {
		String text = value(arg, name);
		int size;
		try {
			size = Integer.parseInt(text);
		} catch (NumberFormatException e) {
			throw new UsageException(name + " needs a positive integer, not '"
					+ text + "'.");
		}

		if (size <= 0) {
			throw new UsageException(name + " needs a positive integer, not '"
					+ text + "'.");
		}

		return size;
	}

	/**
	 * Checks that an option which takes no value was not given one.
	 *
	 * @param arg
	 *            the whole argument
	 * @param name
	 *            the option name
	 * @throws UsageException
	 *             if a value was given
	 */
	private static void requireNoValue(String arg, String name) {
		if (arg.indexOf('=') >= 0) {
			throw new UsageException(name + " takes no value.");
		}
	}
}
