package net.logicsquad.pal;

/**
 * A real operand.
 *
 * <p>
 * An integer literal is accepted and promoted, since {@code LCR 0 1} has
 * always been a way of writing {@code 1.0}.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record RealOperand(float value) implements Operand {
}
