/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuValue;

import com.google.common.base.Objects;

/**
 * A method bound to an instance.
 */
public class SuBoundMethod extends SuValue {
	public final SuValue instance;
	public final SuCallable method;

	public SuBoundMethod(SuValue instance, SuCallable method) {
		this.instance = instance;
		this.method = method;
	}

	@Override
	public Object call(Object... args) {
		return method.eval(instance, args);
	}

	@Override
	public Object eval(Object self, Object... args) {
		return method.eval(self, args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		return this.method.invoke(self, method, args);
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof SuBoundMethod))
			return false;
		SuBoundMethod that = (SuBoundMethod) other;
		return Objects.equal(method, that.method) &&
				Objects.equal(instance, that.instance);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(instance, method);
	}

	@Override
	public String typeName() {
		return "Method";
	}

}
