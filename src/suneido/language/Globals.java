package suneido.language;

import java.util.ArrayList;
import java.util.HashMap;

import suneido.SuException;
import suneido.SuValue;
import static suneido.Suneido.verify;

/**
 * Stores global names and values.
 * Similar to SuSymbols but simpler because there is no extra type.
 * Uses the class itelf as a singleton by making everything static.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Globals {
	private static HashMap<String,Integer> names = new HashMap<String,Integer>();
	private static ArrayList<SuValue> globals = new ArrayList<SuValue>();
	
	private Globals() { // no instances
		throw SuException.unreachable();
	}
	
	public static int num(String name) {
		if (names.containsKey(name))
			return names.get(name);
		int i = globals.size();
		names.put(name, i);
		globals.add(null);
		verify(names.size() == globals.size());
		return i;
	}
	
	public static int size() {
		return globals.size();
	}
	
	public static SuValue get(int i) {
		return globals.get(i);
	}
	
	public static void set(int i, SuValue x) {
		globals.set(i, x);
	}
}
