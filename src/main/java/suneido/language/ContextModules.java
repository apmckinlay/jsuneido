/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import com.google.common.collect.ImmutableList;

/**
 * A {@link Context} for {@link Module}'s
 *
 * NOTE: not currently used, work in progress
 */
public class ContextModules extends Context {
	private final ImmutableList<Module> modules;

	ContextModules(Contexts contexts, ImmutableList<Module> modules) {
		super(contexts);
		this.modules = modules;
	}

	/**
	 * Look in each module for the value of name.
	 * Throw exception if name is found in more than one module.
	 * @return The value of the name or null if not found.
	 */
	@Override
	protected Object fetch(String name) {
		Module where = null;
		Object value = null;
		for (Module m : modules) {
			Object x = m.get(name);
			if (value == null) {
				where = m;
				value = x;
			} else
				throw new RuntimeException("Ambiguous reference to " + name +
						" (found in " + m + " and " + where + ")");
		}
		return value;
	}

}
