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
public abstract class SuClass extends SuValue {
	protected final SuContainer vars;

	SuClass() {
		vars = new SuContainer();
	}

	// used by SuFunction
	SuClass(boolean noVars) {
		vars = null;
	}

	public void setup(FunctionSpec[] params, SuValue[][] constants) {
	}

	// classes store "static" data members into vars in initialization block

	@Override
	abstract public String toString();

	// new x is compiled as x.newInstance(...)
	@Override
	abstract public SuClass newInstance(SuValue... args);

	public static boolean bool(SuValue value) {
		if (value == SuBoolean.TRUE)
			return true;
		else if (value == SuBoolean.FALSE)
			return false;
		else
			throw new SuException("expected true or false, got: "
					+ value.typeName());
	}

	@Override
	public SuValue invoke(String method, SuValue ... args) {
		if (method == "Type")
			return SuString.valueOf("Class");
		// TODO other standard methods on classes
		else {
			// if we get here, method was not found
			// add method to beginning of args and call Default
			SuValue newargs[] = new SuValue[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = SuString.valueOf(method);
			return methodDefault(newargs);
		}
	}

	// overridden by classes defining Default
	public SuValue methodDefault(SuValue[] args) {
		throw unknown_method(args[0].strIfStr());
	}

	/**
	 * Implements Suneido's argument handling.
	 *
	 * @param args		The arguments as an SuValue array.<br />
	 * 					fn(... @args ...) => ... EACH, args ...<br />
	 * 					fn(... name: arg ...) => ... NAMED, name, arg ...<br />
	 * 					Unlike cSuneido, multiple EACH's are allowed.
	 * @return	The locals SuValue array initialized from args.
	 */
	public static SuValue[] massage(FunctionSpec fn, SuValue[] args) {
		final int nlocals = fn.locals.length;
		final boolean args_each =
				args.length == 2 && (args[0] == EACH || args[0] == EACH1);

		if (simple(args) && !fn.atParam && args.length == fn.nparams
				&& nlocals <= args.length)
			// "fast" path - avoid alloc by using args as locals
			return args;

		SuValue[] locals = new SuValue[nlocals];

		if (fn.atParam && args_each) {
			// function (@params) (@args)
			locals[0] = args[1].container().slice(args[0] == EACH ? 0 : 1);
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
						if (fn.locals[j].equals(args[i + 1].strIfStr()))
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
						SuValue x = c.get(fn.locals[j]);
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

	public static SuContainer collectArgs(SuValue[] args, SuContainer c) {
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

	private static boolean simple(SuValue[] args) {
		for (SuValue arg : args)
			if (arg == EACH || arg == NAMED)
				return false;
		return true;
	}

	private static void applyDefaults(FunctionSpec fn, SuValue[] locals) {
		if (fn.ndefaults == 0)
			return;
		for (int i = 0, j = fn.nparams - fn.ndefaults; i < fn.ndefaults; ++i, ++j)
			if (locals[j] == null)
				locals[j] = fn.constants[i];
	}

	private static void verifyAllSupplied(FunctionSpec fn, SuValue[] locals) {
		for (int i = 0; i < fn.nparams; ++i)
			if (locals[i] == null)
				throw new SuException("missing argument: " + fn.locals[i]);
	}

	public static final SuString EACH = SuString.makeUnique("<each>");
	public static final SuString EACH1 = SuString.makeUnique("<each1>");
	public static final SuString NAMED = SuString.makeUnique("<named>");

	@Override
	public boolean equals(Object other) {
		return this == other; // identity
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
