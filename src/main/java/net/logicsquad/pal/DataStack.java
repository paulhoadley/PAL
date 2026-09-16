package net.logicsquad.pal;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract data type representing the PAL data stack.
 * 
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
final class DataStack {
	/** A container for the values on the stack. */
	private List<Datum> data;

	/** The address of the current frame's base. */
	private int frameBase;

	/** Reference to the next free space on top of the stack (TOS). */
	private int top;

	/** Maximum stack size. A value &lt;= 0 indicates no limit. */
	private int maxSize;

	/**
	 * Default constructor. Assumes no limit on stack size.
	 */
	DataStack() {
		this(0);
	}

	/**
	 * Constructor. Allows user to specify a limit on maximum size.
	 * 
	 * @param max
	 *            Maximum stack size.
	 */
	DataStack(int max) {
		top = 0;

		data = new ArrayList<Datum>();

		// Set up mark stack part for main program activation record.
		markStack(0, 0);

		// Sets the first frame's base address.
		setBase(top);

		maxSize = (max > 0 ? max : 0);
	}

	/**
	 * Put a data object onto the top of the stack.
	 * 
	 * @param datum
	 *            the value to push onto the stack.
	 * @throws MachineFault
	 *                if there is insufficient free stack space.
	 */
	void push(Datum datum) {
		if (maxSize != 0 && top + 1 > maxSize) {
			throw MachineFault.stackOverflow(maxSize);
		}

		top++;

		data.add(datum);
	}

	/**
	 * Pop the top value from the stack.
	 * 
	 * @return The value removed from the top of the stack.
	 * @throws MachineFault
	 *                if the current frame holds no value to pop.
	 */
	Datum pop() {
		if (top <= frameBase) {
			throw MachineFault.stackUnderflow();
		}

		return data.remove(--top);
	}

	/**
	 * Peek at the top of the stack.
	 * 
	 * @return The value remaining on the top of the stack.
	 * @throws MachineFault
	 *                if the current frame holds no value to peek at.
	 */
	Datum peek() {
		if (top <= frameBase) {
			throw MachineFault.stackUnderflow();
		}

		return data.get(top - 1);
	}

	/**
	 * Discard everything above a given address, stack mark included.
	 *
	 * <p>
	 * This is how the machine dismantles a frame when returning from a call or
	 * searching for an exception handler, and it is deliberately not
	 * {@link DataStack#pop()}: those callers must go below the current frame
	 * base, which is exactly what {@code pop()} refuses to do. Keeping the
	 * two apart is what lets a pop too many be caught as the program error it
	 * is, rather than quietly eating the frame's mark.
	 *
	 * @param address
	 *            the address to unwind to
	 */
	void unwind(int address) {
		while (top > address) {
			data.remove(--top);
		}
	}

	/**
	 * Read a data location elsewhere in the stack. The location is given as an
	 * absolute stack address.
	 * 
	 * @param address
	 *            The absolute address for the target location.
	 * @return The value at the target location.
	 * @throws MachineFault
	 *                if the supplied address is out of bounds.
	 */
	Datum get(int address) {
		if (address < 0 || address >= top) {
			throw MachineFault.badAddress(address);
		}

		return data.get(address);
	}

	/**
	 * Read a data location elsewhere in the stack. The location is given as a
	 * level difference and offset.
	 * 
	 * @param levelDiff
	 *            The difference in static scope level between the current
	 *            activation record, and the activation record of the target
	 *            location.
	 * @param offset
	 *            The offset into the target stack frame.
	 * @return The value at the target address.
	 * @throws MachineFault
	 *                if the supplied address is out of bounds.
	 */
	Datum get(int levelDiff, int offset) {
		int address = getAddress(levelDiff, offset);

		return get(address);
	}

	/**
	 * Overwrite a data location elsewhere in the stack. The location is given
	 * as an absolute stack address.
	 *
	 * <p>
	 * Values are immutable, so an instruction that writes to a location it did
	 * not push, and there are several, replaces the value in the cell rather
	 * than reaching into it.
	 *
	 * @param address
	 *            The absolute address for the target location.
	 * @param datum
	 *            The value to store there.
	 * @throws MachineFault
	 *                if the supplied address is out of bounds.
	 */
	void set(int address, Datum datum) {
		if (address < 0 || address >= top) {
			throw MachineFault.badAddress(address);
		}

		data.set(address, datum);
	}

	/**
	 * Advance the TOS pointer by a given amount, initialising memory to type
	 * UNDEF as we go.
	 * 
	 * @param amount
	 *            The number of location to advance the TOS pointer.
	 * @throws MachineFault
	 *                if an attempt is made to advance the TOS pointer beyond
	 *                the limit of the stack memory.
	 */
	void incTop(int amount) {
		if ((maxSize != 0) && (amount + top > maxSize)) {
			throw MachineFault.stackOverflow(maxSize);
		}

		for (int i = 0; i < amount; i++) {
			data.add(Undef.INSTANCE);
			top++;
		}
	}

	/**
	 * Mark the stack.
	 * 
	 * @param staticLink
	 *            A pointer to the activation record one level below the current
	 *            level in terms of <em>lexical scope</em>.
	 * @param dynamicLink
	 *            A pointer to the activation record one level below the current
	 *            level in terms of <em>dynamic scope</em>.
	 * @throws MachineFault
	 *                if the TOS pointer is advanced beyond the limit of stack
	 *                memory.
	 */
	void markStack(int staticLink, int dynamicLink) {
		push(new IntValue(staticLink));
		push(new IntValue(dynamicLink));

		// Leave space for return point.
		push(new IntValue(0));

		// Dummy exception handler address - indicates that no handler
		// is registered.
		push(new IntValue(0));
	}

	/**
	 * Set the current frame's base address.
	 * 
	 * @param address
	 *            The address to store as the current frame base.
	 */
	void setBase(int address) {
		frameBase = address;
	}

	/**
	 * Get the absolute address for a stack location, given the level difference
	 * and offset.
	 * 
	 * @param levelDiff
	 *            The difference in static scope level between the current
	 *            activation record, and the activation record of the target
	 *            location.
	 * @param offset
	 *            The offset into the target stack frame.
	 * @return The absolute address for the target location.
	 * @throws MachineFault
	 *                if the supplied level difference is invalid.
	 */
	int getAddress(int levelDiff, int offset) {
		int result = frameBase;

		for (int i = 0; i < levelDiff; i++) {
			// Extract the static link from the stack mark.
			if (!(get(result - 4) instanceof IntValue link)) {
				// The machine writes its own stack marks, so a link that is
				// not an integer is a bug here rather than in the program.
				throw new IllegalStateException(
						"Static link at " + (result - 4) + " is not an integer.");
			}
			result = link.value();
		}

		result += offset;

		return result;
	}

	/**
	 * Get the address of the stack top.
	 * 
	 * @return The absolute address of the top element.
	 */
	int getTop() {
		return top;
	}

	/**
	 * Returns a {@code String} representation of the object. Effectively,
	 * this is a dump of the stack from the uppermost element to the lowermost.
	 * 
	 * @return A {@code String} representation of the object.
	 */
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (int i = top - 1; i >= 0; i--) {
			result.append(get(i)).append('\n');
		}

		return result.toString();
	}
}
