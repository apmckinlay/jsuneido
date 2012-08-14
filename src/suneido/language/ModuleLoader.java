/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A {@link Module} that loads using a loader.
 * Caches the definitions that are loaded.
 */
@ThreadSafe
public class ModuleLoader implements Module {
	final Loader loader;
	final String module;
	private final LoadingCache<String, Object> cache =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Object>() {
						@Override
						public Object load(String name) {
							return fetch(name);
						}});


	ModuleLoader(Loader loader, String module) {
		this.loader = loader;
		this.module = module;
	}

	@Override
	public Object get(String name) {
		return cache.getUnchecked(name);
	}

	private Object fetch(String name) {
		String text = loader.load(module, name);
		try {
			return Compiler.compile(name, text);
		} catch (Exception e) {
			throw new SuException("error loading " + name, e);
		}
	}

	@Override
	public String toString() {
		return module;
	}

}
