package suneido;

import java.nio.ByteBuffer;

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
	public SuDecimal number() {
		return b ? SuDecimal.ONE : SuDecimal.ZERO;
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

	@Override
	public int packSize() {
		return 1;
	}
	@Override
	public void pack(ByteBuffer buf) {
		buf.put(b ? Pack.TRUE : Pack.FALSE);
	}
}
