package net.logicsquad.pal;

/* Data.java */

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

/**
 * A class to represent a tagged datum.
 *
 * @author Philip Roberts &lt;philip.roberts@gmail.com&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
public class Data implements Cloneable {
    /** The type of this datum. */
    private int type;

    /** Constant to represent integer data type. */
    public static final int INT = 0;

    /** Constant to represent real data type. */
    public static final int REAL = 1;

    /** Constant to represent string data type. */
    public static final int STRING = 2;

    /** Constant to represent boolean data type. */
    public static final int BOOL = 3;

    /** Constant to represent undefined data type. */
    public static final int UNDEF = 4;

    /** The value of this datum. */
    private Object value;

    /**
     * Constructor.
     *
     * @param type The type of this datum.
     * @param value An <code>Object</code> representing the value of
     * this datum.
     * @see Code#Code
     */
    public Data(int type, Object value) {
	this.type = type;
	this.value = value;
	return;
    }

    /**
     * Method to duplicate this object.
     *
     * @return A duplicate of this object.
     */
    public Object clone() {
        Object valCopy = null;

        if(value == null) {
            valCopy = null;
        } else if(value instanceof Integer) {
            valCopy = new Integer(((Integer)value).intValue());
        } else if(value instanceof Float) {
            valCopy = new Float(((Float)value).floatValue());
        } else if(value instanceof Boolean) {
            valCopy = new Boolean(((Boolean)value).booleanValue());
        } else if(value instanceof String) {
            valCopy = new String(value.toString());
        }

        return new Data(this.type, valCopy);
    }

    /**
     * Returns the type of this datum.
     *
     * @return The type of this datum.
     */
    public int getType() {
	return type;
    }

    /**
     * Returns the value of this datum.
     *
     * @return The value of this datum.
     * @see Code#Code
     */
    public Object getValue() {
	return value;
    }

    /**
     * Sets the type of this datum.
     *
     * @param type An <code>int</code> representing the type of this
     * datum.
     */
    public void setType(int type) {
	this.type = type;
	return;
    }

    /**
     * Sets the value of this datum.
     *
     * @param value An <code>Object</code> representing the value of
     * this datum.
     * @see Code#Code
     */
    public void setValue(Object value) {
	this.value = value;
	return;
    }

    /**
     * Returns a String representation of the value in this datum.
     *
     * @return A String representation of the value in this datum.
     */
    public String toString() {
	if (type == UNDEF) {
	    return "UNDEF";
	} else {
	    return value.toString();
	}
    }
}
