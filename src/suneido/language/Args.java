package suneido.language;

import static suneido.language.Args.Special.*;
import suneido.SuContainer;
import suneido.SuException;

/**
 * Implements Suneido's argument handling.
 */
public class Args {
	public static enum Special {
		EACH, EACH1, NAMED
	}

	/**
	 * Convert args from caller's version to what the method expects.
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

}
