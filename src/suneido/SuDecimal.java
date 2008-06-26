package suneido;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Wrapper for BigDecimal.
 * @see SuInteger
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class SuDecimal extends SuNumber {
	private BigDecimal n;
	private boolean stripped = false;
	public final static MathContext mc = new MathContext(16);
	public final static BigDecimal INT_MIN = new BigDecimal(Integer.MIN_VALUE);
	public final static BigDecimal INT_MAX = new BigDecimal(Integer.MAX_VALUE);
	public final static SuDecimal ZERO = new SuDecimal(BigDecimal.ZERO);
	public final static SuDecimal ONE = new SuDecimal(BigDecimal.ONE);
	
	public SuDecimal(int n) {
		this.n = new BigDecimal(n);
	}
	public SuDecimal(long n) {
		this.n = new BigDecimal(n);
	}
	public SuDecimal(long n, int s) {
		this.n = new BigDecimal(BigInteger.valueOf(n), s);
	}
	public SuDecimal(String s) {
		this.n = new BigDecimal(s); // may throw NumberFormatException
	}
	public SuDecimal(BigDecimal bd) {
		this.n = bd;
	}

	@Override
	public int index() {
		return integer();
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
	public SuDecimal number() {
		return this;
	}
	
	@Override
	public String toString() {
		strip();
		return n.toString();
	}
		
	/**
	 * Need to strip because two BigDecimal objects that are numerically equal
	 * but differ in scale (like 2.0 and 2.00) may not  have the same hash code. 
	 */ 
	@Override
	public int hashCode() {
		strip();
		return n.hashCode();
	}
	private void strip() {
		if (! stripped) {
			n = n.stripTrailingZeros();
			stripped = true;
		}
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
		if (value instanceof SuDecimal)
			// use compareTo to handle differences in scaling
			return 0 == n.compareTo(((SuDecimal) value).n);
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
	protected SuValue addNum(SuDecimal x) {
		return new SuDecimal(n.add(x.n));
	}
	@Override
	protected SuValue addInt(SuInteger x) {
		return new SuDecimal(n.add(new BigDecimal(x.integer())));
	}
	
	@Override
	public SuValue sub(SuValue x) {
		return x.subNum(this);
	}
	@Override
	protected SuValue subNum(SuDecimal x) {
		return new SuDecimal(x.n.subtract(n));
	}
	@Override
	protected SuValue subInt(SuInteger x) {
		return new SuDecimal(new BigDecimal(x.integer()).subtract(n));
	}
	
	@Override
	public SuValue mul(SuValue x) {
		return x.mulNum(this);
	}
	@Override
	protected SuValue mulNum(SuDecimal x) {
		return new SuDecimal(n.multiply(x.n));
	}
	@Override
	protected SuValue mulInt(SuInteger x) {
		return new SuDecimal(n.multiply(new BigDecimal(x.integer())));
	}
	
	@Override
	public SuValue div(SuValue x) {
		return x.divNum(this);
	}
	@Override
	protected SuValue divNum(SuDecimal x) {
		return new SuDecimal(x.n.divide(n, mc));
	}
	@Override
	protected SuValue divInt(SuInteger x) {
		return new SuDecimal(new BigDecimal(x.integer()).divide(n, mc));
	}
		
	@Override
	public SuValue uminus() {
		return new SuDecimal(n.negate());
	}
	
	@Override
	protected long unscaled() {
		strip();
		return n.unscaledValue().longValue();
	}
	@Override
	protected int scale() {
		strip();
		return n.scale();
	}
}
