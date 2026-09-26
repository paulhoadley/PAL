package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Options}.
 *
 * <p>
 * Nearly everything the command line does is decided here rather than in
 * {@link PAL}, which is the point of the split: these run in process, and
 * {@link CommandLineTest} is left with only the exit codes and the choice of
 * stream to check out of process.
 *
 * @author paulh
 */
public class OptionsTest {
	@Test
	public void anEmptyCommandLineRunsTheDefaultFileWithTheDefaultLimits() {
		Options options = Options.parse(new String[] {});

		assertAll("defaults",
				() -> assertEquals(Options.Mode.RUN, options.mode()),
				() -> assertEquals(Options.DEFAULT_FILENAME, options.filename()),
				() -> assertTrue(options.input().isEmpty(), "reads the console"),
				() -> assertEquals(Loader.CODESIZE, options.codeSize()),
				() -> assertEquals(Machine.DATASIZE, options.dataSize()),
				() -> assertFalse(options.trace()));
	}

	@Test
	public void aBareArgumentIsTheObjectFile() {
		assertEquals("PROG", Options.parse(new String[] { "PROG" }).filename());
	}

	@Test
	public void optionsMayComeBeforeOrAfterTheObjectFile() {
		Options before = Options.parse(new String[] { "--trace", "PROG" });
		Options after = Options.parse(new String[] { "PROG", "--trace" });

		assertEquals(before, after, "position makes no difference");
		assertTrue(after.trace());
		assertEquals("PROG", after.filename());
	}

	@Test
	public void everyOptionIsRead() {
		Options options = Options.parse(new String[] { "--trace",
				"--input=in.txt", "--code-size=2000", "--data-size=750", "PROG" });

		assertAll("all together",
				() -> assertTrue(options.trace()),
				() -> assertEquals("in.txt", options.input().orElseThrow()),
				() -> assertEquals(2000, options.codeSize()),
				() -> assertEquals(750, options.dataSize()),
				() -> assertEquals("PROG", options.filename()));
	}

	@Test
	public void helpIsAnsweredWithHelpWhateverElseIsOnTheLine() {
		assertAll("help wins",
				() -> assertEquals(Options.Mode.HELP,
						Options.parse(new String[] { "--help" }).mode()),
				() -> assertEquals(Options.Mode.HELP,
						Options.parse(new String[] { "-h" }).mode()),
				// Validating first would answer a request for help with a
				// complaint about the thing help would have explained.
				() -> assertEquals(Options.Mode.HELP, Options
						.parse(new String[] { "--code-size=oops", "--help" })
						.mode()),
				() -> assertEquals(Options.Mode.HELP,
						Options.parse(new String[] { "--help", "--version" })
								.mode()));
	}

	@Test
	public void versionIsAskedForByName() {
		assertEquals(Options.Mode.VERSION,
				Options.parse(new String[] { "--version", "PROG" }).mode());
	}

	@Test
	public void anUnknownOptionIsRefused() {
		assertEquals("Unknown option '--nosuch'.",
				assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--nosuch" }))
						.getMessage());
	}

	@Test
	public void aValueMustBeGivenWithAnEqualsSign() {
		assertAll("no value",
				() -> assertEquals("--code-size needs a value, as --code-size=VALUE.",
						assertThrows(UsageException.class, () -> Options
								.parse(new String[] { "--code-size", "2000" }))
								.getMessage()),
				() -> assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--input" })),
				() -> assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--input=" })));
	}

	@Test
	public void anOptionTakingNoValueIsNotGivenOne() {
		assertEquals("--trace takes no value.",
				assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--trace=yes" }))
						.getMessage());
	}

	@Test
	public void aLimitMustBeAPositiveInteger() {
		assertAll("limits",
				() -> assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--code-size=abc" })),
				// Zero and negative mean "no limit" to DataStack, and are
				// refused here rather than quietly passed through.
				() -> assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--data-size=0" })),
				() -> assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "--data-size=-1" })));
	}

	@Test
	public void onlyOneObjectFileCanBeRun() {
		assertEquals("Only one object file can be run at a time.",
				assertThrows(UsageException.class,
						() -> Options.parse(new String[] { "one", "two" }))
						.getMessage());
	}
}
