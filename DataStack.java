/* DataStack.java */

/**
 * PAL Machine Simulator: An implementation in Java
 *
 * Copyright (c) 2002, 2003 Philip J. Roberts and Paul A. Hoadley
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the authors nor the names of any other
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

/**
 * An abstract data type representing the PAL data stack.
 *
 * @version $Revision$
 * @author Philip Roberts &lt;philip.roberts@student.adelaide.edu.au&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class DataStack {
    /** A container for the <code>Data</code> objects. */
    private List data;

    /** The address of the current frame's base. */
    private int frameBase;

    /** Reference to the next free space on top of the stack (TOS). */
    private int top;

    /** Maximum stack size. A value &lt;= 0 indicates no limit.*/
    private int maxSize;

    /**
     * Default constructor.  Assumes no limit on stack size.
     */
    public DataStack() {
        this(0);
    }

    /**
     * Constructor.  Allows user to specify a limit on maximum size.
     *
     * @param max Maximum stack size.
     */
    public DataStack(int max) {
        top = 0;

        data = new ArrayList();

        // Set up mark stack part for main program activation record.
        markStack(0, 0);

        // Sets the first frame's base address.
        setBase(top);

        maxSize = (max > 0 ? max : 0);
    }

    /**
     * Put a data object onto the top of the stack.
     *
     * @param datum A <code>Data</code> object to be pushed onto the
     * stack.
     * @exception java.lang.OutOfMemoryError if there is insufficient
     * free stack space.
     */
    public void push(Data datum) throws OutOfMemoryError {
        top++;

        data.add(datum);

        if (maxSize == 0 || top < maxSize) {
            return;
        }

        throw new OutOfMemoryError("PAL Stack out of memory.");
    }

    /**
     * Pop the top value from the stack.
     *
     * @return The <code>Data</code> object removed from the top of
     * the stack.
     */
    public Data pop() {
        return (Data) data.remove(--top);
    }

    /**
     * Peek at the top of the stack.
     * 
     * @return The <code>Data</code> object remaining on the top of
     * the stack.
     */
    public Data peek() {
        return (Data) data.get(top - 1);
    }

    /**
     * Read a data location elsewhere in the stack.  The location is
     * given as an absolute stack address.
     *
     * @param address The absolute address for the target location.
     * @return The <code>Data</code> object at the target location.
     * @exception java.lang.IndexOutOfBoundsException if the supplied
     * address is out of bounds.
     */
    public Data get(int address) throws IndexOutOfBoundsException {
        if (address < 0 || address >= top) {
            throw new IndexOutOfBoundsException("PAL address out of bounds.");
        }

        return (Data) data.get(address);
    }

    /**
     * Read a data location elsewhere in the stack.  The location is
     * given as a level difference and offset.
     *
     * @param levelDiff The difference in static scope level between
     * the current activation record, and the activation record of the
     * target location.
     * @param offset The offset into the target stack frame.
     * @return The <code>Data</code> object at the target address.
     * @exception java.lang.IndexOutOfBoundsException if the supplied
     * address is out of bounds.
     */
    public Data get(int levelDiff, int offset) throws IndexOutOfBoundsException {
        int address = getAddress(levelDiff, offset);

        return get(address);
    }

    /**
     * Advance the TOS pointer by a given amount, initialising memory
     * to type UNDEF as we go.
     *
     * @param amount The number of location to advance the TOS
     * pointer.
     * @exception java.lang.OutOfMemoryError if an attempt is made to
     * advance the TOS pointer beyond the limit of the stack memory.
     */
    public void incTop(int amount) throws OutOfMemoryError {
        if ((maxSize != 0) && (amount + top > maxSize)) {
            throw new OutOfMemoryError("PAL Stack out of memory");
        }

        for (int i = 0;i < amount;i++) {
            data.add(new Data(Data.UNDEF, null));
            top++;
        }
    }

    /**
     * Mark the stack.
     *
     * @param staticLink A pointer to the activation record one level
     * below the current level in terms of <em>lexical scope</em>.
     * @param dynamicLink A pointer to the activation record one level
     * below the current level in terms of <em>dynamic scope</em>.
     * @exception java.lang.OutOfMemoryError if the TOS pointer is
     * advanced beyond the limit of stack memory.
     */
    public void markStack(int staticLink, int dynamicLink) throws OutOfMemoryError {
        push(new Data(Data.INT, new Integer(staticLink)));
        push(new Data(Data.INT, new Integer(dynamicLink)));

        // Leave space for return point.
        push(new Data(Data.INT, new Integer(0)));

        // Dummy exception handler address - indicates that no handler
        // is registered.
        push(new Data(Data.INT, new Integer(0)));
    }

    /**
     * Set the current frame's base address.
     *
     * @param address The address to store as the current frame base.
     */
    public void setBase(int address) {
        frameBase = address;
    }

    /**
     * Get the absolute address for a stack location, given the level
     * difference and offset.
     *
     * @param levelDiff The difference in static scope level between
     * the current activation record, and the activation record of the
     * target location.
     * @param offset The offset into the target stack frame.
     * @return The absolute address for the target location.
     * @exception java.lang.IndexOutOfBoundsException if the supplied
     * level difference is invalid.
     */
    public int getAddress(int levelDiff, int offset) throws IndexOutOfBoundsException {
        int result = frameBase;

        for (int i = 0;i < levelDiff;i++) {
            // Extract the static link from the stack mark.
            result = ((Integer) get(result - 4).getValue()).intValue();
        }

        result += offset;

        return result;
    }

    /**
     * Get the address of the stack top.
     *
     * @return The absolute address of the top element.
     */
    public int getTop() {
        return top;
    }

    /**
     * Returns a <code>String</code> representation of the object.
     * Effectively, this is a dump of the stack from the uppermost
     * element to the lowermost.
     *
     * @return A <code>String</code> representation of the object.
     */
    public String toString() {
        String result = new String();
        for (int i = top - 1;i >= 0;i--) {
            result += get(i) + "\n";
        }

        return result;
    }
}
