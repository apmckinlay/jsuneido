package suneido;

import java.util.ArrayList;
import java.util.HashMap;

public class SuSymbol extends SuString {
	static ArrayList<SuSymbol> symbols = new ArrayList<SuSymbol>();
	static HashMap<String, Integer> names = new HashMap<String, Integer>();
	
	final public static SuSymbol CALL = SuSymbol.symbol("<call>");
	final public static SuSymbol DEFAULT = SuSymbol.symbol("Default");
	final public static SuSymbol EACH = SuSymbol.symbol("<each>");
	final public static SuSymbol NAMED = SuSymbol.symbol("<named>");
	final public static int CALLi = CALL.symnum();
	final public static int DEFAULTi = DEFAULT.symnum();
	final public static int EACHi = EACH.symnum();
	final public static int NAMEDi = NAMED.symnum();

	public static SuSymbol symbol(String s) {
		if (names.containsKey(s))
			return symbols.get(names.get(s));
		SuSymbol symbol = new SuSymbol(s);
		names.put(s, symbols.size());
		symbols.add(symbol);
		return symbol;
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
		if (value == this)
			return true;
		else if (value instanceof SuSymbol)
			return false;
		else
			return super.equals(value);
	}
	
}
