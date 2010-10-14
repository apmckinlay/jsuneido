package suneido.language;

import suneido.SuValue;

import com.google.common.base.Objects;

/**
 * A method bound to an instance.
 *
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuMethod extends SuValue {
	public final SuValue instance;
	public final SuFunction method;

	public SuMethod(SuValue instance, SuFunction method) {
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
		if (! (other instanceof SuMethod))
			return false;
		SuMethod that = (SuMethod) other;
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
