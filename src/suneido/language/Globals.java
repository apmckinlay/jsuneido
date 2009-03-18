package suneido.language;

import java.util.HashMap;

import suneido.SuException;
import suneido.SuValue;

/**
 * Stores global names and values.
 * Uses the class itself as a singleton by making everything static.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Globals {
	private static HashMap<String, SuValue> globals =
			new HashMap<String, SuValue>();

	private Globals() { // no instances
		throw SuException.unreachable();
	}

	public static int size() {
		return globals.size();
	}

	public static SuValue get(String name) {
		return globals.get(name);
	}

	public static void put(String name, SuValue x) {
		globals.put(name, x);
	}
}
