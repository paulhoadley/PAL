package net.logicsquad.pal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The version this was built as, for {@code --version} to report.
 *
 * <p>
 * The value comes from a resource Maven filters, not from the jar manifest.
 * A manifest is only present when running from the jar, and the command line
 * tests deliberately run from {@code target/classes}, so a manifest
 * would have left the one thing worth testing here untestable.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class Version {
	/** What to report if the version cannot be read. */
	static final String UNKNOWN = "unknown";

	/** Resource Maven filters the version into. */
	private static final String RESOURCE = "version.properties";

	/** Key holding the version in {@link Version#RESOURCE}. */
	private static final String KEY = "version";

	/** The version, read once. */
	private static final String VERSION = read();

	/** No instances. */
	private Version() {
		throw new AssertionError("Not instantiable.");
	}

	/**
	 * Returns the version this was built as, or {@link Version#UNKNOWN} if
	 * that cannot be determined. Being unable to name our own version is not
	 * worth failing a run over, so this never throws.
	 *
	 * @return the version
	 */
	static String version() {
		return VERSION;
	}

	/**
	 * Reads the version from {@link Version#RESOURCE}.
	 *
	 * @return the version, or {@link Version#UNKNOWN}
	 */
	private static String read() {
		try (InputStream is = Version.class.getResourceAsStream(RESOURCE)) {
			if (is == null) {
				return UNKNOWN;
			}

			Properties properties = new Properties();
			properties.load(is);
			String version = properties.getProperty(KEY);

			return version == null || version.isBlank() ? UNKNOWN : version;
		} catch (IOException e) {
			return UNKNOWN;
		}
	}
}
