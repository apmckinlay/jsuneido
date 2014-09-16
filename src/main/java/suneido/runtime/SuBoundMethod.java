/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuValue;

/**
 * A method bound to an instance.
 */
public class SuBoundMethod extends SuWrappingCallable {
	public final SuValue instance;

	public SuBoundMethod(SuValue instance, SuCallable method) {
		super(method);
		this.instance = instance;
		params = method.params;
		callableType = CallableType.BOUND_METHOD;
	}

	//
	// ACCESSORS
	//

	public SuCallable method() {
		return wrapped;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		return wrapped.eval(instance, args);
	}

	@Override
	public Object eval(Object self, Object... args) {
		return wrapped.eval(self, args);
	}

	@Override
	public SuValue lookup(String methodName) {
		return this.wrapped.lookup(methodName);
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
		return instance == that.instance && wrapped.equals(that.wrapped);
	}

	@Override
	public int hashCode() {
		return 31 * System.identityHashCode(instance) + wrapped.hashCode();
	}
}
