package suneido;

import java.nio.ByteBuffer;

public abstract class SuNumber extends SuValue {
	protected abstract long unscaled();
	protected abstract int scale();
	
	@Override
	public int packsize() {
		long n = unscaled();
		if (n == 0)
			return 1;
// TODO packsize
		return 0;
	}
	
	@Override
	public void pack(ByteBuffer buf) {
		long n = unscaled();
		if (n == 0) {
			buf.put(Pack.PLUS);
			return ;
		}
		buf.put(n < 0 ? Pack.MINUS : Pack.PLUS);
		if (n < 0)
			n = -n;
		int e = scale();
		packLong(buf, n);
// TODO pack		
	}
	private void packLong(ByteBuffer buf, long n) {
		short sh[] = new short[4];
		int i;
		for (i = 0; n != 0; ++i) {
			sh[i] = (short) (n % 10000);
			n /= 10000;
		}
		for (; i >= 0; --i)
			buf.putShort(sh[i]);		
	}
	
	public static SuValue unpack1(ByteBuffer buf) {
		if (buf.limit() <= 1)
			return SuInteger.ZERO;
		long n = 0;
// TODO unpack
		if (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
			return new SuInteger((int) n);
		else
			return new SuDecimal(n);
	}
}
