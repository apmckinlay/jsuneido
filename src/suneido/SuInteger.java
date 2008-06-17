package suneido;

// import java.util.Random;

public class SuInteger extends SuNumber {
	private int n;
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
	public boolean equals(Object value) {
		if (value instanceof SuInteger)
			return n == ((SuInteger) value).n;
		if (value instanceof SuDecimal)
			return value.equals(this); // SuNumber
		return false;
	}
	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		return -value.compareToInt(this);
	}
	@Override 
	public int compareToInt(SuInteger i) {
		return Integer.valueOf(n).compareTo(i.n);
	}
	@Override
	public int order() {
		return Order.NUMBER.ordinal();
	}

	@Override
	public SuValue add(SuValue x) {
		return x.addInt(this);
	}
	@Override
	protected SuValue addInt(SuInteger x) {
		return valueOf(x.n + n);
	}

	@Override
	public SuValue sub(SuValue x) {
		return x.subInt(this);
	}
	@Override
	protected SuValue subInt(SuInteger x) {
		return valueOf(x.n - n);
	}

	@Override
	public SuValue mul(SuValue x) {
		return x.mulInt(this);
	}
	@Override
	protected SuValue mulInt(SuInteger x) {
		return valueOf(x.n * n);
	}
	
	// div is handled by SuDecimal

	@Override
	public SuValue uminus() {
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
