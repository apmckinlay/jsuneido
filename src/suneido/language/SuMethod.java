package suneido.language;

import suneido.SuValue;

/**
 * SuMethod makes methods first class values.
 * It binds the method and the instance it "came from".
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuMethod extends SuValue {
	public SuValue instance;
	private final String method;

	public SuMethod(SuValue instance, String method) {
		this.instance = instance;
		this.method = method;
	}

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return instance.invoke(this.method, args);
		else
			throw unknown_method(method);
	}

	@Override
	public String toString() {
		return instance.toString() + "." + method;
	}

}
