/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuValue;

/**
 * A method bound to an instance.
 */
public class SuBoundMethod extends SuCallable {
	public final SuValue instance;
	public final SuCallable method;

	public SuBoundMethod(SuValue instance, SuCallable method) {
		this.instance = instance;
		this.method = method;
		params = method.params;
		callableType = CallableType.BOUND_METHOD;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		return method.eval(instance, args);
	}

	@Override
	public Object eval(Object self, Object... args) {
		return method.eval(self, args);
	}

	@Override
	public SuValue lookup(String methodName) {
		return this.method.lookup(methodName);
	}

	@Override
	public String display() {
		return method.display();
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof SuBoundMethod))
			return false;
		SuBoundMethod that = (SuBoundMethod) other;
		return instance == that.instance && method.equals(that.method);
	}

	@Override
	public int hashCode() {
		return 31 * System.identityHashCode(instance) + method.hashCode();
	}
}
