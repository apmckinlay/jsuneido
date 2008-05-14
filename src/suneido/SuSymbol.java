package suneido;

import java.util.ArrayList;
import java.util.HashMap;

public class SuSymbol extends SuString {
	static ArrayList<SuSymbol> symbols = new ArrayList<SuSymbol>();
	static HashMap<String, Integer> names = new HashMap<String, Integer>();
	
	final public static int CALL = SuSymbol.symbol("<call>").symnum();
	final public static int DEFAULT = SuSymbol.symbol("Default").symnum();

	public static SuSymbol symbol(String s) {
		return names.containsKey(s)
			? symbols.get(names.get(s))
			: new SuSymbol(s);
	}
	
	private int num;
	private int hash;
	
	private SuSymbol(String s) {
		super(s);
		num = symbols.size();
		hash = s.hashCode();
		symbols.add(this);
	}
	
	public int symnum() {
		return num;
	}
	
	public static SuSymbol symbol(int num) {
		return symbols.get(num);
	}
	
	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object value) {
		if (value instanceof SuSymbol)
			return value == this;
		else
			return super.equals(value);
	}
	
}
