package suneido.language;

import suneido.*;

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
	protected final SuContainer vars;

	public SuClass() {
		vars = new SuContainer();
	}

	// classes store "static" data members into vars in initialization block

	@Override
	public Object call(Object... args) {
		// default for calling class is to create instance
		return newInstance(args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Type")
			return "Class";
		// TODO other standard methods on classes
		else if (method == "<new>")
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
