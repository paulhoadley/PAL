package net.logicsquad.pal;

/**
 * An integer value.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record IntValue(int value) implements Datum {
	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
