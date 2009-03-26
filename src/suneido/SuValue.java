package suneido;

import java.nio.ByteBuffer;

/**
 * Base class for Suneido data types.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public abstract class SuValue implements Packable, Comparable<SuValue> {
	@Override
	public abstract String toString();

	public String strIfStr() {
		return null;
	}

	public String string() {
		throw new SuException(typeName() + " cannot be converted to string");
	}

	public int hashCode(int nest) {
		return hashCode();
	}

	// sequence must match Order
	public static class Pack {
		public static final byte FALSE = 0;
		public static final byte TRUE = 1;
		public static final byte MINUS = 2;
		public static final byte PLUS = 3;
		public static final byte STRING = 4;
		public static final byte DATE = 5;
		public static final byte OBJECT = 6;
		public static final byte RECORD = 7;
		public static final byte FUNCTION = 8;
		public static final byte CLASS = 9;
	}

	public ByteBuffer pack() {
		ByteBuffer buf = ByteBuffer.allocate(packSize());
		pack(buf);
		buf.rewind();
		return buf;
	}

	public int packSize() {
		return packSize(0);
	}
	public int packSize(int nest) {
		throw new SuException(typeName() + " cannot be stored");
	}
	public void pack(ByteBuffer buf) {
		throw new SuException(typeName() + " cannot be stored");
	}
	public static SuValue unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return SuString.EMPTY;
		switch (buf.get()) {
		case Pack.FALSE :
			return SuBoolean.FALSE;
		case Pack.TRUE :
			return SuBoolean.TRUE;
		case Pack.MINUS :
		case Pack.PLUS :
			return SuNumber.unpack1(buf);
		case Pack.STRING :
			return SuString.unpack1(buf);
		case Pack.DATE:
			return SuDate.unpack1(buf);
		case Pack.OBJECT:
			return SuContainer.unpack1(buf);
		// TODO unpack other types
		default :
			throw SuException.unreachable();
		}
	}
	public static String toString(ByteBuffer buf) {
		int pos = buf.position();
		SuValue x = unpack(buf);
		buf.position(pos);
		return x.toString();
	}

	public SuValue get(String member) {
		return get(SuString.valueOf(member));
	}
	public SuValue get(SuValue member) {
		throw new SuException(typeName() + " does not support get");
	}

	public void put(String member, SuValue value) {
		put(SuString.valueOf(member), value);
	}
	public void put(SuValue member, SuValue value) {
		throw new SuException(typeName() + " does not support put");
	}

	public String typeName() {
		return getClass().getName().substring(10); // strip Suneido.Su
	}

	/**
	 * Used by SuContainer.
	 * @return value if integer, -1 otherwise
	 */
	public int index() {
		return -1;
	}
	public int integer() {
		throw new SuException("can't convert " + typeName() + " to integer");
	}
	public SuNumber number() {
		throw new SuException("can't convert " + typeName() + " to number");
	}
	public SuContainer container() {
		throw new SuException("can't convert " + typeName() + " to object");
	}

	/**
	 * This is a default implementation,
	 * it should be overridden if there is a "natural" ordering, e.g. numbers, strings, dates
	 * Orders first by order(), then by hashCode.
	 * <p>WARNING: will return 0 for different objects if they happen to have the same hashCode.
	 * @param value
	 * @return 0, -1, or +1
	 */
	public int compareTo(SuValue value) {
		if (this == value)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		ord = hashCode() - value.hashCode(); // default ordering
		return ord < 0 ? -1 : ord > 0 ? +1 : 0;
	}
	protected int compareToInt(SuInteger x) {
		throw SuException.unreachable();
	}

	protected int compareToDec(SuDecimal x) {
		throw SuException.unreachable();
	}

	protected enum Order { BOOLEAN, NUMBER, STRING, DATE, CONTAINER, OTHER };
	public int order() {
		return Order.OTHER.ordinal();
	}

	// handle conversion of left hand side (this)
	public SuNumber add(SuValue x) {
		return number().add(x);
	}
	public SuNumber sub(SuValue x) {
		return number().sub(x);
	}
	public SuNumber mul(SuValue x) {
		return number().mul(x);
	}
	public SuNumber div(SuValue x) {
		return number().div(x);
	}
	public SuNumber mod(SuValue x) {
		return number().mod(x);
	}

	// handle conversion of other side
	protected SuNumber addInt(SuInteger x) {
		return number().addInt(x);
	}
	protected SuNumber subInt(SuInteger x) {
		return number().subInt(x);
	}
	protected SuNumber mulInt(SuInteger x) {
		return number().mulInt(x);
	}
	protected SuNumber divInt(SuInteger x) {
		return number().divInt(x);
	}
	protected SuNumber modInt(SuInteger x) {
		return number().modInt(x);
	}

	protected SuNumber addDec(SuDecimal x) {
		return number().addDec(x);
	}
	protected SuNumber subDec(SuDecimal x) {
		return number().subDec(x);
	}
	protected SuNumber mulDec(SuDecimal x) {
		return number().mulDec(x);
	}
	protected SuNumber divDec(SuDecimal x) {
		return number().divDec(x);
	}
	protected SuNumber modDec(SuDecimal x) {
		return number().modDec(x);
	}

	public SuNumber uminus() {
		throw new SuException("can't convert " + typeName() + " to number");
	}

	public final SuString cat(SuValue other) {
		return SuString.valueOf(string() + other.string());
	}

	public final SuValue add1() {
		return addInt(SuInteger.ONE);
	}

	public final SuValue sub1() {
		return subInt(SuInteger.ONE);
	}

	public SuValue newInstance(SuValue... args) {
		throw new SuException("can't do new " + typeName());
	}
	public SuValue invoke(SuValue... args) {
		throw new SuException("can't call " + typeName());
	}
	public SuValue invoke(String method, SuValue ... args) {
		throw unknown_method(method);
	}

	public SuException unknown_method(String method) {
		return new SuException("unknown method " + typeName() + "." + method);
	}
}
