/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuMethod extends SuCallable {

	@Override
	public String typeName() {
		return "Method";
	}

	@Override
	public abstract Object eval(Object self, Object...args);

}
