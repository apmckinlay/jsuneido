package suneido;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.Util.array;
import static suneido.Util.bufferToString;
import static suneido.language.SuClass.massage;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import suneido.language.FunctionSpec;

/**
 * Wrapper for Java String
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuString extends SuValue {
	private final String s;
	final public static SuString EMPTY = new SuString("");

	public static SuString valueOf(String s) {
		return s.equals("") ? EMPTY : new SuString(s);
	}

	/**
	 * @return An SuString with escape sequences interpreted
	 */
	public static SuString literal(String s) {
		int i = s.indexOf('\\');
		if (i == -1)
			return valueOf(s);
		StringBuilder buf = new StringBuilder(s);
		for (; -1 != (i = buf.indexOf("\\", i)); ++i)
			escape(buf, i);
		return valueOf(buf.toString());
	}

	private static void escape(StringBuilder buf, int i) {
		int end = i + 1;
		char c = buf.charAt(i + 1);
		switch (c) {
		case 'n':
			c = '\n';
			break;
		case 'r':
			c = '\r';
			break;
		case 't':
			c = '\t';
			break;
		case 'x':
			if (i + 4 > buf.length())
				return;
			c = (char) Integer.parseInt(buf.substring(i + 2, i + 4), 16);
			end += 3;
			break;
		case '0':
		case '1':
		case '2':
		case '3':
			if (i + 4 > buf.length())
				return;
			c = (char) Integer.parseInt(buf.substring(i + 1, i + 4), 8);
			end += 3;
			break;
		default:
			return; // unrecognized sequences are kept as is
		}
		buf.setCharAt(i, c);
		buf.delete(i + 1, end);
	}

	protected SuString(String s) {
		this.s = s;
	}

	public static SuString makeUnique(String s) {
		return new SuString(s);
	}

	@Override
	public String string() {
		return s;
	}

	/**
	 * @param member Converted to an integer zero-based position in the string.
	 * @return An SuString containing the single character at the position,
	 * 			or "" if the position is out of range.
	 */
	@Override
	public SuValue get(SuValue member) {
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
	public SuNumber number() {
		try {
			return SuNumber.valueOf(s);
		} catch (NumberFormatException e) {
			return super.number();
		}
	}

	@Override
	public String toString() {
		return "'" + s.replace("'", "\\'") + "'"; //TODO smarter quoting/escaping
	}

	@Override
	public String strIfStr() {
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
	public int packSize(int nest) {
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
		if (buf.remaining() == 0)
			return EMPTY;
		return new SuString(bufferToString(buf));
	}

	// methods ======================================================

	private static FunctionSpec[] params = new FunctionSpec[] {
		new FunctionSpec("Substr", array("i", "n"), 2),
		new FunctionSpec("Size", new String[0], 0),
	};
	private static final int SUBSTR = 0;
	private static final int SIZE = 1;

	@Override
	public SuValue invoke(String method, SuValue ... args) {
		if (method == "Substr")
			return substr(massage(params[SUBSTR], args));
		else if (method == "Size")
			return size(massage(params[SIZE], args));
		else
			return super.invoke(method, args);
	}

	private SuValue substr(SuValue[] args) {
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

	private SuValue size(SuValue[] args) {
		return SuInteger.valueOf(s.length());
	}
}
