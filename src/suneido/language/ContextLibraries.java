/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * Old style context with a stack of layered libraries
 */
class ContextLibraries extends Context {
	static final Context context = new ContextLibraries();

	@Override
	protected Object fetch(String name) {
		return Globals.tryget(name);
	}

}
