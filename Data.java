/* Data.java */

/* $Id$ */

/**
 * A class to represent a tagged datum.
 */
public class Data {
    /** The type of this datum. */
    private int type;

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
}
