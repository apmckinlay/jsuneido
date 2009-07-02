package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.Ops.toInt;
import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.*;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuContainer.IterResult;
import suneido.SuContainer.IterWhich;
import suneido.language.*;

public class ContainerMethods {

	public static Object invoke(SuContainer c, String method, Object... args) {
		if (method == "Add")
			return Add(c, args);
		if (method == "Assocs")
			return Assocs(c, args);
		if (method == "Copy")
			return Copy(c, args);
		if (method == "Delete")
			return Delete(c, args);
		if (method == "Erase")
			return Erase(c, args);
		if (method == "Find")
			return Find(c, args);
		if (method == "GetDefault")
			return GetDefault(c, args);
		if (method == "Join")
			return Join(c, args);
		if (method == "Member?")
			return MemberQ(c, args);
		if (method == "Members")
			return Members(c, args);
		if (method == "Readonly?")
			return ReadonlyQ(c, args);
		if (method == "Reverse!")
			return Reverse(c, args);
		if (method == "Set_default")
			return Set_default(c, args);
		if (method == "Set_readonly")
			return Set_readonly(c, args);
		if (method == "Size")
			return Size(c, args);
		if (method == "Slice")
			return Slice(c, args);
		if (method == "Sort" || method == "Sort!")
			return Sort(c, args);
		if (method == "Values")
			return Values(c, args);
		if (method == "Unique!")
			return Unique(c, args);
		return userDefined("Objects", method).invoke(c, method, args);
	}

	private static Object Set_readonly(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		c.setReadonly();
		return c.getReadonly();
	}

	private static Object ReadonlyQ(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return c.getReadonly();
	}

	private static Object Reverse(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Collections.reverse(c.getVec());
		return c;
	}

	private static final FunctionSpec sliceFS =
			new FunctionSpec(array("i", "n"), Integer.MAX_VALUE);

	private static Object Slice(SuContainer c, Object[] args) {
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

	private static Object Copy(SuContainer c, Object[] args) {
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

	@SuppressWarnings("unchecked")
	private static SuContainer Add(SuContainer c, Object[] args) {
		Object at = c.size();
		int n = 0;
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object x = iter.next();
			if (x instanceof Map.Entry
					&& ((Map.Entry<Object, Object>) x).getKey() == "at") {
				at = ((Map.Entry<Object, Object>) x).getValue();
				if (iter.hasNext())
					addUsage();
				break;
			}
			++n;
		}
		if (n == 0)
			return c;
		iter = new ArgsIterator(args);
		if (at instanceof Integer) {
			int at_i = (Integer) at;
			for (Object x : iter) {
				if (x instanceof Map.Entry)
					addUsage();
				else
					c.insert(at_i++, x);
				if (--n == 0)
					break; // stop before at:
			}
		} else if (n == 1)
			c.put(at, iter.next());
		else
			throw new SuException("can only Add multiple values to un-named "
					+ "or to numeric positions");
		return c;
	}

	private static void addUsage() {
		throw new SuException("usage: object.Add(value, ... [ at: position ])");
	}

	private static Object Assocs(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.ASSOC));
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	static Object Delete(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.delete(args[0]) ? c : false;
	}

	private static Object Erase(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.erase(args[0]) ? c : false;
	}

	private static Object Find(SuContainer c, Object[] args) {
		args = Args.massage(valueFS, args);
		Object key = c.find(args[0]);
		return key == null ? false : key;
	}

	static String Join(SuContainer c, Object... args) {
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

	private static boolean MemberQ(SuContainer c, Object[] args) {
		args = Args.massage(keyFS, args);
		return c.containsKey(args[0]);
	}

	private static Object Members(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.KEY));
	}

	private static int Size(SuContainer c, Object[] args) {
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

	private static SuContainer Sort(SuContainer c, Object[] args) {
		args = Args.massage(blockFS, args);
		c.sort(args[0]);
		return c;
	}

	private static Object Values(SuContainer c, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(c.iterable(iterWhich(args), IterResult.VALUE));
	}


	private static final FunctionSpec valueFS = new FunctionSpec("value");
	private static Object Set_default(SuContainer c, Object[] args) {
		args = Args.massage(valueFS, args);
		c.setDefault(args[0]);
		return c;
	}

	private static Object Unique(SuContainer c, Object[] args) {
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
