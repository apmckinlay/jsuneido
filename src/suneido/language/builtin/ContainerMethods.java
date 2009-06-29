package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Args.Special.NAMED;
import static suneido.language.Ops.toInt;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.Collections;
import java.util.List;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuContainer.IterResult;
import suneido.SuContainer.IterWhich;
import suneido.language.*;

public class ContainerMethods {

	public static Object invoke(SuContainer c, String method, Object... args) {
		if (method == "Add")
			return add(c, args);
		if (method == "Assocs")
			return assocs(c, args);
		if (method == "Copy")
			return copy(c, args);
		if (method == "Delete")
			return delete(c, args);
		if (method == "Erase")
			return erase(c, args);
		if (method == "Find")
			return find(c, args);
		if (method == "GetDefault")
			return GetDefault(c, args);
		if (method == "Join")
			return join(c, args);
		if (method == "Member?")
			return memberQ(c, args);
		if (method == "Members")
			return members(c, args);
		if (method == "Reverse!")
			return reverse(c, args);
		if (method == "Set_default")
			return set_default(c, args);
		if (method == "Size")
			return size(c, args);
		if (method == "Slice")
			return slice(c, args);
		if (method == "Sort" || method == "Sort!")
			return sort(c, args);
		if (method == "Values")
			return values(c, args);
		if (method == "Unique!")
			return unique(c, args);
		return userDefined("Objects", method).invoke(c, method, args);
	}

	private static Object reverse(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Collections.reverse(c.getVec());
		return c;
	}

	private static final FunctionSpec sliceFS =
			new FunctionSpec(array("i", "n"), Integer.MAX_VALUE);

	private static Object slice(SuContainer c, Object[] args) {
		args = Args.massage(sliceFS, args);
		int vecsize = c.vecSize();
		int i = toInt(args[0]);
		if (i < 0)
			i += vecsize;
		i = max(0, min(i, vecsize));
		int n = toInt(args[1]);
		if (n < 0)
			n += vecsize - i;
		n = max(0, min(n, vecsize - i));
		return new SuContainer(c.getVec().subList(i, i + n));
	}

	private static Object copy(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(c);
	}

	private static final FunctionSpec keyValueFS =
			new FunctionSpec("key", "block");

	private static Object GetDefault(SuContainer c, Object[] args) {
		args = Args.massage(keyValueFS, args);
		Object x = c.getDefault(args[0], args[1]);
		if (x == args[1] && x instanceof SuBlock)
			x = Ops.call(x);
		return x;
	}

	private static SuContainer add(SuContainer c, Object[] args) {
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

	private static Object assocs(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.ASSOC));
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	static Object delete(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.delete(args[0]) ? c : false;
	}

	private static Object erase(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.erase(args[0]) ? c : false;
	}

	private static Object find(SuContainer c, Object[] args) {
		args = Args.massage(valueFS, args);
		Object key = c.find(args[0]);
		return key == null ? false : key;
	}

	static String join(SuContainer c, Object... args) {
		args = Args.massage(valueFS, args);
		String sep = Ops.toStr(args[0]);
		StringBuilder sb = new StringBuilder();
		for (Object x : c.getVec()) {
			if (x instanceof String)
				sb.append((String) x);
			else
				sb.append(Ops.display(x));
			sb.append(sep);
		}
		if (sb.length() > 0)
			sb.delete(sb.length() - sep.length(), sb.length());
		return sb.toString();
	}

	private static boolean memberQ(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.containsKey(args[0]);
	}

	private static Object members(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.KEY));
	}

	private static int size(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		switch (iterWhich(args)) {
		case LIST:
			return c.vecSize();
		case NAMED:
			return c.mapSize();
		default:
			return c.size();
		}
	}
	private static final FunctionSpec list_named_FS =
			new FunctionSpec(array("list", "named"), false, false);

	private static IterWhich iterWhich(Object[] args) {
		args = Args.massage(list_named_FS, args);
		boolean list = Ops.toBool(args[0]) == 1;
		boolean named = Ops.toBool(args[1]) == 1;
		if (list && !named)
			return IterWhich.LIST;
		else if (!list && named)
			return IterWhich.NAMED;
		else
			return IterWhich.ALL;
	}

	private static final FunctionSpec blockFS =
			new FunctionSpec(array("block"), Boolean.FALSE);

	private static SuContainer sort(SuContainer c, Object[] args) {
		args = Args.massage(blockFS, args);
		c.sort(args[0]);
		return c;
	}

	private static Object values(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.VALUE));
	}


	private static final FunctionSpec valueFS = new FunctionSpec("value");
	private static Object set_default(SuContainer c, Object[] args) {
		args = Args.massage(valueFS, args);
		c.setDefault(args[0]);
		return c;
	}

	private static Object unique(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		List<Object> v = c.getVec();
		int dst = 1;
		for (int src = 1; src < v.size(); ++src) {
			if (Ops.is_(v.get(src), v.get(src - 1)))
				continue;
			if (dst < src)
				v.set(dst, v.get(src));
			++dst;
		}
		while (v.size() > dst)
			v.remove(v.size() - 1);
		return c;
	}
}
