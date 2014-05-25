/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A {@link Module} that loads using a {@link Loader} and
 * caches the definitions that are loaded.
 *
 * NOTE: not currently used, work in progress
 */
@ThreadSafe
public class ModuleLoader implements Module {
	final String module;
	final Loader loader;
	private final static Object nonexistent = new Object();
	private final LoadingCache<String, Object> cache =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Object>() {
						@Override
						public Object load(String name) {
							Object x = loader.load(module, name);
							return x == null ? nonexistent : x;
						}});


	ModuleLoader(String module, Loader loader) {
		this.module = module;
		this.loader = loader;
	}

	@Override
	public Object get(String name) {
		Object x = cache.getUnchecked(name);
		return x == nonexistent ? null : x;
	}

	void clear(String name) {
		cache.invalidate(name);
	}

	@Override
	public String toString() {
		return module;
	}

}
