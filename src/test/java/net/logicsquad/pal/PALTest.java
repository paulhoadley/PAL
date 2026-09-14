package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Functional tests on {@link PAL}, running each fixture in process against
 * injected streams. See {@link Fixtures} for the fixture file conventions.
 *
 * <p>
 * Fixtures are discovered from the filesystem, so adding one is a matter of
 * dropping files into place. Each becomes its own test, meaning a failure in
 * one no longer hides the fixtures after it.
 *
 * <p>
 * Programs that fail during loading rather than execution cannot be run this
 * way, because the loader still calls <code>System.exit()</code> and would
 * take the test JVM with it. Those live in
 * <code>src/test/resources/negative</code> and are driven by
 * {@link NegativeFixtureTest} instead.
 *
 * @author paulh
 */
public class PALTest {
	/**
	 * Fixtures that need no console input.
	 *
	 * @return one test per fixture
	 * @throws IOException
	 *             if the fixture directory cannot be read
	 */
	@TestFactory
	public Stream<DynamicTest> basicFixtures() throws IOException {
		return tests("basic");
	}

	/**
	 * Fixtures driven by console input from a <code>.in</code> file.
	 *
	 * @return one test per fixture
	 * @throws IOException
	 *             if the fixture directory cannot be read
	 */
	@TestFactory
	public Stream<DynamicTest> interactiveFixtures() throws IOException {
		return tests("interactive");
	}

	/**
	 * Builds one {@link DynamicTest} per fixture in a directory.
	 *
	 * @param directory
	 *            fixture directory, relative to the resource root
	 * @return one test per fixture
	 * @throws IOException
	 *             if the directory cannot be read
	 */
	private static Stream<DynamicTest> tests(String directory) throws IOException {
		return Fixtures.in(directory).stream().map(fixture -> DynamicTest
				.dynamicTest(directory + "/" + fixture.getFileName(), () -> check(fixture)));
	}

	/**
	 * Runs a fixture and compares its output and exit status against the
	 * expected values, or rewrites those expectations if
	 * <code>pal.updateRefs</code> is set.
	 *
	 * @param fixture
	 *            the object file to run
	 * @throws IOException
	 *             if a fixture file cannot be read or written
	 */
	private static void check(Path fixture) throws IOException {
		// Program output and diagnostics share one buffer: the reference
		// files were captured with stderr redirected onto stdout, so the two
		// interleave.
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(buffer, true, StandardCharsets.UTF_8);

		PAL machine = new PAL(Files.newInputStream(fixture), input(fixture), stream, stream);
		PAL.ExitStatus status = machine.execute();
		stream.flush();

		String actual = buffer.toString(StandardCharsets.UTF_8);

		if (Fixtures.UPDATE_REFS) {
			Fixtures.record(fixture, actual, status.exitCode());
			return;
		}

		assertAll(fixture.getFileName().toString(),
				() -> assertEquals(Fixtures.expectedOutput(fixture), actual, "output"),
				() -> assertEquals(Fixtures.expectedStatus(fixture), status.exitCode(),
						"exit status"));
	}

	/**
	 * Console input for a fixture, taken from its <code>.in</code> file, or
	 * empty if it has none.
	 *
	 * @param fixture
	 *            the fixture
	 * @return console input for the run
	 * @throws IOException
	 *             if the input file cannot be read
	 */
	private static InputStream input(Path fixture) throws IOException {
		Path in = Fixtures.companion(fixture, ".in");
		return Files.exists(in) ? Files.newInputStream(in)
				: new ByteArrayInputStream(new byte[0]);
	}
}
