package suneido.language;

import suneido.SuValue;

/**
 * SuMethod makes methods first class values.
 * It binds the method and the instance it "came from".
 * Also used for nested anonymous functions.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuMethod extends SuValue {
	/** not private final because instance is filled in later
	 *  @see CompileGenerator.linkConstants */
	public Object instance;
	private final String method;

	public SuMethod(Object instance, String method) {
		this.instance = instance;
		this.method = method;
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call")
			return Ops.invoke(instance, this.method, args);
		else
			throw unknown_method(method);
	}

	@Override
	public String toString() {
		return (instance == null ? "null" : instance.toString()) + "." + method;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof SuMethod) {
			SuMethod that = (SuMethod) other;
			return instance.equals(that.instance) && method.equals(that.method);
		}
		return false;
	}

	/** as recommended by Effective Java */
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + instance.hashCode();
		result = 31 * result + method.hashCode();
		return result;
	}

}
