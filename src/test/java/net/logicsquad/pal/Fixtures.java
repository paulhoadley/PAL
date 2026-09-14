package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Conventions shared by the file-based fixture suites.
 *
 * <p>
 * A fixture is an object file. Beside it may sit a <code>.ref</code> file
 * holding its expected output, a <code>.in</code> file supplying console
 * input, and a <code>.status</code> file naming the expected process exit
 * code. A fixture with no <code>.status</code> file is expected to exit zero,
 * which is why only the deliberate error cases have one.
 *
 * @author paulh
 */
final class Fixtures {
	/** Project base directory. */
	static final Path BASEDIR = Path
			.of(System.getProperty("basedir", System.getProperty("user.dir")));

	/** Root of the fixture tree. */
	static final Path ROOT = BASEDIR.resolve("src/test/resources");

	/** Rewrite reference files rather than assert against them? */
	static final boolean UPDATE_REFS = Boolean.getBoolean("pal.updateRefs");

	/** Suffixes marking a file as a fixture's companion, not a fixture. */
	private static final List<String> COMPANIONS = List.of(".ref", ".in", ".status");

	private Fixtures() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Every fixture in a directory, in name order.
	 *
	 * @param directory
	 *            fixture directory, relative to the resource root
	 * @return the fixtures found there, never empty
	 * @throws IOException
	 *             if the directory cannot be read
	 */
	static List<Path> in(String directory) throws IOException {
		Path path = ROOT.resolve(directory);
		assertTrue(Files.isDirectory(path), "No fixture directory at " + path);

		List<Path> found;
		try (Stream<Path> entries = Files.list(path)) {
			found = entries.filter(Fixtures::isFixture).sorted().toList();
		}

		// Guard against a wrong path quietly producing no tests at all.
		assertTrue(!found.isEmpty(), "No fixtures found in " + path);
		return found;
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
	 * A fixture's companion file.
	 *
	 * @param fixture
	 *            the fixture
	 * @param suffix
	 *            companion suffix, including the dot
	 * @return path to the companion file, which need not exist
	 */
	static Path companion(Path fixture, String suffix) {
		return fixture.resolveSibling(fixture.getFileName() + suffix);
	}

	/**
	 * The output a fixture is expected to produce.
	 *
	 * @param fixture
	 *            the fixture
	 * @return expected output
	 * @throws IOException
	 *             if the reference file cannot be read
	 */
	static String expectedOutput(Path fixture) throws IOException {
		return Files.readString(companion(fixture, ".ref"), StandardCharsets.UTF_8);
	}

	/**
	 * The exit code a fixture is expected to produce.
	 *
	 * @param fixture
	 *            the fixture
	 * @return expected process exit code
	 * @throws IOException
	 *             if the status file cannot be read
	 */
	static int expectedStatus(Path fixture) throws IOException {
		Path status = companion(fixture, ".status");
		return Files.exists(status)
				? Integer.parseInt(Files.readString(status, StandardCharsets.UTF_8).trim())
				: 0;
	}

	/**
	 * Records what a fixture actually did, for
	 * <code>-Dpal.updateRefs=true</code>. The status file is removed entirely
	 * when the status is the zero default.
	 *
	 * @param fixture
	 *            the fixture
	 * @param output
	 *            the output it produced
	 * @param exitCode
	 *            the exit code it produced
	 * @throws IOException
	 *             if a file cannot be written or removed
	 */
	static void record(Path fixture, String output, int exitCode) throws IOException {
		Files.writeString(companion(fixture, ".ref"), output, StandardCharsets.UTF_8);
		Path status = companion(fixture, ".status");
		if (exitCode == 0) {
			Files.deleteIfExists(status);
		} else {
			Files.writeString(status, exitCode + "\n", StandardCharsets.UTF_8);
		}
	}
}
