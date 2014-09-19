/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * The {@link Module} for the builtins.
 * Doesn't need to cache.
 *
 * NOTE: not currently used, work in progress
 */
class ModuleBuiltins implements Module {

	@Override
	public Object get(String name) {
		return Builtins.get(name);
	}

	@Override
	public String toString() {
		return "builtins";
	}

}
