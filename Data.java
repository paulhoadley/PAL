/* Data.java */

/* $Id$ */

/**
 * A class to represent a tagged datum.
 */
public class Data {
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
     */
    public Data(int type, Object value) {
	this.type = type;
	this.value = value;
	return;
    }

    /**
     * Returns the type of this datum.
     */
    public int getType() {
	return type;
    }

    /**
     * Returns the value of this datum.
     */
    public Object getValue() {
	return value;
    }

    /**
     * Sets the type of this datum.
     */
    public void setType(int type) {
	this.type = type;
	return;
    }

    /**
     * Sets the value of this datum.
     */
    public void setValue(Object value) {
	this.value = value;
	return;
    }

    /**
     * Returns a String representation of the value in this Data
     * object.
     */
    public String toString() {
	return value.toString();
    }
}
