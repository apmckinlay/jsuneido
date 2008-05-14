package suneido;

public abstract class SuValue {
	public SuValue getdata(SuValue member) {
		throw new SuException(typeName() + " does not support get");
	}
	
	public void putdata(SuValue member, SuValue value) {
		throw new SuException(typeName() + " does not support put");
	}
	public String typeName() {
		return getClass().getName().substring(10); // strip Suneido.Su
	}
	
	public boolean is_numeric() {
		return false;
	}
	public int integer() {
		throw new SuException("can't convert " + typeName() + " to integer");
	}
	public SuNumber number() {
		throw new SuException("can't convert " + typeName() + " to number");
	}
	public abstract String toString();
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
		return Integer.signum(order() - i.order());
	}
	public int compareToDate(SuDate d) {
		return Integer.signum(order() - d.order());
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
	protected SuValue addNum(SuNumber x) {
		return number().addNum(x);
	}
	
	public SuValue sub(SuValue x) {
		return x.number().subNum(number());
	}
	protected SuValue subInt(SuInteger x) {
		return number().subInt(x);
	}
	protected SuValue subNum(SuNumber x) {
		return number().subNum(x);
	}
	
	public SuValue mul(SuValue x) {
		return mulNum(x.number());
	}
	protected SuValue mulInt(SuInteger x) {
		return number().mulInt(x);
	}
	protected SuValue mulNum(SuNumber x) {
		return number().mulNum(x);
	}
	
	public SuValue div(SuValue x) {
		return x.number().divNum(number());
	}
	protected SuValue divInt(SuInteger x) {
		return number().divInt(x);
	}
	protected SuValue divNum(SuNumber x) {
		return number().divNum(x);
	}
	
	public SuValue uminus() {
		return number().uminus();
	}
	
	public SuValue invoke(int method, SuValue ... args) {
		throw method == SuSymbol.CALL
			? new SuException("can't call " + typeName())
			: unknown_method(method);
	}
	public SuException unknown_method(int method) {
		return new SuException("unknown method " + typeName() + SuSymbol.symbol(method));
	}
}
