package net.logicsquad.pal;

/**
 * A tagged value on the PAL data stack.
 *
 * <p>
 * The set of tags is closed and known at compile time, so this is a sealed
 * hierarchy and a {@code switch} over it can be exhaustive. That is the
 * point of it: the machine used to carry an {@code int} tag beside an
 * {@code Object} value and re-derive the relationship between the two at
 * every use, with a cast to match.
 *
 * <p>
 * Values are immutable. The stack cells holding them are not: several
 * instructions overwrite a location in place, which is why {@link DataStack}
 * offers {@link DataStack#set(int, Datum)}. Since a value cannot change under
 * anyone, storing one needs no defensive copy, and the hand-written
 * {@code clone()} this replaces is gone.
 *
 * <p>
 * Each implementation renders itself exactly as the machine has always
 * rendered that tag, because program output and the stack dump both go through
 * {@code toString()} and the reference files record the result.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
sealed interface Datum
		permits IntValue, RealValue, StringValue, BoolValue, Undef {
}
