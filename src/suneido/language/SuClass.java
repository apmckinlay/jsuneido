package suneido.language;

import suneido.SuContainer;
import suneido.SuValue;

/**
 * The Java base class for compiled Suneido classes.
 * @see SuMethod
 * @see SuFunction
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuClass extends SuValue {
	protected final SuContainer vars;

	public SuClass() {
		vars = new SuContainer();
	}

	// used by SuFunction
	protected SuClass(boolean noVars) {
		vars = null;
	}

	public void setup(FunctionSpec[] params, Object[][] constants) {
	}

	// classes store "static" data members into vars in initialization block

	@Override
	abstract public String toString();

	abstract public SuClass newInstance();

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "Type")
			return "Class";
		else if (method == "<new>")
			return newInstance();
		else {
			// if we get here, method was not found
			// add method to beginning of args and call Default
			Object newargs[] = new Object[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = method;
			return methodDefault(newargs);
		}
		// TODO other standard methods on classes
	}

	// overridden by classes defining Default
	public Object methodDefault(Object[] args) {
		throw unknown_method((String) args[0]);
	}

	@Override
	public boolean equals(Object other) {
		return this == other; // identity
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
