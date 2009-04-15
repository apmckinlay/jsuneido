package suneido.language;

import suneido.SuContainer;

/**
 * The Java base class for compiled Suneido classes.
 * @see SuMethod
 * @see SuFunction
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuClass extends SuCallable {
	protected final SuContainer vars;

	public SuClass() {
		vars = new SuContainer();
	}

	// classes store "static" data members into vars in initialization block

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Type")
			return "Class";
		else if (method == "<new>")
			return new SuInstance(this);
		else {
			// if we get here, method was not found
			// add method to beginning of args and call methodDefault
			Object newargs[] = new Object[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = method;
			return methodDefault(newargs);
		}
		// TODO other standard methods on classes
	}

	// TODO default for call class is new instance

	// overridden by classes defining Default
	public Object methodDefault(Object[] args) {
		throw unknown_method((String) args[0]);
	}

}
