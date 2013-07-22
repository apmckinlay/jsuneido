/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.List;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

/**
 * Keeps track of contexts and modules.
 */
public class Contexts {
	static final Loader loader = new LoaderLibrary();
	private final List<Context> contexts = Lists.newArrayList();
	private final LoadingCache<String, Module> modules =
			CacheBuilder.newBuilder().build(
					new CacheLoader<String, Module>() {
						@Override
						public Module load(String name) {
							return new ModuleLoader(name, loader);
						}});

	void addContext(Context context) {
		contexts.add(context);
	}

	Object fetchExplicit(String name) {
		int i = name.indexOf('@');
		String modname = name.substring(i + 1);
		name = name.substring(0,  i);
		return modules.getUnchecked(modname).get(name);
	}

}
