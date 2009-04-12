package suneido.language;

import static suneido.language.SuClass.SpecialArg.*;
import suneido.*;

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

	public static boolean bool(Object x) {
		if (x == Boolean.TRUE)
			return true;
		else if (x == Boolean.FALSE)
			return false;
		else
			throw new SuException("expected true or false, got: " + x);
	}

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

	/**
	 * Implements Suneido's argument handling.
	 *
	 * @param args
	 *            The arguments as an Object array.<br />
	 *            fn(... @args ...) => ... EACH, args ...<br />
	 *            fn(... name: arg ...) => ... NAMED, name, arg ...<br />
	 *            Unlike cSuneido, multiple EACH's are allowed.
	 * @return The locals Object array initialized from args.
	 */
	public static Object[] massage(FunctionSpec fn, Object[] args) {
		final int nlocals = fn.locals.length;
		final boolean args_each =
				args.length == 2 && (args[0] == EACH || args[0] == EACH1);

		if (simple(args) && !fn.atParam && args.length == fn.nparams
				&& nlocals <= args.length)
			// "fast" path - avoid alloc by using args as locals
			return args;

		Object[] locals = new Object[nlocals];

		if (fn.atParam && args_each) {
			// function (@params) (@args)
			locals[0] = ((SuContainer) args[1]).slice(args[0] == EACH ? 0 : 1);
		} else if (fn.atParam) {
			// function (@params)
			SuContainer c = new SuContainer();
			locals[0] = c;
			collectArgs(args, c);
		} else {
			assert nlocals >= fn.nparams;
			int li = 0;
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == NAMED) {
					for (int j = 0; j < fn.nparams; ++j)
						if (fn.locals[j].equals(args[i + 1]))
							locals[j] = args[i + 2];
					// else ignore named arg not matching param
					i += 2;
				}
				else if (args[i] == EACH || args[i] == EACH1) {
					int start = args[i] == EACH ? 0 : 1;
					SuContainer c = (SuContainer) args[++i];
					if (c.vecSize() - start > nlocals - li)
						throw new SuException("too many arguments");
					for (int j = start; j < c.vecSize(); ++j)
						locals[li++] = c.vecGet(j);
					for (int j = 0; j < fn.nparams; ++j) {
						Object x = c.get(fn.locals[j]);
						if (x != null)
							locals[j] = x;
					}
				}
				else if (li < fn.nparams)
					locals[li++] = args[i];
				else
					throw new SuException("too many arguments");
			}
		}

		applyDefaults(fn, locals);

		verifyAllSupplied(fn, locals);

		return locals;
	}

	public static SuContainer collectArgs(Object[] args, SuContainer c) {
		for (int i = 0; i < args.length; ++i) {
			if (args[i] == NAMED) {
				c.put(args[i + 1], args[i + 2]);
				i += 2;
			}
			else if (args[i] == EACH)
				c.merge((SuContainer) args[++i]);
			else
				c.append(args[i]);
		}
		return c;
	}

	private static boolean simple(Object[] args) {
		for (Object arg : args)
			if (arg == EACH || arg == NAMED)
				return false;
		return true;
	}

	private static void applyDefaults(FunctionSpec fn, Object[] locals) {
		if (fn.ndefaults == 0)
			return;
		for (int i = 0, j = fn.nparams - fn.ndefaults; i < fn.ndefaults; ++i, ++j)
			if (locals[j] == null)
				locals[j] = fn.constants[i];
	}

	private static void verifyAllSupplied(FunctionSpec fn, Object[] locals) {
		for (int i = 0; i < fn.nparams; ++i)
			if (locals[i] == null)
				throw new SuException("missing argument: " + fn.locals[i]);
	}

	public static enum SpecialArg {
		EACH, EACH1, NAMED
	};

	@Override
	public boolean equals(Object other) {
		return this == other; // identity
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
