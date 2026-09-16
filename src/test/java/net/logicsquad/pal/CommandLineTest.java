package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Tests on the ways the command line itself can be wrong, as opposed to the
 * program it names.
 *
 * <p>
 * These run out of process for the same reason as {@link NegativeFixtureTest}:
 * the interesting part is what the user sees and what the shell gets back, and
 * <code>main()</code> ends in <code>System.exit()</code>. Each runs in an empty
 * directory, so that a <code>CODE</code> file somewhere up the tree cannot make
 * the no-arguments case pass by accident.
 *
 * @author paulh
 */
public class CommandLineTest {
	/** Long enough that a machine doing nothing has certainly finished. */
	private static final int TIMEOUT_SECONDS = 60;

	@Test
	public void aMissingFileIsReportedWithUsage() throws Exception {
		Result result = run("nosuchfile");

		assertAll("missing file",
				() -> assertEquals(1, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().startsWith("Cannot open nosuchfile."),
						"names what it could not open: " + result.output()),
				() -> assertTrue(result.output().contains("usage:"),
						"and says how to invoke it: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"nothing on stdout: " + result.stdout()));
	}

	@Test
	public void noArgumentsLooksForCODEAndSaysSoWhenItIsMissing() throws Exception {
		Result result = run();

		assertAll("no arguments",
				() -> assertEquals(1, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().startsWith("Cannot open CODE."),
						"names the default file: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"nothing on stdout: " + result.stdout()));
	}

	@Test
	public void tooManyArgumentsPrintsUsageOnStderr() throws Exception {
		Result result = run("one", "two");

		assertAll("too many arguments",
				() -> assertEquals(1, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().contains("usage:"),
						"says how to invoke it: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"usage is a diagnostic, so nothing on stdout: "
								+ result.stdout()));
	}

	/**
	 * Runs the machine in its own JVM, in a directory of its own.
	 *
	 * @param args
	 *            command line arguments
	 * @return what it wrote, and the code it exited with
	 * @throws IOException
	 *             if the machine cannot be started
	 * @throws InterruptedException
	 *             if interrupted waiting for it to finish
	 */
	private static Result run(String... args) throws IOException, InterruptedException {
		Path classes = Fixtures.BASEDIR.resolve("target/classes");
		assertTrue(Files.isDirectory(classes), "No compiled classes at " + classes);

		Path empty = Files.createTempDirectory("pal-cli");
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command().add(Path.of(System.getProperty("java.home"), "bin", "java")
					.toString());
			builder.command().add("-cp");
			builder.command().add(classes.toString());
			builder.command().add(PAL.class.getName());
			builder.command().addAll(java.util.List.of(args));
			builder.directory(empty.toFile());

			Process process = builder.start();
			process.getOutputStream().close();

			// Drain both before waiting, so neither pipe can fill and deadlock
			// against us.
			String out = new String(process.getInputStream().readAllBytes(),
					StandardCharsets.UTF_8);
			String err = new String(process.getErrorStream().readAllBytes(),
					StandardCharsets.UTF_8);
			boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
			}
			assertTrue(finished, "machine did not finish within " + TIMEOUT_SECONDS
					+ " seconds");

			return new Result(out, err, process.exitValue());
		} finally {
			Files.deleteIfExists(empty);
		}
	}

	/**
	 * What a run produced.
	 *
	 * @param stdout
	 *            what the machine wrote to standard output
	 * @param output
	 *            what the machine wrote to standard error, which is where a
	 *            diagnostic belongs
	 * @param exitCode
	 *            the process exit code
	 */
	private record Result(String stdout, String output, int exitCode) {
	}
}
