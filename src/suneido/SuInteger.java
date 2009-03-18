package suneido;

import java.math.BigDecimal;

// import java.util.Random;

/**
 * Wrapper for a Java int.
 * @see SuDecimal
 */
public class SuInteger extends SuNumber {
	final int n;
	final public static SuInteger ZERO = new SuInteger(0);
	final public static SuInteger ONE = new SuInteger(1);

	// lazy initialization as per Effective Java
	private static class SmallInts {
		static SuInteger[] vals = new SuInteger[256];
		static {
			for (int i = 0; i < 256; ++i)
				vals[i] = new SuInteger(i - 128);
		}
	}

	public static SuInteger valueOf(int n) {
		return -128 <= n && n < 128
			? SmallInts.vals[n + 128]
			: new SuInteger(n);
	}

	private SuInteger(int n) {
		this.n = n;
	}

	@Override
	public int index() {
		return n;
	}
	@Override
	public int integer() {
		return n;
	}

	@Override
	public SuDecimal number() {
		return new SuDecimal(n);
	}

	@Override
	public String toString() {
		return "" + n;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(n).hashCode();
	}
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof SuInteger)
			return n == ((SuInteger) other).n;
		if (other instanceof SuDecimal)
			return other.equals(this); // SuNumber
		return false;
	}
	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		return value.compareToInt(this);
	}
	@Override
	protected int compareToInt(SuInteger x) {
		return Integer.valueOf(x.n).compareTo(n);
	}
	@Override
	protected int compareToDec(SuDecimal x) {
		return x.n.compareTo(BigDecimal.valueOf(n));
	}
	@Override
	public int order() {
		return Order.NUMBER.ordinal();
	}

	// double dispatch
	@Override
	public SuNumber add(SuValue that) {
		return that.addInt(this);
	}
	@Override
	protected SuInteger addInt(SuInteger x) {
		return valueOf(x.n + n);
	}
	@Override
	protected SuDecimal addDec(SuDecimal x) {
		return new SuDecimal(x.n.add(BigDecimal.valueOf(n)));
	}

	// double dispatch
	@Override
	public SuNumber sub(SuValue that) {
		return that.subInt(this);
	}
	@Override
	protected SuInteger subInt(SuInteger x) {
		return valueOf(x.n - n);
	}
	@Override
	protected SuDecimal subDec(SuDecimal x) {
		return new SuDecimal(x.n.subtract(BigDecimal.valueOf(n)));
	}

	// double dispatch
	@Override
	public SuNumber mul(SuValue that) {
		return that.mulInt(this);
	}
	@Override
	protected SuInteger mulInt(SuInteger x) {
		return valueOf(x.n * n);
	}
	@Override
	protected SuDecimal mulDec(SuDecimal x) {
		return new SuDecimal(x.n.multiply(BigDecimal.valueOf(n)));
	}

	// double dispatch
	@Override
	public SuNumber div(SuValue that) {
		return that.divInt(this);
	}
	@Override
	protected SuDecimal divInt(SuInteger x) {
		return new SuDecimal(BigDecimal.valueOf(x.n).divide(
				BigDecimal.valueOf(n), SuDecimal.mc));
	}
	@Override
	protected SuDecimal divDec(SuDecimal x) {
		return new SuDecimal(x.n.divide(BigDecimal.valueOf(n), SuDecimal.mc));
	}

	// double dispatch
	@Override
	public SuNumber mod(SuValue that) {
		return that.modInt(this);
	}
	@Override
	protected SuInteger modInt(SuInteger x) {
		return valueOf(x.n % n);
	}
	@Override
	protected SuDecimal modDec(SuDecimal x) {
		return new SuDecimal(x.n.remainder(BigDecimal.valueOf(n), SuDecimal.mc));
	}

	@Override
	public SuInteger uminus() {
		return new SuInteger(-n);
	}

	@Override
	protected long unscaled() {
		return n;
	}
	@Override
	protected int scale() {
		return 0;
	}

//	public static void main(String args[]) {
//		SuInteger.init();
//		SuValue y;
//		for (int j = 0; j < 10; ++j) {
//			Random gen = new Random(1234567);
//			long t = System.currentTimeMillis();
//			for (int i = 0; i < 100 * 1000 * 1000; ++i) {
//				y = new SuInteger(gen.nextInt(100));
//			}
//			long base = System.currentTimeMillis() - t;
//
//			SuValue x = new SuInteger(0);
//			t = System.currentTimeMillis();
//			for (int i = 0; i < 100 * 1000 * 1000; ++i) {
//				y = new SuInteger(gen.nextInt(100));
//				x = x.add(y); x = x.add(y); x = x.sub(y); x = x.sub(y);
//			}
//			System.out.println(System.currentTimeMillis() - t - base);
//		}
//		System.out.println("done");
//	}
}
