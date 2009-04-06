package suneido;

import java.math.*;

/**
 * Wrapper for BigDecimal.
 * @see SuInteger
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. 
 * Licensed under GPLv2.</small></p>
 */
public class SuDecimal extends SuNumber {
	BigDecimal n;
	private boolean stripped = false;
	public final static MathContext mc = new MathContext(16);
	public final static BigDecimal INT_MIN = new BigDecimal(Integer.MIN_VALUE);
	public final static BigDecimal INT_MAX = new BigDecimal(Integer.MAX_VALUE);
	public final static SuDecimal ZERO = new SuDecimal(BigDecimal.ZERO);
	public final static SuDecimal ONE = new SuDecimal(BigDecimal.ONE);

	public SuDecimal(int n) {
		this.n = BigDecimal.valueOf(n);
	}
	public SuDecimal(long n) {
		this.n = BigDecimal.valueOf(n);
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
		String s = Math.abs(scale()) > 10 ? n.toString() : n.toPlainString();
		return removeLeadingZero(s).replace("E", "e").replace("e+", "e");
	}
	private String removeLeadingZero(String s) {
		if (s.startsWith("0.") && s.length() > 2)
			s = s.substring(1);
		return s;
	}

	/**
	 * Need to strip because two BigDecimal objects that are numerically equal
	 * but differ in scale (like 2.0 and 2.00) may not have the same hash code.
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
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof SuDecimal)
			// use compareTo to handle differences in scaling
			return 0 == n.compareTo(((SuDecimal) other).n);
		if (other instanceof SuInteger)
			try {
				return n.intValueExact() == ((SuInteger) other).integer();
			} catch (ArithmeticException e) {
				return false;
			}
		return false;
	}

	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		return value.compareToDec(this);
	}
	@Override
	protected int compareToDec(SuDecimal x) {
		return x.n.compareTo(n);
	}
	@Override
	protected int compareToInt(SuInteger x) {
		return BigDecimal.valueOf(x.n).compareTo(n);
	}

	@Override
	public int order() {
		return Order.NUMBER.ordinal();
	}

	// double dispatch
	@Override
	public SuNumber add(SuValue that) {
		return that.addDec(this);
	}
	@Override
	protected SuDecimal addDec(SuDecimal x) {
		return new SuDecimal(n.add(x.n));
	}
	@Override
	protected SuDecimal addInt(SuInteger x) {
		return new SuDecimal(n.add(BigDecimal.valueOf(x.integer())));
	}

	@Override
	public SuNumber sub(SuValue that) {
		return that.subDec(this);
	}
	@Override
	protected SuDecimal subDec(SuDecimal x) {
		return new SuDecimal(x.n.subtract(n));
	}
	@Override
	protected SuDecimal subInt(SuInteger x) {
		return new SuDecimal(BigDecimal.valueOf(x.n).subtract(n));
	}

	@Override
	public SuNumber mul(SuValue that) {
		return that.mulDec(this);
	}
	@Override
	protected SuDecimal mulDec(SuDecimal x) {
		return new SuDecimal(x.n.multiply(n));
	}
	@Override
	protected SuDecimal mulInt(SuInteger x) {
		return new SuDecimal(BigDecimal.valueOf(x.n).multiply(n));
	}

	@Override
	public SuNumber div(SuValue that) {
		return that.divDec(this);
	}
	@Override
	protected SuDecimal divDec(SuDecimal x) {
		return new SuDecimal(x.n.divide(n, mc));
	}
	@Override
	protected SuDecimal divInt(SuInteger x) {
		return new SuDecimal(BigDecimal.valueOf(x.n).divide(n, mc));
	}

	@Override
	public SuNumber mod(SuValue that) {
		return that.modDec(this);
	}
	@Override
	protected SuDecimal modDec(SuDecimal x) {
		return new SuDecimal(x.n.remainder(n, mc));
	}
	@Override
	protected SuDecimal modInt(SuInteger x) {
		return new SuDecimal(BigDecimal.valueOf(x.n).remainder(n, mc));
	}

	@Override
	public SuDecimal uminus() {
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
