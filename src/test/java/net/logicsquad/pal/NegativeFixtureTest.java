package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests covering the ways a program can be rejected or fail, using the
 * fixtures in <code>src/test/resources/negative</code>. See {@link Fixtures}
 * for the fixture file conventions.
 *
 * <p>
 * Unlike {@link PALTest}, each fixture runs in its own JVM. Load errors still
 * call <code>System.exit()</code> from inside the loader, which would take the
 * test JVM down with it, and running out of process has the side benefit of
 * checking the real process exit code rather than an
 * {@link PAL.ExitStatus}. Once #28 gives the loader an exception to throw,
 * these could move in process, though the end-to-end exit code check is worth
 * keeping.
 *
 * <p>
 * Each fixture is run with its own directory as the working directory and is
 * named by its bare filename, so the file name appearing in a runtime error
 * message is stable wherever the project is checked out.
 *
 * <p>
 * Some reference files here record behaviour that is <em>known to be poor</em>
 * and is scheduled to improve: <code>UNTERMINATEDSTRING</code> and
 * <code>STACKUNDERFLOW</code> currently surface raw Java exception messages
 * (#28, #29, #30), and <code>BLANKLINESTOLIMIT</code> is rejected only because
 * the code store limit counts lines rather than instructions (#30). They are
 * here so that those fixes show up as a visible change in expected output.
 *
 * @author paulh
 */
public class NegativeFixtureTest {
	/** How long a fixture may run before we call it hung. */
	private static final int TIMEOUT_SECONDS = 60;

	/**
	 * One test per negative fixture.
	 *
	 * @return one test per fixture
	 * @throws IOException
	 *             if the fixture directory cannot be read
	 */
	@TestFactory
	public Stream<DynamicTest> negativeFixtures() throws IOException {
		return Fixtures.in("negative").stream().map(fixture -> DynamicTest
				.dynamicTest("negative/" + fixture.getFileName(), () -> check(fixture)));
	}

	/**
	 * Runs a fixture and compares its output and exit code against the
	 * expected values, or rewrites those expectations if
	 * <code>pal.updateRefs</code> is set.
	 *
	 * @param fixture
	 *            the object file to run
	 * @throws IOException
	 *             if a fixture file cannot be read or written
	 * @throws InterruptedException
	 *             if interrupted waiting for the machine to finish
	 */
	private static void check(Path fixture) throws IOException, InterruptedException {
		Result result = run(fixture);

		if (Fixtures.UPDATE_REFS) {
			Fixtures.record(fixture, result.output(), result.exitCode());
			return;
		}

		assertAll(fixture.getFileName().toString(),
				() -> assertEquals(Fixtures.expectedOutput(fixture), result.output(), "output"),
				() -> assertEquals(Fixtures.expectedStatus(fixture), result.exitCode(),
						"exit code"));
	}

	/**
	 * Runs a fixture in its own JVM.
	 *
	 * @param fixture
	 *            the object file to run
	 * @return what the machine wrote, and the code it exited with
	 * @throws IOException
	 *             if the machine cannot be started
	 * @throws InterruptedException
	 *             if interrupted waiting for it to finish
	 */
	private static Result run(Path fixture) throws IOException, InterruptedException {
		Path classes = Fixtures.BASEDIR.resolve("target/classes");
		assertTrue(Files.isDirectory(classes), "No compiled classes at " + classes);

		ProcessBuilder builder = new ProcessBuilder(
				Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-cp", classes.toString(),
				PAL.class.getName(),
				fixture.getFileName().toString());
		builder.directory(fixture.getParent().toFile());
		// The reference files were captured with stderr redirected onto
		// stdout, so the two interleave.
		builder.redirectErrorStream(true);

		Path in = Fixtures.companion(fixture, ".in");
		boolean hasInput = Files.exists(in);
		if (hasInput) {
			builder.redirectInput(in.toFile());
		}

		Process process = builder.start();
		if (!hasInput) {
			// No input file means the program sees an immediate end of file,
			// rather than inheriting whatever the build was started with.
			process.getOutputStream().close();
		}

		// Drain before waiting, so a chatty program cannot fill the pipe and
		// deadlock against us.
		String output = new String(process.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
		boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
		}
		assertTrue(finished, fixture.getFileName() + " did not finish within "
				+ TIMEOUT_SECONDS + " seconds");

		return new Result(output, process.exitValue());
	}

	/**
	 * What a fixture run produced.
	 *
	 * @param output
	 *            everything the machine wrote, stdout and stderr interleaved
	 * @param exitCode
	 *            the process exit code
	 */
	private record Result(String output, int exitCode) {
	}
}
