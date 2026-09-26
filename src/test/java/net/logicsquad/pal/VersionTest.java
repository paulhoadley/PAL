package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Version}.
 *
 * @author paulh
 */
public class VersionTest {
	@Test
	public void theVersionIsKnownWhenRunningFromTheBuildOutput() {
		// The reason for a filtered resource rather than the jar manifest:
		// this test does not run from a jar, and neither do most of the others.
		assertNotEquals(Version.UNKNOWN, Version.version());
	}

	@Test
	public void theVersionWasActuallyFilteredRatherThanCopied() {
		assertFalse(Version.version().contains("${"),
				"unfiltered placeholder: " + Version.version());
	}
}
