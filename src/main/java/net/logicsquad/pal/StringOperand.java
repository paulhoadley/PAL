package net.logicsquad.pal;

/**
 * A string operand, with the delimiting apostrophes already removed.
 *
 * <p>
 * The loader strips them, where the machine used to carry them through to
 * <code>LCS</code> and strip them at run time on every execution.
 *
 * @param value
 *            the value
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record StringOperand(String value) implements Operand {
}
