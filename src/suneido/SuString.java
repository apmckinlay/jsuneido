package suneido;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import static java.lang.Math.min;
import static java.lang.Math.max;


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
			return super.integer();
		}
	}
	
	@Override
	public SuDecimal number() {
		try {
			return new SuDecimal(s);
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

	// packing ======================================================
	@Override
	public int packsize() {
		int n = s.length();
		return n == 0 ? 0 : 1 + n; 
	}

	@Override
	public void pack(ByteBuffer buf) {
		if (s.length() == 0)
			return ;
		buf.put(Pack.STRING);
		try {
			buf.put(s.getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			throw new SuException("can't pack string", e);
		}
	}

	public static SuValue unpack1(ByteBuffer buf) {
		if (buf.limit() <= 1)
			return EMPTY;
		buf.get(); // skip STRING
		byte[] bytes = new byte[buf.remaining()];
		buf.get(bytes);
		try {
			return new SuString(new String(bytes, "US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			throw new SuException("can't unpack string", e);
		}
	}
	
	// methods ======================================================
	
	// TODO program that finds all the method symbols in the source
	// and creates a file to initialize them all
	// since they have to be constant ints for switches
	
	public SuValue invoke2(SuValue self, int method, SuValue[] args) {
		switch (method) {
		case SuSymbol.SUBSTR :
			return substr(args);
		default:
			return super.invoke2(method, args);
		}
	}
	private SuValue substr(SuValue[] args) {
		final int[] params = new int[] { SuSymbol.I, SuSymbol.N };
		args = SuClass.massage(2, args, params);
		int len = s.length();		
		int i = args[0].integer();
		if (i < 0)
			i += len;
		i = max(0, min(i, len - 1));
		int n = args[1] == null ? len : args[1].integer();
		if (n < 0)
			n += len - i;
		n = max(0, min(n, len - i));
		return new SuString(s.substring(i, i + n));
	}
}
