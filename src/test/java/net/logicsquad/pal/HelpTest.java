package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests that the help text keeps up with the options.
 *
 * <p>
 * {@link PAL} spells the option names out as prose rather than assembling the
 * help from {@link Options}' constants, so that the columns line up in the
 * source where they can be seen. This is the check that pays for that: an
 * option added to {@code Options} and not to the help fails here.
 *
 * @author paulh
 */
public class HelpTest {
	/** Prefix marking a constant in {@link Options} as an option's name. */
	private static final String PREFIX = "OPTION_";

	@Test
	public void everyOptionIsDocumented() throws Exception {
		String help = help();

		for (String option : options()) {
			assertTrue(help.contains(option),
					option + " is not mentioned in the help:\n" + help);
		}
	}

	@Test
	public void theHelpBeginsWithTheUsageMessage() {
		assertTrue(help().startsWith("usage:"), help());
	}

	@Test
	public void theHelpNamesTheDefaultsItWouldOtherwiseHide() {
		String help = help();

		assertTrue(help.contains(Options.DEFAULT_FILENAME),
				"the file it runs when none is named: " + help);
		assertTrue(help.contains(Integer.toString(Loader.CODESIZE)),
				"the instruction limit: " + help);
		assertTrue(help.contains(Integer.toString(Machine.DATASIZE)),
				"the data stack limit: " + help);
	}

	/**
	 * Every option name {@link Options} knows about.
	 *
	 * @return the option names
	 * @throws IllegalAccessException
	 *             if a constant cannot be read
	 */
	private static List<String> options() throws IllegalAccessException {
		List<String> found = new ArrayList<>();
		for (Field field : Options.class.getDeclaredFields()) {
			if (field.getName().startsWith(PREFIX)) {
				found.add((String) field.get(null));
			}
		}
		assertTrue(!found.isEmpty(), "found no options to check");

		return found;
	}

	/**
	 * The help text.
	 *
	 * @return what {@code PAL.help()} writes
	 */
	private static String help() {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PAL.help(new PrintStream(buffer, true, StandardCharsets.UTF_8));

		return buffer.toString(StandardCharsets.UTF_8);
	}
}
