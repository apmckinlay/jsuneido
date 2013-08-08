package suneido.language;

import static suneido.language.Args.Special.EACH;
import static suneido.language.Args.Special.EACH1;
import static suneido.language.Args.Special.NAMED;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuContainer;
import suneido.SuException;

/**
 * Implements Suneido's argument handling.
 * The function parameters are specified by {@link FunctionSpec}
 * @see ArgsIterator
 */
@ThreadSafe
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
	public static Object[] massage(FunctionSpec fs, Object[] args) {
		final int nlocals = fs.nLocals;
		final boolean args_each =
				args.length == 2 && (args[0] == EACH || args[0] == EACH1);

		if (simple(args) && !fs.atParam && args.length == fs.params.length
				&& nlocals <= args.length)
			// "fast" path - avoid alloc by using args as locals
			return args;

		Object[] locals = new Object[nlocals];

		if (fs.atParam && args_each) {
			// function (@params) (@args)
			SuContainer c = Ops.toContainer(args[1]);
			if (c == null)
				throw new SuException("@args requires object");
			locals[0] = c.slice(args[0] == EACH ? 0 : 1);
		} else if (fs.atParam) {
			// function (@params)
			SuContainer c = new SuContainer();
			locals[0] = c;
			collectArgs(c, args);
		} else {
			assert nlocals >= fs.params.length;
			int li = 0;
			for (int i = 0; i < args.length; ++i) {
				if (args[i] == NAMED) {
					for (int j = 0; j < fs.params.length; ++j)
						if (fs.params[j].equals(args[i + 1]))
							locals[j] = args[i + 2];
					// else ignore named arg not matching param
					i += 2;
				}
				else if (args[i] == EACH || args[i] == EACH1) {
					int start = args[i] == EACH ? 0 : 1;
					SuContainer c = Ops.toContainer(args[++i]);
					if (c.vecSize() - start > nlocals - li)
						throw new SuException("too many arguments");
					for (int j = start; j < c.vecSize(); ++j)
						locals[li++] = c.vecGet(j);
					for (int j = 0; j < fs.params.length; ++j) {
						Object x = c.getIfPresent(fs.params[j]);
						if (x != null)
							locals[j] = x;
					}
				}
				else if (li < fs.params.length)
					locals[li++] = args[i];
				else
					throw new SuException("too many arguments");
			}
		}

		dynamicImplicits(fs, locals);

		applyDefaults(fs, locals);

		verifyAllSupplied(fs, locals);

		return locals;
	}

	public static SuContainer collectArgs(SuContainer c, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			if (args[i] == NAMED) {
				c.preset(args[i + 1], args[i + 2]);
				i += 2;
			}
			else if (args[i] == EACH)
				c.merge(Ops.toContainer(args[++i]));
			else if (args[i] == EACH1) {
				int extra = c.vecSize();
				c.merge(Ops.toContainer(args[++i]));
				c.delete(extra);
			} else
				c.add(args[i]);
		}
		return c;
	}

	private static boolean simple(Object[] args) {
		for (Object arg : args)
			if (arg == EACH || arg == EACH1 || arg == NAMED)
				return false;
		return true;
	}

	/** also done by {@link SuCallable} fillin */
	private static void dynamicImplicits(FunctionSpec fs, Object[] locals) {
		for (int i = 0; i < fs.params.length; ++i)
			if (fs.isDynParam(fs.params[i]) && locals[i] == null)
				locals[i] = Dynamic.getOrNull("_" + fs.params[i]);
	}

	/** also done by {@link SuCallable} fillin */
	private static void applyDefaults(FunctionSpec fn, Object[] locals) {
		if (fn.defaults.length == 0)
			return;
		for (int i = 0, j = fn.params.length - fn.defaults.length; i < fn.defaults.length; ++i, ++j)
			if (locals[j] == null)
				locals[j] = fn.defaults[i];
	}

	private static void verifyAllSupplied(FunctionSpec fn, Object[] locals) {
		for (int i = 0; i < fn.params.length; ++i)
			if (locals[i] == null)
				throw new SuException("missing argument: " + fn.params[i]);
	}

}
