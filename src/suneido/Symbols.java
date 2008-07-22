package suneido;

import java.util.*;

/**
 * Stores symbol names and instances.
 * Maps names to symbol number (index).
 * Stores instances of private SuSymbol class.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Symbols {
	private static ArrayList<SuSymbol> symbols = new ArrayList<SuSymbol>();
	private static HashMap<String, Integer> names= new HashMap<String, Integer>();

	public static class Sym {
		final public static SuSymbol CALL = symbol("<call>");
		final public static SuSymbol DEFAULT = symbol("Default");
		final public static SuSymbol EACH = symbol("<each>");
		final public static SuSymbol EACH1 = symbol("<each1>");
		final public static SuSymbol NAMED = symbol("<named>");
	}
	public static class Num {
		final public static int CALL = 0;
		final public static int DEFAULT = 1;
		final public static int EACH = 2;
		final public static int EACH1 = 3;
		final public static int NAMED = 4;
		final public static int SUBSTR = 5;
		final public static int I = 6;
		final public static int N = 7;
		final public static int SIZE = 8;
		final public static int CALL_INSTANCE = 9;
		final public static int CALL_CLASS = 10;
		final public static int INSTANTIATE = 11;
		final public static int NEW = 12;
	}

	static {
		for (String s : new String[] {
				"<call>", "Default", "<each>", "<each1>", "<named>",
				"Substr", "i", "n", "Size",
				"<call_instance>", "<call_class>", "<instantiate>", "New" })
			symbol(s);
		assert symbol(Num.NEW).symnum() == Num.NEW;
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

	public static class SuSymbol extends SuString {
		private final int num;

		private SuSymbol(String s, int num) {
			super(s);
			this.num = num;
		}

		@Override
		public boolean equals(Object value) {
			if (value instanceof SuSymbol)
				return this == value;
			else
				return super.equals(value);
		}

		public int symnum() {
			return num;
		}

		/**
		 * symbol(value, ...) is treated as value.symbol(...)
		 */
		@Override
		public SuValue invoke(SuValue self, int method, SuValue ... args) {
			if (method == Num.CALL) {
				method = num;
				self = args[0];
				SuValue[] newargs = Arrays.copyOfRange(args, 1, args.length);
				return self.invoke(self, method, newargs);
			} else
				return super.invoke(self, method, args);
		}
	}
}
