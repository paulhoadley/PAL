package net.logicsquad.pal;

/**
 * A real value.
 *
 * <p>
 * A <code>float</code>, not a <code>double</code>: the manual specifies Java
 * <code>float</code> semantics, and the reference files record
 * <code>Float.toString</code>.
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
