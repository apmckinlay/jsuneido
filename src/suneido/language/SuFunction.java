/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * A Suneido function compiles to a class that extends SuFunction
 * with the definition in a "call" method
 */
public abstract class SuFunction extends SuCallable {

	@Override
	public String typeName() {
		return "Function";
	}

	/**
	 * compiled Suneido functions define eval so there is a self
	 * in case it is referenced by the code
	 * but calls don't pass self so we need this
	 */
	@Override
	public Object call(Object...args) {
		return eval(this, args);
	}

}
