package suneido;

import java.math.BigDecimal;
import java.math.MathContext;

public class SuNumber extends SuValue {
	private BigDecimal n;
	public final static MathContext mc = new MathContext(16);
	public final static BigDecimal INT_MIN = new BigDecimal(Integer.MIN_VALUE);
	public final static BigDecimal INT_MAX = new BigDecimal(Integer.MAX_VALUE);
	public final static SuNumber ZERO = new SuNumber(0);
	public final static SuNumber ONE = new SuNumber(1);
	
	public SuNumber(int n) {
		this.n = new BigDecimal(n);
	}
	public SuNumber(String s) {
		this.n = new BigDecimal(s); // may throw NumberFormatException
	}
	public SuNumber(BigDecimal bd) {
		this.n = bd;
	}

	@Override
	public int integer() {
		if (n.compareTo(INT_MIN) == -1)
			return Integer.MIN_VALUE;
		else if (n.compareTo(INT_MAX) == 1)
			return Integer.MAX_VALUE;
		else
			return n.intValue();
	}
	@Override
	public SuNumber number() {
		return this;
	}
	
	@Override
	public boolean is_numeric() {
		return true;
	}

	@Override
	public String toString() {
		return n.toString();
	}
		
	@Override
	public int hashCode() {
		return n.hashCode();
	}
	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		if (value instanceof SuInteger)
			try {
				return n.intValueExact() == ((SuInteger) value).integer();
			} catch (ArithmeticException e) {
				return false;
			}
		if (value instanceof SuNumber)
			return n.equals(((SuNumber) value).n);
		return false;
	}
	@Override
	public int compareTo(SuValue value) {
		int ord = order() - value.order();
		return ord < 0 ? -1 : ord > 0 ? +1 :
			n.compareTo(value.number().n);
	}
	@Override
	public int compareToInt(SuInteger i) {
		return n.compareTo(new BigDecimal(i.integer()));
	}
	@Override
	public int order() {
		return Order.NUMBER.ordinal();
	}
	
	@Override
	public SuValue add(SuValue x) {
		return x.addNum(this);
	}
	@Override
	protected SuValue addNum(SuNumber x) {
		return new SuNumber(n.add(x.n));
	}
	@Override
	protected SuValue addInt(SuInteger x) {
		return new SuNumber(n.add(new BigDecimal(x.integer())));
	}
	
	@Override
	public SuValue sub(SuValue x) {
		return x.subNum(this);
	}
	@Override
	protected SuValue subNum(SuNumber x) {
		return new SuNumber(x.n.subtract(n));
	}
	@Override
	protected SuValue subInt(SuInteger x) {
		return new SuNumber(new BigDecimal(x.integer()).subtract(n));
	}
	
	@Override
	public SuValue mul(SuValue x) {
		return x.mulNum(this);
	}
	@Override
	protected SuValue mulNum(SuNumber x) {
		return new SuNumber(n.multiply(x.n));
	}
	@Override
	protected SuValue mulInt(SuInteger x) {
		return new SuNumber(n.multiply(new BigDecimal(x.integer())));
	}
	
	@Override
	public SuValue div(SuValue x) {
		return x.divNum(this);
	}
	@Override
	protected SuValue divNum(SuNumber x) {
		return new SuNumber(x.n.divide(n, mc));
	}
	@Override
	protected SuValue divInt(SuInteger x) {
		return new SuNumber(new BigDecimal(x.integer()).divide(n, mc));
	}
		
	@Override
	public SuValue uminus() {
		return new SuNumber(n.negate());
	}
}
