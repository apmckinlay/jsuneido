/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.concurrent.atomic.AtomicInteger;

import suneido.SuException;
import suneido.SuValue;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Stores global names and values.
 * Uses the class itself as a singleton by making everything static.
 */
public class Globals {
	private final static LoadingCache<String, Object> globals =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Object>() {
						@Override
						public Object load(String name) {
							return fetch(name);
						}});
	private static final AtomicInteger overload = new AtomicInteger();

	private Globals() { // no instances
		throw SuException.unreachable();
	}

	/** @return The value of name. Throws exception if not found. */
	public static Object get(String name) {
		Object x = tryget(name);
		if (x == null)
			throw new SuException("can't find " + name);
		return x;
	}

	private static final Object nonExistent = new Object();

	// does NOT prevent two threads concurrently getting same name but this
	// shouldn't matter since it's idempotent i.e. result should be the same no
	// matter which thread "wins"
	/** @return The value of name or null if not found */
	public static Object tryget(String name) {
		Object x = globals.getUnchecked(name);
		return x == nonExistent ? null : x;
	}

	private static Object fetch(String name) {
		Object x = Builtins.get(name);
		if (x == null)
			x = Library.load(name);
		return (x == null) ? nonExistent : x;
	}

	/** used by Library load and tests */
	public static void put(String name, Object x) {
		globals.put(name, x);
	}

	public static void unload(String name) {
		globals.invalidate(name);
	}

	/** for Use/Unuse */
	public static void clear() {
		globals.invalidateAll();
	}

	public synchronized static String overload(String base) {
		String name = base.substring(1); // remove leading underscore
		Object x = get(name);
		int n = overload.getAndIncrement();
		String nameForPreviousValue = n + base;
		globals.put(nameForPreviousValue, x);
		return nameForPreviousValue;
	}

	/** used by generated code to call globals
	 *  NOTE: does NOT handle calling a string in a global
	 *  requires globals to be SuValue
	 */
	public static Object invoke(String name, Object... args) {
		return ((SuValue) get(name)).call(args);
	}
	public static Object invoke0(String name) {
		return ((SuValue) get(name)).call0();
	}
	public static Object invoke1(String name, Object a) {
		return ((SuValue) get(name)).call1(a);
	}
	public static Object invoke2(String name, Object a, Object b) {
		return ((SuValue) get(name)).call2(a, b);
	}
	public static Object invoke3(String name, Object a, Object b, Object c) {
		return ((SuValue) get(name)).call3(a, b, c);
	}
	public static Object invoke4(String name, Object a, Object b,
			Object c, Object d) {
		return ((SuValue) get(name)).call4(a, b, c, d);
	}

}
