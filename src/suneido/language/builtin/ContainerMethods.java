package suneido.language.builtin;

import static suneido.language.Args.Special.NAMED;
import suneido.SuContainer;
import suneido.SuException;
import suneido.language.Args;
import suneido.language.FunctionSpec;

public class ContainerMethods {

	public static Object invoke(SuContainer c, String method, Object... args) {
		if (method == "Size")
			return size(c, args);
		if (method == "Member?")
			return memberQ(c, args);
		if (method == "Add")
			return add(c, args);
		// TODO check user defined Objects
		throw new SuException("unknown method: object." + method);
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");
	private static boolean memberQ(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.containsKey(args[0]);
	}

	private static int size(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return c.size();
	}

	private static Object add(SuContainer c, Object[] args) {
		// TODO handle Add(@args)
		int n = args.length;
		Object at = c.size();
		if (n >= 3 && args[n - 3] == NAMED && args[n - 2] == "at") {
			at = args[n - 1];
			n -= 3;
		}
		if (at instanceof Integer) {
			int at_i = (Integer) at;
			for (int i = 0; i < n; ++i) {
				if (args[i] == NAMED)
					throw new SuException(
							"usage: object.Add(value, ... [ at: position ])");
				else if (0 <= at_i && at_i <= c.vecSize())
					c.insert(at_i++, args[i]);
				else
					c.put(at_i++, args[i]);
			}
		} else if (n == 1)
			c.put(at, args[0]);
		else
			throw new SuException("can only Add multiple values to un-named "
					+ "or to numeric positions");
		return c;
	}
}
