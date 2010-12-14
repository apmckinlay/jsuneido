/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

/**
 * Standalone functions derive from SuFunction and define eval<p>
 * Functions that reference "this" derive from {@link SuMethod} and define eval<p>
 * For simple args {@link SuFunction0} ... {@link SuFunction9} are used
 */
public abstract class SuFunction extends SuCallable {

	@Override
	public String typeName() {
		return getClass().getName().startsWith("suneido.language.builtin")
			? "Builtin"
			: isBlock ? "Block" : "Function";
	}

	@Override
	public abstract Object call(Object... args);

	@Override
	public Object eval(Object self, Object... args) {
		return call(args);
	}

}
