/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.SuException.methodNotFound;
import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec params;
	protected Object[] constants;
	/** used to do super calls by methods and blocks within methods
	 *  set by {@link SuClass}.linkMethods */
	protected SuClass myClass;

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Params")
			return Params(self, args);
		throw methodNotFound(self, method);
	}

	private Object Params(Object self, Object[] args) {
		return params.params();
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	public Object superInvoke(Object self, String member, Object... args) {
		return myClass.superInvoke(self, member, args);
	}

	@Override
	public String toString() {
		return super.typeName().replace(AstCompile.METHOD_SEPARATOR, '.');
	}

}
