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
 * Deciding to stop the JVM happens here and nowhere else. The loader throws and
 * the machine returns; neither of them calls {@code System.exit()}, which
 * is what lets both be driven from a test.
 *
 * <p>
 * The class keeps its name because it is what {@code java -jar} invokes.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class PAL {
	/** The object file to run when none is named. */
	private static final String DEFAULT_FILENAME = "CODE";

	/** No instances. */
	private PAL() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Main method for command line operation.
	 * 
	 * @param args
	 *            Command line options are limited to a single filename.
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			usage(System.err);
			System.exit(ExitStatus.ABNORMAL.exitCode());
		}

		String filename = args.length == 1 ? args[0] : DEFAULT_FILENAME;

		// Anything that stops us getting as far as a termination instruction
		// is abnormal.
		ExitStatus status = ExitStatus.ABNORMAL;
		try (InputStream is = new FileInputStream(filename)) {
			Program program = Loader.load(is, filename);
			status = new Machine(program, System.in, System.out, System.err)
					.execute();
		} catch (LoadException e) {
			System.err.println(filename + ":" + e.lineno() + ": "
					+ e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open " + filename + ".");
			usage(System.err);
		} catch (IOException e) {
			System.err.println("Error reading " + filename + ": "
					+ e.getMessage());
		}
		System.exit(status.exitCode());
	}

	/**
	 * Simple usage information.
	 *
	 * @param stream
	 *            stream to print the usage message to
	 */
	private static void usage(PrintStream stream) {
		stream.println("usage: java -jar PAL.jar [filename]");
	}
}
