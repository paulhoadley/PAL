package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests on the command line itself, as opposed to the program it names.
 *
 * <p>
 * These run out of process for the same reason as {@link NegativeFixtureTest}:
 * the interesting part is what the user sees and what the shell gets back, and
 * <code>main()</code> ends in <code>System.exit()</code>. The exit codes are
 * written here as the bare numbers a shell would see rather than as
 * {@link ExitStatus} constants, because the numbers are the contract.
 *
 * <p>
 * Each runs in a directory of its own, so that a <code>CODE</code> file
 * somewhere up the tree cannot make the no-arguments case pass by accident.
 *
 * @author paulh
 */
public class CommandLineTest {
	/** Long enough that a machine doing nothing has certainly finished. */
	private static final int TIMEOUT_SECONDS = 60;

	/** A program that prints a string and stops, for tracing. */
	private static final String HELLO = """
			LCS 0 'Hello, world.'
			OPR 0 20
			OPR 0 21
			JMP 0 0
			""";

	@Test
	public void aMissingFileIsReportedWithUsage() throws Exception {
		Result result = run("nosuchfile");

		assertAll("missing file",
				() -> assertEquals(2, result.exitCode(), "exit code"),
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
				() -> assertEquals(2, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().startsWith("Cannot open CODE."),
						"names the default file: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"nothing on stdout: " + result.stdout()));
	}

	@Test
	public void tooManyArgumentsPrintsUsageOnStderr() throws Exception {
		Result result = run("one", "two");

		assertAll("too many arguments",
				() -> assertEquals(2, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().contains("usage:"),
						"says how to invoke it: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"usage is a diagnostic, so nothing on stdout: "
								+ result.stdout()));
	}

	@Test
	public void anUnknownOptionIsRejected() throws Exception {
		Result result = run("--nosuchoption");

		assertAll("unknown option",
				() -> assertEquals(2, result.exitCode(), "exit code"),
				() -> assertTrue(
						result.output().contains("Unknown option '--nosuchoption'."),
						"names the option: " + result.output()),
				() -> assertTrue(result.stdout().isEmpty(),
						"nothing on stdout: " + result.stdout()));
	}

	@Test
	public void aLimitOfZeroIsRejected() throws Exception {
		Result result = run("--data-size=0");

		assertAll("zero limit",
				() -> assertEquals(2, result.exitCode(), "exit code"),
				() -> assertTrue(result.output().contains("positive integer"),
						"says what it wanted: " + result.output()));
	}

	@Test
	public void helpIsAskedForSoItGoesToStandardOutput() throws Exception {
		Result result = run("--help");

		assertAll("help",
				() -> assertEquals(0, result.exitCode(), "exit code"),
				() -> assertTrue(result.stdout().contains("usage:"),
						"on stdout: " + result.stdout()),
				() -> assertTrue(result.output().isEmpty(),
						"and nothing on stderr: " + result.output()));
	}

	@Test
	public void versionReportsTheVersionItWasBuiltAs() throws Exception {
		Result result = run("--version");

		assertAll("version",
				() -> assertEquals(0, result.exitCode(), "exit code"),
				() -> assertTrue(result.stdout().startsWith("PAL "),
						"names itself: " + result.stdout()),
				// The point of the filtered resource: a manifest would leave
				// this unknown when running from target/classes, as here.
				() -> assertTrue(!result.stdout().contains(Version.UNKNOWN),
						"and knows its version: " + result.stdout()),
				() -> assertTrue(result.output().isEmpty(),
						"nothing on stderr: " + result.output()));
	}

	@Test
	public void inputComesFromTheNamedFile(@TempDir Path directory)
			throws Exception {
		Files.writeString(directory.resolve("PROG"), """
				INC 0 1
				RDI 0 0
				LDV 0 0
				OPR 0 20
				OPR 0 21
				JMP 0 0
				""", StandardCharsets.UTF_8);
		Files.writeString(directory.resolve("numbers"), "42\n",
				StandardCharsets.UTF_8);

		Result result = runIn(directory, "--input=numbers", "PROG");

		assertAll("input from a file",
				() -> assertEquals(0, result.exitCode(), "exit code"),
				() -> assertEquals("42\n", result.stdout(), "read the file"),
				() -> assertTrue(result.output().isEmpty(),
						"nothing on stderr: " + result.output()));
	}

	@Test
	public void aTraceGoesToStandardErrorAndLeavesOutputAlone(
			@TempDir Path directory) throws Exception {
		Files.writeString(directory.resolve("CODE"), HELLO,
				StandardCharsets.UTF_8);

		Result result = runIn(directory, "--trace");

		List<String> trace = result.output().lines().toList();

		assertAll("trace",
				() -> assertEquals(0, result.exitCode(), "exit code"),
				() -> assertEquals("Hello, world.\n", result.stdout(),
						"the program's output is untouched"),
				() -> assertEquals(4, trace.size(),
						"one line per instruction: " + result.output()),
				// Address and line number agree here, there being no blank
				// lines; OptionsTest and TracerTest cover them diverging.
				() -> assertEquals("trace: 1:1 tos=- LCS 0 'Hello, world.'",
						trace.get(0), "first instruction"),
				() -> assertEquals("trace: 2:2 tos=Hello, world. OPR 0 20",
						trace.get(1), "and what it left on the stack"));
	}

	@Test
	public void noTraceIsAskedForAndNoneIsGiven(@TempDir Path directory)
			throws Exception {
		Files.writeString(directory.resolve("CODE"), HELLO,
				StandardCharsets.UTF_8);

		Result result = runIn(directory);

		assertAll("untraced",
				() -> assertEquals(0, result.exitCode(), "exit code"),
				() -> assertEquals("Hello, world.\n", result.stdout(), "output"),
				() -> assertTrue(result.output().isEmpty(),
						"nothing on stderr: " + result.output()));
	}

	/**
	 * Runs the machine in an empty directory of its own.
	 *
	 * @param args
	 *            command line arguments
	 * @return what it wrote, and the code it exited with
	 * @throws IOException
	 *             if the machine cannot be started
	 * @throws InterruptedException
	 *             if interrupted waiting for it to finish
	 */
	private static Result run(String... args)
			throws IOException, InterruptedException {
		Path empty = Files.createTempDirectory("pal-cli");
		try {
			return runIn(empty, args);
		} finally {
			delete(empty);
		}
	}

	/**
	 * Runs the machine in its own JVM, in a given directory.
	 *
	 * @param directory
	 *            working directory for the run
	 * @param args
	 *            command line arguments
	 * @return what it wrote, and the code it exited with
	 * @throws IOException
	 *             if the machine cannot be started
	 * @throws InterruptedException
	 *             if interrupted waiting for it to finish
	 */
	private static Result runIn(Path directory, String... args)
			throws IOException, InterruptedException {
		Path classes = Fixtures.BASEDIR.resolve("target/classes");
		assertTrue(Files.isDirectory(classes), "No compiled classes at " + classes);

		ProcessBuilder builder = new ProcessBuilder();
		builder.command().add(Path.of(System.getProperty("java.home"), "bin", "java")
				.toString());
		builder.command().add("-cp");
		builder.command().add(classes.toString());
		builder.command().add(PAL.class.getName());
		builder.command().addAll(List.of(args));
		builder.directory(directory.toFile());

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
		assertTrue(finished,
				"machine did not finish within " + TIMEOUT_SECONDS + " seconds");

		return new Result(out, err, process.exitValue());
	}

	/**
	 * Removes a directory and anything in it.
	 *
	 * @param directory
	 *            directory to remove
	 * @throws IOException
	 *             if it cannot be read
	 */
	private static void delete(Path directory) throws IOException {
		try (Stream<Path> entries = Files.walk(directory)) {
			for (Path each : entries.sorted(Comparator.reverseOrder())
					.toList()) {
				Files.deleteIfExists(each);
			}
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
