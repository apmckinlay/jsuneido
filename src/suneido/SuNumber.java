package suneido;

import java.nio.ByteBuffer;

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
	public int packsize() {
		long n = unscaled();
		return n == 0 ? 1 : 2 + 2 * packshorts(n);
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
		buf.put(n < 0 ? Pack.MINUS : Pack.PLUS);
		if (n == 0)
			return ;
		if (n < 0)
			n = -n;
		int e = -scale();
		while ((e % 4) != 0) {
			n *= 10;
			--e;
		}
//System.out.print("\tunscaled " + n + " scale " + -e);
		e = e / 4 + packshorts(n);
		buf.put((byte) (e + 128));
		packLong(buf, n);
	}
	private void packLong(ByteBuffer buf, long n) {
		short sh[] = new short[4];
		int i;
		for (i = 0; n != 0; ++i) {
			sh[i] = (short) (n % 10000);
			n /= 10000;
		}
		while (--i >= 0)
			buf.putShort(sh[i]);		
	}
	
	public static SuValue unpack1(ByteBuffer buf) {
		if (buf.limit() <= 1)
			return SuInteger.ZERO;
		int s = (int) (buf.get(1) & 0xff) - 128;
		long n = unpackLong(buf);
		s = -(s - packshorts(n)) * 4;
		if (-10 <= s && s < 0)
			for (; s < 0; ++s)
				n *= 10;
		if (buf.get(0) == Pack.MINUS)
			n = -n;
		if (s == 0 && Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
			return new SuInteger((int) n);
		else
			return new SuDecimal(n, s);
	}
	private static long unpackLong(ByteBuffer buf) {
		long n = 0;
		for (int i = 2; i < buf.limit(); i += 2)
			n = n * 10000 + buf.getShort(i);
		return n;
	}

	public static void main(String args[]) {
		String[] values = { 
				"1", "10", "123", "1000", "9999", "10000", "10002", "100020000", "100020003","1000200030004",
				".12", ".1", ".01", ".001", ".0001", ".00010002", ".00001"
				};
		for (String s : values) {
			SuNumber n = new SuDecimal(s);
			int ps = n.packsize();
			System.out.print(n);
//			System.out.print("\tunscaled " + n.unscaled() + " scale " + n.scale());
			ByteBuffer buf = ByteBuffer.allocate(ps);
			n.pack(buf);
			for (int i = 0; i < ps; ++i)
				System.out.print(" " + (int) (buf.get(i) & 0xff));
			SuValue x = unpack1(buf);
			System.out.print(" " + x.typeName() + " " + x);
			System.out.println("");
			
		}
	}
}
