package net.logicsquad.pal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link DataStack}.
 *
 * @author paulh
 */
public class DataStackTest {
	/** Slots in a stack mark: static link, dynamic link, return point, handler. */
	private static final int MARK_SIZE = 4;

	@Test
	public void newStackHasAMarkForTheMainProgram() {
		DataStack stack = new DataStack();
		assertEquals(MARK_SIZE, stack.getTop(), "mark occupies four slots");
		for (int i = 0; i < MARK_SIZE; i++) {
			assertEquals(Data.INT, stack.get(i).getType(), "mark slot " + i + " is an integer");
			assertEquals(0, ((Integer) stack.get(i).getValue()).intValue(),
					"mark slot " + i + " starts at zero");
		}
	}

	@Test
	public void baseSitsJustAboveTheMark() {
		DataStack stack = new DataStack();
		// Displacement zero in the current frame is the first slot above the
		// mark, so it is the mark's size.
		assertEquals(MARK_SIZE, stack.getAddress(0, 0));
	}

	@Test
	public void pushAndPopAreSymmetric() {
		DataStack stack = new DataStack();
		int before = stack.getTop();
		Data datum = new Data(Data.INT, Integer.valueOf(7));
		stack.push(datum);
		assertEquals(before + 1, stack.getTop());
		assertSame(datum, stack.peek(), "peek leaves the datum in place");
		assertEquals(before + 1, stack.getTop(), "peek does not pop");
		assertSame(datum, stack.pop());
		assertEquals(before, stack.getTop());
	}

	@Test
	public void incTopFillsWithUndefined() {
		DataStack stack = new DataStack();
		int before = stack.getTop();
		stack.incTop(3);
		assertEquals(before + 3, stack.getTop());
		for (int i = before; i < stack.getTop(); i++) {
			assertEquals(Data.UNDEF, stack.get(i).getType());
			assertEquals("UNDEF", stack.get(i).toString());
		}
	}

	@Test
	public void addressOutOfBoundsIsRefused() {
		DataStack stack = new DataStack();
		assertThrows(IndexOutOfBoundsException.class, () -> stack.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> stack.get(stack.getTop()),
				"the slot above the top is not readable");
	}

	@Test
	public void getAddressFollowsStaticLinks() {
		DataStack stack = new DataStack();
		int outerBase = stack.getAddress(0, 0);

		// Build a frame whose static link points back at the outer frame.
		stack.markStack(outerBase, outerBase);
		stack.setBase(stack.getTop());

		assertEquals(stack.getTop(), stack.getAddress(0, 0), "level 0 is this frame");
		assertEquals(outerBase, stack.getAddress(1, 0), "level 1 follows the static link");
		assertEquals(outerBase + 2, stack.getAddress(1, 2), "displacement applies after the hop");
	}

	@Test
	public void unlimitedStackGrowsPastAnyReasonableSize() {
		DataStack stack = new DataStack();
		for (int i = 0; i < 5000; i++) {
			stack.push(new Data(Data.INT, Integer.valueOf(i)));
		}
		assertEquals(5000 + MARK_SIZE, stack.getTop());
	}

	@Test
	public void incTopRefusesToCrossTheLimit() {
		DataStack stack = new DataStack(10);
		assertThrows(OutOfMemoryError.class, () -> stack.incTop(100));
		assertEquals(MARK_SIZE, stack.getTop(), "a refused incTop leaves the stack alone");
	}

	/**
	 * Documents the inconsistency between the two ways of growing the stack.
	 * {@code incTop} checks before it grows and so reaches the configured
	 * size, while {@code push} appends first and only then complains, so it
	 * stops one short and leaves the datum behind. #30 makes them agree.
	 */
	@Test
	public void pushAndIncTopDisagreeAboutTheLimit() {
		DataStack byIncTop = new DataStack(10);
		byIncTop.incTop(10 - MARK_SIZE);
		assertEquals(10, byIncTop.getTop(), "incTop reaches the limit exactly");

		DataStack byPush = new DataStack(10);
		assertThrows(OutOfMemoryError.class, () -> {
			for (int i = 0; i < 10; i++) {
				byPush.push(new Data(Data.INT, Integer.valueOf(i)));
			}
		});
		assertEquals(10, byPush.getTop(), "push throws only after growing, see #30");
	}

	@Test
	public void dumpListsTheStackFromTheTopDown() {
		DataStack stack = new DataStack();
		stack.push(new Data(Data.STRING, "top"));
		String dump = stack.toString();
		assertTrue(dump.startsWith("top"), "uppermost element comes first: " + dump);
		assertEquals(MARK_SIZE + 1, dump.lines().count(), "one line per slot");
	}

	@Test
	public void dataClonesAreIndependent() {
		Data original = new Data(Data.INT, Integer.valueOf(1));
		Data copy = (Data) original.clone();
		assertNotSame(original, copy);
		copy.setValue(Integer.valueOf(2));
		assertEquals(1, ((Integer) original.getValue()).intValue(), "clone does not share state");
	}
}
