package net.logicsquad.pal;

/**
 * A real value.
 *
 * <p>
 * A {@code float}, not a {@code double}: the manual specifies Java
 * {@code float} semantics, and the reference files record
 * {@code Float.toString}.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record RealValue(float value) implements Datum {
	@Override
	public String toString() {
		return Float.toString(value);
	}
}
