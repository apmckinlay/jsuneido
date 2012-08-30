/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.concurrent.atomic.AtomicInteger;

import suneido.SuException;
import suneido.TheDbms;
import suneido.database.server.Dbms.LibGet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Old style context with a stack of layered libraries
 * <p>
 * Overloading (_Name) has two forms:
 * - class base e.g. Name = _Name { ... } - "previous" value is given unique name
 * - reference in code - "previous" value becomes constant
 */
public class ContextLayered extends Context {
	private static final Object nonExistent = new Object();
	private final LoadingCache<String, Object> loader =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Object>() {
						@Override
						public Object load(String name) {
							return myload(name);
						}});
	private static final AtomicInteger overload = new AtomicInteger();

	public ContextLayered(Contexts contexts) {
		super(contexts);
	}

	@Override
	protected Object fetch(String name) {
		Object value = loader.getUnchecked(name);
		return (value == nonExistent) ? null : value;
	}

	private Object myload(String name) {
		Object x = Builtins.get(name);
		if (x == null)
			x = libget(name);
		return (x == null) ? nonExistent : x;
	}

	private Object libget(String name) {
		if (! TheDbms.isAvailable())
			return null;
		// System.out.println("LOAD " + name);
		Object result = null;
		for (LibGet libget : TheDbms.dbms().libget(name)) {
			String src = (String) Pack.unpack(libget.text);
			try {
				result = Compiler.compile(name, src);
				// needed inside loop for overloading references
				set(name, result);
			} catch (Exception e) {
				throw new SuException("error loading " + name, e);
			}
		}
		return result;
	}

	/***
	 * Called by AstCompile when for classes that inherit from _Name
	 */
	public String overload(String base) {
		assert base.startsWith("_");
		String name = base.substring(1); // remove leading underscore
		Object x = get(name);
		assert x != null;
		int n = overload.getAndIncrement();
		String nameForPreviousValue = n + base;
		set(nameForPreviousValue, x);
		return nameForPreviousValue;
	}

	@Override
	public void clear(String name) {
		loader.invalidate(name);
		super.clear(name);
	};

	@Override
	public void clearAll() {
		loader.invalidateAll();
		super.clearAll();
	}

}
