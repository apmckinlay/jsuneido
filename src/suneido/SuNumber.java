package suneido;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public abstract class SuNumber extends SuValue {
	protected abstract long unscaled();

	/**
	 * Used by packsize and pack.
	 * @return Same as BigDecimal scale.
	 * If positive, the number of digits to the right of the decimal,
	 * if negative, the number of zeros to be added to the right.
	 * e.g. 1 => 0, 123 => 0, 1000 => -3, 1.1 => 1
	 */
	protected abstract int scale();

	/**
	 * @return The number of bytes needed to pack the number.
	 * Zero takes 1 byte,
	 * otherwise, 1 byte for tag/sign, 1 byte for exponent,
	 * plus 2 bytes (a short) for each 4 decimal digits
	 */
	@Override
	public int packSize(int nest) {
		long n = unscaled();
		if (n == 0)
			return 1;
		if (n < 0)
			n = -n;
		int e = -scale();
		for (; (e % 4) != 0; --e)
			n *= 10;
		return 2 + 2 * packshorts(n);
	}
	private static int packshorts(long n) {
		return n < 100000000L
			? n < 10000 ? 1 : 2
			: n < 1000000000000L ? 3 : 4;
	}

	/**
	 * Serialize/marshal into a ByteBuffer.
	 * An unsigned byte array compare should give same ordering as original values.
	 * Format matches cSuneido SuNumber i.e. base 10000.
	 * Converts unscaled() and scale() to .???? and base 10000 exponent.
	 */
	@Override
	public void pack(ByteBuffer buf) {
		long n = unscaled();
		boolean minus = n < 0;
		buf.put(minus ? Pack.MINUS : Pack.PLUS);
		if (n == 0)
			return ;
		if (n < 0)
			n = -n;
		int e = -scale();
		for (; (e % 4) != 0; --e)
			n *= 10;
		e = e / 4 + packshorts(n);
		byte eb = (byte) ((e ^ 0x80) & 0xff);
		if (minus)
			eb = (byte) ((~eb) & 0xff);
		buf.put(eb);
		packLongPart(buf, n, minus);
	}
	private void packLongPart(ByteBuffer buf, long n, boolean minus) {
		short sh[] = new short[4];
		int i;
		for (i = 0; n != 0; ++i) {
			sh[i] = (short) (minus ? (~(n % 10000) & 0xffff) : (n % 10000));
			n /= 10000;
		}
		while (--i >= 0)
			buf.putShort(sh[i]);
	}

	public static SuValue unpack1(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return SuInteger.ZERO;
		boolean minus = buf.get(0) == Pack.MINUS;
		int s = buf.get() & 0xff;
		if (minus)
			s = ((~s) & 0xff);
		s = (byte) (s ^ 0x80);

		long n = unpackLongPart(buf, minus);
		s = -(s - packshorts(n)) * 4;
		if (-10 <= s && s < 0)
			for (; s < 0; ++s)
				n *= 10;
		if (minus)
			n = -n;
		if (s == 0 && Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
			return SuInteger.valueOf((int) n);
		else
			return new SuDecimal(n, s);
	}
	private static long unpackLongPart(ByteBuffer buf, boolean minus) {
		long n = 0;
		while (buf.remaining() > 0) {
			short x = buf.getShort();
			if (minus)
				x = (short) ((~x) & 0xffff);
			n = n * 10000 + x;
		}
		return n;
	}

	/**
	 * <b>Warning:</b> This <u>ignores</u> any scale.
	 * @param buf
	 * @return The long value.
	 */
	public static long unpackLong(ByteBuffer buf) {
		byte b = buf.get();
		verify(b == Pack.PLUS || b == Pack.MINUS);
		if (buf.remaining() == 0)
			return 0;
		buf.get(); // skip scale/exponent
		return unpackLongPart(buf, b == Pack.MINUS);
	}

	public static SuNumber valueOf(String s) {
		if (s.startsWith("0x"))
			return SuInteger.valueOf(Integer.parseInt(s.substring(2), 16));
		else if (s.startsWith("0"))
			return SuInteger.valueOf(Integer.parseInt(s, 8));
		else if (s.indexOf('.') == -1 && s.indexOf('e') == -1
				&& s.indexOf("E") == -1 && s.length() < 10)
			return SuInteger.valueOf(Integer.parseInt(s));
		else
			return new SuDecimal(s);
	}

//	public static void main(String args[]) {
//		String[] values = {
//				"1", "10", "123", "1000", "9999", "10000", "10002", "100020000", "100020003","1000200030004",
//				".12", ".1", ".01", ".001", ".0001", ".00010002", ".00001"
//				};
//		for (String s : values) {
//			SuNumber n = new SuDecimal(s);
//			int ps = n.packsize();
//			System.out.print(n);
//			ByteBuffer buf = ByteBuffer.allocate(ps);
//			n.pack(buf);
//			for (int i = 0; i < ps; ++i)
//				System.out.print(" " + (int) (buf.get(i) & 0xff));
//			SuValue x = unpack1(buf);
//			System.out.print(" " + x.typeName() + " " + x);
//			System.out.println("");
//		}
//	}
}
