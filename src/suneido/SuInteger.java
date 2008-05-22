package suneido;

public class SuInteger extends SuNumber {
	private int n;
	final public static SuInteger ZERO = new SuInteger(0);
	final public static SuInteger ONE = new SuInteger(1);
	
	public SuInteger(int n) {
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
		return new Integer(n).hashCode();
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
		return new Integer(n).compareTo(i.n);
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
		return new SuInteger(x.n + n);
	}

	@Override
	public SuValue sub(SuValue x) {
		return x.subInt(this);
	}
	@Override
	protected SuValue subInt(SuInteger x) {
		return new SuInteger(x.n - n);
	}

	@Override
	public SuValue mul(SuValue x) {
		return x.mulInt(this);
	}
	@Override
	protected SuValue mulInt(SuInteger x) {
		return new SuInteger(x.n * n);
	}

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
}
