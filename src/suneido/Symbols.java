package suneido;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Stores symbol names and instances.
 * Maps names to symbol number (index).
 * @author Andrew McKinlay
 */
public class Symbols {
	private static ArrayList<SuSymbol> symbols = new ArrayList<SuSymbol>();
	private static HashMap<String, Integer> names= new HashMap<String, Integer>();
	
	final public static SuSymbol CALL = symbol("<call>");
	final public static SuSymbol DEFAULT = symbol("Default");
	final public static SuSymbol EACH = symbol("<each>");
	final public static SuSymbol EACH1 = symbol("<each1>");
	final public static SuSymbol NAMED = symbol("<named>");
	final public static int CALLi = 0;
	final public static int DEFAULTi = 1;
	final public static int EACHi = 2;
	final public static int EACH1i = 3;
	final public static int NAMEDi = 4;
	final public static int SUBSTR = 5;
	final public static int I = 6;
	final public static int N = 7;
	final public static int SIZE = 8;
	final public static int CALL_INSTANCE = 9;
	final public static int CALL_CLASS = 10;
	
	static {
		for (String s : new String[] { "Substr", "i", "n", "Size", "<call_instance>", "<call_class>" }) 
			symbol(s);
	}
	
	public static SuSymbol symbol(String s) {
		if (names.containsKey(s))
			return symbols.get(names.get(s));
		int num = symbols.size();
		SuSymbol symbol = new SuSymbol(s, num);
		names.put(s, num);
		symbols.add(symbol);
		return symbol;
	}
	public static int symnum(String s) {
		return symbol(s).symnum();
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
		
		/**
		 * symbol(value, ...) is treated as value.symbol(...)
		 */
		@Override
		public SuValue invoke(SuValue self, int method, SuValue ... args) {
			if (method == Symbols.CALLi) {
				method = num;
				self = args[0];
				SuValue[] newargs = Arrays.copyOfRange(args, 1, args.length);
				return self.invoke(self, method, newargs);
			} else
				return super.invoke(self, method, args);
		}
	}	
}
