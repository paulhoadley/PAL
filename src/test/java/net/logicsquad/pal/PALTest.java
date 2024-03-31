package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * "Functional tests" on {@link PAL}. What we're doing here is just running the existing "test suite" using JUnit. We already have expected
 * output for a set of input files. Due to {@link PAL}'s fondness for calling {@code System.exit()}, we can't run all of these just yet.
 * 
 * @author paulh
 */
public class PALTest {
	private static final List<String> INPUTS = List.of(
	"BASICS",
	"BOOLS",
	"INC",
	"LOCALS",
	"OPR-11",
	"OPR-12",
	"OPR-13",
	"OPR-14",
	"OPR-15",
	"OPR-2",
	"OPR-24",
	"OPR-29-30",
	"OPR-3-4-5-6",
	"OPR-6a",
	"OPR-6b",
	"OPR-7",
	"OPR-8",
	"OPR-8-27-28",
	"SIGa",
	"SIGb",
	"SIGc",
	"SIGd",
	"STRINGPARSE");

	@Test
	public void nonInteractiveTests() throws IOException {
		for (String input : INPUTS) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			System.setOut(new PrintStream(baos));
			System.setErr(new PrintStream(baos));
			InputStream is = PALTest.class.getResourceAsStream("/basic/" + input);
			PAL pal = new PAL(is);
			pal.execute();
			baos.flush();
			String output = new String(baos.toByteArray());
			String expected = new String(PALTest.class.getResourceAsStream("/basic/" + input + ".ref").readAllBytes(), StandardCharsets.UTF_8);
			assertEquals(expected, output);
		}
		return;
	}
}
