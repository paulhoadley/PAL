package net.logicsquad.pal;

/**
 * An undefined value, which is what {@code INC} leaves in the space it makes.
 *
 * <p>
 * Carries nothing, so one instance serves for all of them.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record Undef() implements Datum {
	/** The only instance anyone needs. */
	static final Undef INSTANCE = new Undef();

	@Override
	public String toString() {
		return "UNDEF";
	}
}
