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

	public String string() {
		throw new SuException(typeName() + " cannot be converted to string");
	}

	// sequence must match Order
	static class Pack {
		static final byte FALSE = 0;
		static final byte TRUE = 1;
		static final byte MINUS = 2;
		static final byte PLUS = 3;
		static final byte STRING = 4;
		static final byte DATE = 5;
		static final byte OBJECT = 6;
		static final byte RECORD = 7;
		static final byte FUNCTION = 8;
		static final byte CLASS = 9;
	}

	public ByteBuffer pack() {
		ByteBuffer buf = ByteBuffer.allocate(packSize());
		pack(buf);
		buf.position(0);
		return buf;
	}
	public int packSize() {
		throw new SuException(typeName() + " cannot be stored");
	}
	public void pack(ByteBuffer buf) {
		throw new SuException(typeName() + " cannot be stored");
	}
	public static SuValue unpack(ByteBuffer buf) {
		if (buf.limit() == 0)
			return SuString.EMPTY;
		switch (buf.get(0)) {
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
//		case Pack.CLASS :
//			return SuClass.unpack1(buf);
		default :
			throw SuException.unreachable();
		}
	}

	public SuValue getdata(SuValue member) {
		throw new SuException(typeName() + " does not support get");
	}

	public void putdata(SuValue member, SuValue value) {
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
	public SuDecimal number() {
		throw new SuException("can't convert " + typeName() + " to number");
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
	public int compareToInt(SuInteger i) {
		throw SuException.unreachable();
	}

	protected enum Order { BOOLEAN, NUMBER, STRING, DATE, CONTAINER, OTHER };
	public int order() {
		return Order.OTHER.ordinal();
	}

	public SuValue add(SuValue x) {
		return addNum(x.number());
	}
	protected SuValue addInt(SuInteger x) {
		return number().addInt(x);
	}
	protected SuValue addNum(SuDecimal x) {
		return number().addNum(x);
	}

	public SuValue sub(SuValue x) {
		return x.number().subNum(number());
	}
	protected SuValue subInt(SuInteger x) {
		return number().subInt(x);
	}
	protected SuValue subNum(SuDecimal x) {
		return number().subNum(x);
	}

	public SuValue mul(SuValue x) {
		return mulNum(x.number());
	}
	protected SuValue mulInt(SuInteger x) {
		return number().mulInt(x);
	}
	protected SuValue mulNum(SuDecimal x) {
		return number().mulNum(x);
	}

	public SuValue div(SuValue x) {
		return x.number().divNum(number());
	}
	protected SuValue divInt(SuInteger x) {
		return number().divInt(x);
	}
	protected SuValue divNum(SuDecimal x) {
		return number().divNum(x);
	}

	public SuValue uminus() {
		return number().uminus();
	}

	public SuValue invoke(SuValue self, int method, SuValue ... args) {
		throw method == Symbols.Num.CALL
			? new SuException("can't call " + typeName())
			: unknown_method(method);
	}

	public SuValue invoke(int method, SuValue ... args) {
		return invoke(this, method, args);
	}

	public SuException unknown_method(int method) {
		return unknown_method(Symbols.symbol(method));
	}
	public SuException unknown_method(SuValue method) {
		return new SuException("unknown method " + typeName() + "." + method);
	}
}
