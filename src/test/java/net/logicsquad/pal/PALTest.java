package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Functional tests on {@link PAL}. Each fixture in
 * <code>src/test/resources</code> is an object file with a
 * <code>.ref</code> file holding its expected output, optionally a
 * <code>.in</code> file supplying console input, and optionally a
 * <code>.status</code> file naming the expected process exit code. A fixture
 * with no <code>.status</code> file is expected to exit zero.
 *
 * <p>
 * Fixtures are discovered from the filesystem, so adding one is a matter of
 * dropping files into place. Each becomes its own test, meaning a failure in
 * one no longer hides the fixtures after it.
 *
 * <p>
 * Running with <code>-Dpal.updateRefs=true</code> rewrites every
 * <code>.ref</code> and <code>.status</code> file from current behaviour
 * instead of asserting against them. That replaces the old
 * <code>make test-ref</code> target. Check the resulting diff carefully: it
 * records whatever the machine does today, correct or not.
 *
 * @author paulh
 */
public class PALTest {
	/** Root of the fixture tree. */
	private static final Path RESOURCES = Path
			.of(System.getProperty("basedir", System.getProperty("user.dir")))
			.resolve("src/test/resources");

	/** Rewrite reference files rather than assert against them? */
	private static final boolean UPDATE_REFS = Boolean.getBoolean("pal.updateRefs");

	/** Suffixes marking a file as a fixture's companion, not a fixture. */
	private static final List<String> COMPANIONS = List.of(".ref", ".in", ".status");

	/**
	 * Fixtures that need no console input.
	 *
	 * @return one test per fixture
	 * @throws IOException
	 *             if the fixture directory cannot be read
	 */
	@TestFactory
	public Stream<DynamicTest> basicFixtures() throws IOException {
		return fixtures("basic");
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
		return fixtures("interactive");
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
	private static Stream<DynamicTest> fixtures(String directory) throws IOException {
		Path path = RESOURCES.resolve(directory);
		assertTrue(Files.isDirectory(path), "No fixture directory at " + path);

		List<Path> found;
		try (Stream<Path> entries = Files.list(path)) {
			found = entries.filter(PALTest::isFixture).sorted().toList();
		}

		// Guard against a wrong path quietly producing no tests at all.
		assertTrue(!found.isEmpty(), "No fixtures found in " + path);

		return found.stream().map(fixture -> DynamicTest
				.dynamicTest(directory + "/" + fixture.getFileName(), () -> check(fixture)));
	}

	/**
	 * Is this a fixture, as opposed to one of a fixture's companion files?
	 *
	 * @param path
	 *            candidate
	 * @return <code>true</code> if this is a fixture
	 */
	private static boolean isFixture(Path path) {
		String name = path.getFileName().toString();
		return Files.isRegularFile(path) && COMPANIONS.stream().noneMatch(name::endsWith);
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

		if (UPDATE_REFS) {
			Files.writeString(companion(fixture, ".ref"), actual, StandardCharsets.UTF_8);
			updateStatus(fixture, status);
			return;
		}

		assertAll(fixture.getFileName().toString(),
				() -> assertEquals(Files.readString(companion(fixture, ".ref"),
						StandardCharsets.UTF_8), actual, "output"),
				() -> assertEquals(expectedStatus(fixture), status.exitCode(), "exit status"));
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
		Path in = companion(fixture, ".in");
		return Files.exists(in) ? Files.newInputStream(in)
				: new ByteArrayInputStream(new byte[0]);
	}

	/**
	 * The exit code a fixture is expected to produce. A fixture with no
	 * <code>.status</code> file is expected to exit zero.
	 *
	 * @param fixture
	 *            the fixture
	 * @return expected process exit code
	 * @throws IOException
	 *             if the status file cannot be read
	 */
	private static int expectedStatus(Path fixture) throws IOException {
		Path status = companion(fixture, ".status");
		return Files.exists(status)
				? Integer.parseInt(Files.readString(status, StandardCharsets.UTF_8).trim())
				: 0;
	}

	/**
	 * Records a fixture's exit status, removing the file entirely when the
	 * status is the zero default.
	 *
	 * @param fixture
	 *            the fixture
	 * @param status
	 *            the status it produced
	 * @throws IOException
	 *             if the status file cannot be written or removed
	 */
	private static void updateStatus(Path fixture, PAL.ExitStatus status) throws IOException {
		Path path = companion(fixture, ".status");
		if (status.exitCode() == 0) {
			Files.deleteIfExists(path);
		} else {
			Files.writeString(path, status.exitCode() + "\n", StandardCharsets.UTF_8);
		}
	}

	/**
	 * A fixture's companion file.
	 *
	 * @param fixture
	 *            the fixture
	 * @param suffix
	 *            companion suffix, including the dot
	 * @return path to the companion file, which need not exist
	 */
	private static Path companion(Path fixture, String suffix) {
		return fixture.resolveSibling(fixture.getFileName() + suffix);
	}
}
