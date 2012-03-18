package net.logicsquad.pal;

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
	 * @param type
	 *            The type of this datum.
	 * @param value
	 *            An <code>Object</code> representing the value of this datum.
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

		if (value == null) {
			valCopy = null;
		} else if (value instanceof Integer) {
			valCopy = new Integer(((Integer) value).intValue());
		} else if (value instanceof Float) {
			valCopy = new Float(((Float) value).floatValue());
		} else if (value instanceof Boolean) {
			valCopy = new Boolean(((Boolean) value).booleanValue());
		} else if (value instanceof String) {
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
	 * @param type
	 *            An <code>int</code> representing the type of this datum.
	 */
	public void setType(int type) {
		this.type = type;
		return;
	}

	/**
	 * Sets the value of this datum.
	 * 
	 * @param value
	 *            An <code>Object</code> representing the value of this datum.
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
