package suneido.language;

import suneido.*;

/**
 * The Java base class for compiled Suneido classes.
 * The Java class hierarchy is "flat".
 * All compiled Suneido classes derive directly from SuClass.
 * Suneido inheritance is handled by invoke.
 * A Suneido class with "no" parent calls super.invoke from its invoke's default
 * else it calls Globals.get(parent).invoke2
 * @see SuMethod
 * @see SuInstance
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
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

	// classes store "static" data members into vars in initialization block

	@Override
	abstract public String toString();

	// new x is compiled as x.newInstance(...)
	@Override
	abstract public SuClass newInstance(SuValue... args);

	@Override
	public SuValue invoke(SuValue... args) {
		// default for calling a class is to instantiate
		// overridden by class defining CallClass
		return newInstance(args);
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

	// overridden by class defining Default
	public SuValue methodDefault(SuValue[] args) {
		throw unknown_method(args[0].strIfStr());
	}

	/**
	 * Implements Suneido's argument handling.
	 * Called at the start of generated sub-class methods.
	 *
	 * @param nlocals	The number of local variables required, including parameters.
	 * @param args		The arguments as an SuValue array.<br />
	 * 					fn(... @args ...) => ... EACH, args ...<br />
	 * 					fn(... name: arg ...) => ... NAMED, name, arg ...<br />
	 * 					Unlike cSuneido, multiple EACH's are allowed.
	 * @param params	A variable number of parameter names as symbol indexes.<br />
	 * 					function (@args) => EACH, args<br />
	 * 					No other params are allowed with EACH.
	 * @return	The locals SuValue array initialized from args.
	 */
	public static SuValue[] massage(int nlocals, final SuValue[] args,
			final String... params) {
		boolean params_each = params.length > 0 && params[0] == "<each>";

		if (simple(args) && !params_each) {
			if (args.length != params.length)
				throw new SuException("wrong number of arguments");

			// "fast" path - when possible, avoid alloc and just return args
			if (nlocals <= args.length) {
				return args;
			}
		}

		// "slow" path - alloc and copy into locals
		SuValue[] locals = new SuValue[nlocals];
		if (args.length == 0)
			return locals;
		if (params_each) {
			// function (@params)
			if (args[0] == EACH && args.length == 2)
				// optimize function (@params) (@args)
				locals[0] = new SuContainer((SuContainer) args[1]);
			else {
				SuContainer c = new SuContainer();
				locals[0] = c;
				for (int i = 0; i < args.length; ++i) {
					if (args[i] == NAMED) {
						c.putdata(args[i + 1], args[i + 2]);
						i += 2;
					}
					else if (args[i] == EACH)
						c.merge((SuContainer) args[++i]);
					else
						c.append(args[i]);
				}
			}
		} else {
			assert nlocals >= params.length;
			int li = 0;
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == NAMED) {
					for (int j = 0; j < params.length; ++j)
						if (params[j].equals(args[i + 1].strIfStr()))
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
					for (int j = 0; j < params.length; ++j) {
						SuValue x = c.getdata(params[j]);
						if (x != null)
							locals[j] = x;
					}
				}
				else
					locals[li++] = args[i];
			}
		}
		return locals;
	}
	private static boolean simple(SuValue[] args) {
		for (SuValue arg : args)
			if (arg == EACH || arg == NAMED)
				return false;
		return true;
	}

	public static SuValue[] massage(final SuValue[] args,
			final String... params) {
		return massage(params.length, args, params);
	}

	public static final SuString EACH = SuString.valueOf("<each>");
	public static final SuString EACH1 = SuString.valueOf("<each1>");
	public static final SuString NAMED = SuString.valueOf("<named>");

	//TODO handle @+# args, maybe just add EACH1 since we only ever use @+1
	//TODO parameters with default values
	//TODO check for missing arguments
}
