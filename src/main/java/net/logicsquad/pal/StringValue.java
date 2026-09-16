package net.logicsquad.pal;

/**
 * A string value, without the delimiting apostrophes the loader saw.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record StringValue(String value) implements Datum {
	@Override
	public String toString() {
		return value;
	}
}
