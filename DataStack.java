/* DataStack.java */

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * An abstract data type representing the PAL data stack.
 */
public class DataStack {
    /**The underlying container */
    private List data, stackFrames;

    /**Reference index of the top of the stack (TOS) */
    int top;

    //FIXME doesn't actually do anything yet - maybe throw an exception?
    /**Maximum stack size */
    int maxSize;

    /**
     * Default Constructor - assumes no limit on stack size.
     */
    public DataStack() {
        this(0);
//          data = new ArrayList();

//          stackFrames = new LinkedList();

//          top = 0;

//          maxSize = 0;
    }

    /**
     * Constructor - allows user to specify a limit on maximum size.
     */
    public DataStack(int max) {
        data = new ArrayList();

        stackFrames = new LinkedList();

        //FIXME - should there be a stack mark for the lowest level,
        //e.g. to hold exception handler offsets?

        //Base address of first stack frame.
        stackFrames.add(new Integer(0));

        top = 0;

        maxSize = max;
    }

    //FIXME - should we have these modifiying methods, or should
    // we just read out the actual Data object, then modify it?

    /**
     * Put a data object onto the top of the stack.
     */
    public void push(Data datum) {
        top++;

        data.add(datum);
    }

    /**
     * Overwrite a data location elsewhere in the stack.
     * The location is given as an absolute stack address.
     */
    public void put(Data datum, int address) {
        //FIXME check bounds on address?
        data.add(address, datum);
    }

    /**
     * Overwrite a data location elsewhere in the stack.
     * The location is given as a level difference and offset.
     */
    public void put(Data datum, int levelDiff, int offset) {
        //FIXME check bounds on address?  That includes making
        // sure the offset remains below the next stack frame...

        int address = offset;

        address += ((Integer)stackFrames.get(stackFrames.size() - levelDiff)).intValue();

        data.set(address, datum);
    }

    /**
     * Pop the top value from the stack.
     */
    public Data pop() {
        return (Data)data.remove(--top);
    }

    /**
     * Peek at the top of the stack.  FIXME do we need this?
     */
    public Data peek() {
        return (Data)data.get(top);
    }

    /**
     * Read a data location elsewhere in the stack.
     * The location is given as an absolute stack address.
     */
    public Data get(int address) {
        //FIXME check bounds on address?
        return (Data)data.get(address);
    }

    /**
     * Read a data location elsewhere in the stack.
     * The location is given as a level difference and offset.
     */
    public Data get(int levelDiff, int offset) {
        //FIXME check bounds on address?
        int address = offset;
        address += ((Integer)stackFrames.get(stackFrames.size() - levelDiff)).intValue();

        return (Data)data.get(address);
    }

    /**
     * Advance the TOS pointer by a given amount, initialising memory as we go.
     */
    public void incTop(int amount) {
        for(int i = 0;i < amount;i++) {
            top++;
            data.set(top, new Data(Data.UNDEF, null));
        }
    }

    /**
     * Mark the stack.
     * Takes two parameters (FIXME describe):
     * - static link
     * - dynamic link
     */
    public void markStack(int staticLink, int dynamicLink) {
        push(new Data(Data.INT, new Integer(staticLink)));
        push(new Data(Data.INT, new Integer(dynamicLink)));

        incTop(2);

        stackFrames.add(new Integer(top));
    }
}
