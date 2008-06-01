package suneido;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores symbol names and instances.
 * Maps names to symbol number (index).
 * @author Andrew McKinlay
 */
public class Symbols {
	private static ArrayList<SuSymbol> symbols = new ArrayList<SuSymbol>();
	private static HashMap<String, Integer> names = new HashMap<String, Integer>();
	
	final public static SuSymbol CALL = Symbols.symbol("<call>");
	final public static SuSymbol DEFAULT = Symbols.symbol("Default");
	final public static SuSymbol EACH = Symbols.symbol("<each>");
	final public static SuSymbol NAMED = Symbols.symbol("<named>");
	final public static int CALLi = CALL.symnum();
	final public static int DEFAULTi = DEFAULT.symnum();
	final public static int EACHi = EACH.symnum();
	final public static int NAMEDi = NAMED.symnum();
	
	final public static int SUBSTR = 100;
	final public static int I = 101;
	final public static int N = 102;

	public static SuSymbol symbol(String s) {
		if (names.containsKey(s))
			return symbols.get(names.get(s));
		int num = symbols.size();
		SuSymbol symbol = new SuSymbol(s, num);
		names.put(s, num);
		symbols.add(symbol);
		return symbol;
	}
	
	public static SuSymbol symbol(int num) {
		return symbols.get(num);
	}
		
	static class SuSymbol extends SuString {
		private int num;
		private int hash;
		
		private SuSymbol(String s, int num) {
			super(s);
			this.num = num;
			hash = s.hashCode();
		}
		
		public int symnum() {
			return num;
		}
		
		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object value) {
			if (value == this)
				return true;
			else if (value instanceof Symbols)
				return false;
			else
				return super.equals(value);
		}
	}	
}
