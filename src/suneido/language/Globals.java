package suneido.language;

import java.util.HashMap;

import suneido.SuException;

/**
 * Stores global names and values.
 * Uses the class itself as a singleton by making everything static.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Globals {
	private static HashMap<String, Object> globals =
			new HashMap<String, Object>();

	private Globals() { // no instances
		throw SuException.unreachable();
	}

	public static int size() {
		return globals.size();
	}

	public static Object get(String name) {
		Object x = globals.get(name);
		if (x == null)
			x = Libraries.load(name);
		if (x == null)
			x = loadClass(name);
		if (x == null)
			throw new SuException("can't find " + name);
		return x;
	}

	public static Object loadClass(String name) {
		Class<?> c = null;
		try {
			c = Class.forName("suneido.language." + name);
		} catch (ClassNotFoundException e) {
			System.out.println(e);
			return null;
		}
		SuClass sc = null;
		try {
			sc = (SuClass) c.newInstance();
		} catch (InstantiationException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
		// System.out.println("<loaded: " + name + ">");
		put(name, sc);
		return sc;
	}

	public static void put(String name, Object x) {
		globals.put(name, x);
	}
}
