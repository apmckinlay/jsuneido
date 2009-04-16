package suneido.language;

import java.util.Map;

import suneido.SuException;
import suneido.SuValue;

/**
 * The Java base class for compiled Suneido classes.
 * @see SuMethod
 * @see SuFunction
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuClass extends SuCallable {
	protected String baseGlobal = null;
	protected Map<String, Object> vars;

	@Override
	public Object get(Object member) {
		Object value = vars.get(member);
		if (value == null && baseGlobal != null) {
			Object base = Globals.get(baseGlobal);
			if (!(base instanceof SuClass))
				throw new SuException("base must be class");
			return ((SuClass) base).get(member);
		}
		return value;
	}

	@Override
	public Object call(Object... args) {
		return invoke(this, "CallClass", args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Type")
			return "Class";
		// TODO other standard methods on classes
		else if (method == "<new>" || method == "CallClass")
			return newInstance(args);
		else if (method == "_init")
			return init(args);
		else if (baseGlobal != null) {
			Object base = Globals.get(baseGlobal);
			if (!(base instanceof SuValue))
				throw new SuException("class base must be a Suneido value");
			return ((SuValue) base).invoke(self, method, args);
		} else if (method != "Default") {
			// if we get here, method was not found
			// add method to beginning of args and call Default
			Object newargs[] = new Object[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = method;
			return Ops.invoke(self, "Default", newargs);
			// COULD make a defaultMethod and bypass invoke (like "call")
		} else
			// method == "Default"
			throw unknown_method((String) args[0]);
	}

	private Object newInstance(Object[] args) {
		SuInstance x = new SuInstance(this);
		x.invoke(x, "_init", args);
		return x;
	}

	private static Object init(Object[] args) {
		// TODO massage args (shouldn't be any)
		return null;
	}

}
