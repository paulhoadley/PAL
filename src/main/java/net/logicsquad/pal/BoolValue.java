package net.logicsquad.pal;

/**
 * A boolean value.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record BoolValue(boolean value) implements Datum {
	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
