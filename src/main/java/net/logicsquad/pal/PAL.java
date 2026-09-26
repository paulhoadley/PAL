package net.logicsquad.pal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Command line entry point: works out what to run, wires a {@link Loader} to a
 * {@link Machine}, and turns the result into a process exit code.
 *
 * <p>
 * Deciding to stop the JVM happens here and nowhere else, in exactly one
 * place: {@link PAL#main(String[])} does nothing but exit with what the rest
 * of the class worked out. The loader throws and the machine returns; neither
 * of them calls {@code System.exit()}, which is what lets both be
 * driven from a test.
 *
 * <p>
 * The class keeps its name because it is what {@code java -jar}
 * invokes.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class PAL {
	/** No instances. */
	private PAL() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Main method for command line operation.
	 *
	 * @param args
	 *            an optional object file, and the options described by
	 *            {@link PAL#help(PrintStream)}
	 */
	public static void main(String[] args) {
		System.exit(run(args).exitCode());
	}

	/**
	 * Does what the command line asks, and reports how it went.
	 *
	 * @param args
	 *            command line arguments
	 * @return the status the process should exit with
	 */
	private static ExitStatus run(String[] args) {
		Options options;
		try {
			options = Options.parse(args);
		} catch (UsageException e) {
			System.err.println(e.getMessage());
			usage(System.err);
			return ExitStatus.COULD_NOT_START;
		}

		return switch (options.mode()) {
		case HELP -> {
			// Asked for, so it is output rather than a diagnostic.
			help(System.out);
			yield ExitStatus.NORMAL;
		}
		case VERSION -> {
			System.out.println("PAL " + Version.version());
			yield ExitStatus.NORMAL;
		}
		case RUN -> execute(options);
		};
	}

	/**
	 * Loads and runs the program the options name.
	 *
	 * @param options
	 *            what to run, and how
	 * @return the status the process should exit with
	 */
	private static ExitStatus execute(Options options) {
		String filename = options.filename();

		// Loaded to completion before anything is run, which keeps the two
		// files' failures apart: either can be missing, and each deserves to
		// be named in its own diagnostic.
		Program program;
		try (InputStream is = new FileInputStream(filename)) {
			program = Loader.load(is, filename, options.codeSize());
		} catch (FileNotFoundException e) {
			return cannotOpen(filename);
		} catch (LoadException e) {
			System.err.println(filename + ":" + e.lineno() + ": "
					+ e.getMessage());
			return ExitStatus.ABNORMAL;
		} catch (IOException e) {
			System.err.println("Error reading " + filename + ": "
					+ e.getMessage());
			return ExitStatus.ABNORMAL;
		}

		if (options.input().isEmpty()) {
			return machine(program, options, System.in).execute();
		}

		String input = options.input().get();
		try (InputStream is = new FileInputStream(input)) {
			return machine(program, options, is).execute();
		} catch (FileNotFoundException e) {
			return cannotOpen(input);
		} catch (IOException e) {
			System.err.println("Error reading " + input + ": "
					+ e.getMessage());
			return ExitStatus.ABNORMAL;
		}
	}

	/**
	 * Returns a {@link Machine} set up as the options ask.
	 *
	 * @param program
	 *            the program to run
	 * @param options
	 *            the data stack limit, and whether to trace
	 * @param in
	 *            stream the running program reads from
	 * @return a machine ready to execute
	 */
	private static Machine machine(Program program, Options options,
			InputStream in) {
		// A trace goes to standard error: standard output belongs to the
		// program, and a trace mixed into its output would be no use to
		// anything reading it.
		Tracer tracer = options.trace() ? Tracer.to(System.err) : Tracer.none();

		return new Machine(program, in, System.out, System.err,
				options.dataSize(), tracer);
	}

	/**
	 * Reports a file that could not be opened.
	 *
	 * @param filename
	 *            the file named on the command line
	 * @return {@link ExitStatus#COULD_NOT_START}, for the caller to return
	 */
	private static ExitStatus cannotOpen(String filename) {
		System.err.println("Cannot open " + filename + ".");
		usage(System.err);

		return ExitStatus.COULD_NOT_START;
	}

	/**
	 * Prints how to invoke the machine.
	 *
	 * @param stream
	 *            stream to print the usage message to
	 */
	private static void usage(PrintStream stream) {
		stream.println("usage: java -jar pal.jar [options] [objectfile]");
	}

	/**
	 * Prints the usage message and what the options do.
	 *
	 * <p>
	 * The option names are spelled out here rather than built from the
	 * constants in {@link Options}, because this is prose and needs to line up
	 * as prose. {@code HelpTest} checks that every option
	 * {@link Options} knows about appears in it, which is the drift that
	 * matters.
	 *
	 * @param stream
	 *            stream to print the help to
	 */
	static void help(PrintStream stream) {
		usage(stream);
		stream.println(String.format("""

				Runs a PAL object file, or ./%s if none is named.

				Options:
				  -h, --help         print this message and exit
				      --version      print the version and exit
				      --trace        trace each instruction on standard error
				      --input=FILE   read program input from FILE, not the console
				      --code-size=N  allow N instructions rather than %d
				      --data-size=N  allow N data stack words rather than %d\
				""", Options.DEFAULT_FILENAME, Loader.CODESIZE, Machine.DATASIZE));
	}
}
