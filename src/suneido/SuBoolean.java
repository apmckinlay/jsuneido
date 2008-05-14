package suneido;

public class SuBoolean extends SuValue {
	private boolean b;
	
	final public static SuBoolean TRUE = new SuBoolean(true);
	final public static SuBoolean FALSE = new SuBoolean(false);
	
	private SuBoolean(boolean b) {
		this.b = b;
	}

	@Override
	public String toString() {
		return b ? "true" : "false";
	}
	@Override
	public int integer() {
		return b ? 1 : 0;
	}
	@Override
	public SuNumber number() {
		return b ? SuNumber.ONE : SuNumber.ZERO;
	}
	
	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		return (this == TRUE ? 1 : 0) - (value == TRUE ? 1 : 0);
	}
	@Override
	public int order() {
		return Order.BOOLEAN.ordinal();
	}
}
