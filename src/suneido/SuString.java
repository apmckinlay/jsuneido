package suneido;

/**
 * Suneido string class - simple wrapper for Java String
 * @author Andrew McKinlay
 *
 */
public class SuString extends SuValue {
	private String s;
	final public static SuString EMPTY = new SuString("");
	
	public SuString(String s) {
		this.s = s;
	}
	
	/**
	 * @param member Converted to an integer zero-based position in the string.
	 * @return An SuString containing the single character at the position, 
	 * 			or "" if the position is out of range.
	 */
	@Override
	public SuValue getdata(SuValue member) {
		if (! member.is_numeric())
			throw new SuException("strings subscripts must be integers");
		int i = member.integer();
		return 0 <= i && i < s.length()
			? new SuString(s.substring(i, i + 1))
			: EMPTY;
	}

	@Override
	public int integer() {
		String t = s;
		int radix = 10;
		if (s.startsWith("0x") || s.startsWith("0X")) {
			radix = 16;
			t = s.substring(2);
		}
		else if (s.startsWith("0"))
			radix = 8;
		try {
			return Integer.parseInt(t, radix);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	@Override
	public SuNumber number() {
		try {
			return new SuNumber(s);
		} catch (NumberFormatException e) {
			return super.number();
		}
	}

	@Override
	public String toString() {
		return s;
	}

	@Override
	public int hashCode() {
		return s.hashCode();
	}
	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		if (value instanceof SuString)
			return s.equals(((SuString) value).s);
		return false;
	}
	@Override
	public int compareTo(SuValue value) {
		int ord = order() - value.order();
		return ord < 0 ? -1 : ord > 0 ? +1 :
			Integer.signum(s.compareTo(((SuString) value).s));
	}
	@Override
	public int order() {
		return Order.STRING.ordinal();
	}
}
