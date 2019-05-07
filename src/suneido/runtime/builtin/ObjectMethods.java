/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.runtime.Numbers.intOrMin;
import static suneido.util.Util.array;

import java.util.AbstractMap;
import java.util.Map;

import suneido.SuException;
import suneido.SuObject;
import suneido.SuObject.IterResult;
import suneido.SuObject.IterWhich;
import suneido.SuValue;
import suneido.runtime.*;

/** Used by {@link SuObject} */
public final class ObjectMethods {
	private static final BuiltinMethods methods =
			new BuiltinMethods("object", ObjectMethods.class, "Objects");

	/** no instances, all static */
	private ObjectMethods() {
	}

	@SuppressWarnings("unchecked")
	public static Object Add(Object self, Object... args) {
		SuObject c = (SuObject) self;
		int numValuesToAdd = 0;
		Object atArg = null;
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object x = iter.next();
			if (x instanceof Map.Entry) {
					if (((Map.Entry<Object, Object>) x).getKey().equals("at")) {
						atArg = ((Map.Entry<Object, Object>) x).getValue();
						break;
					}
			} else
				++numValuesToAdd;
		}
		if (0 == numValuesToAdd) {
			// nothing to do
		} else if (null == atArg) {
			addAt(c, c.vecSize(), numValuesToAdd, args);
		} else {
			int i = intOrMin(atArg);
			if (i != Integer.MIN_VALUE)
				addAt(c, i, numValuesToAdd, args);
			else
				putAt(c, atArg, numValuesToAdd, args);
		}
		return c;
	}

	private static void addUsage() {
		throw new SuException("usage: object.Add(value, ... [ at: position ])");
	}

	private static void addAt(SuObject c, int at, int count, Object...args) {
		final int endIndex = at + count;
		final ArgsIterator iter = new ArgsIterator(args);
		for (; at < endIndex; ++at) {
			Object x = iter.next();
			if (x instanceof Map.Entry)
				addUsage();
			else
				c.insert(at, x);
		}
	}

	private static void putAt(SuObject c, Object atArg, int numValuesToAdd,
			Object... args) {
		if (1 == numValuesToAdd) {
			c.put(atArg, new ArgsIterator(args).next());
		} else {
			throw new SuException("can only Add multiple values to un-named "
					+ "or numeric positions");
		}
	}

	private static SuObject toObject(Object x) {
		if (x instanceof SuObject)
			return (SuObject) x;
		if (x instanceof SuValue) {
			SuObject c = Ops.toObject(x);
			if (c != null)
				return c;
		}
		throw new SuException("can't convert to container");
	}

	public static Object Assocs(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args);
		return new Sequence(toObject(self).iterable(iterWhich(args), IterResult.ASSOC));
	}

	@Params("value, block=false")
	public static Object BinarySearch(Object self, Object a, Object b) {
		return toObject(self).binarySearch(a, b);
	}

	public static Object Copy(Object self) {
		return new SuObject(toObject(self));
	}

	public static Object Delete(Object self, Object... args) {
		return delete(self, args);
	}

	static Object delete(Object self, Object[] args) {
		SuObject c = toObject(self);
		ArgsIterator iter = new ArgsIterator(args);
		if (! iter.hasNext())
			deleteUsage();
		Object arg = iter.next();
		if (arg instanceof AbstractMap.SimpleEntry) {
			@SuppressWarnings("unchecked")
			AbstractMap.SimpleEntry<Object,Object> e =
					(AbstractMap.SimpleEntry<Object,Object>) arg;
			if (! "all".equals(e.getKey()))
				deleteUsage();
			if (e.getValue() == Boolean.TRUE)
				c.deleteAll();
		} else {
			c.delete(arg);
			while (iter.hasNext()) {
				arg = iter.next();
				if (arg instanceof AbstractMap.SimpleEntry)
					deleteUsage();
				c.delete(arg);
			}
		}
		return c;
	}

	private static void deleteUsage() {
		throw new SuException("usage: object.Delete(member ...) or object.Delete(all:)");
	}

	public static Object Erase(Object self, Object... args) {
		SuObject c = toObject(self);
		ArgsIterator iter = new ArgsIterator(args);
		if (! iter.hasNext())
			eraseUsage();
		for (Object arg : iter) {
			if (arg instanceof AbstractMap.SimpleEntry)
				eraseUsage();
			c.erase(arg);
		}
		return c;
	}

	private static void eraseUsage() {
		throw new SuException("usage: object.Erase(member ...)");
	}

	// also called by SuInstance and SuClass
	public static Object Eval(Object self, Object... args) {
		return evaluate(self, args);
	}

	public static Object Eval2(Object self, Object... args) {
		Object value = evaluate(self, args);
		SuObject result = new SuObject();
		if (value != null)
			result.add(value);
		return result;
	}

	private static Object evaluate(Object self, Object... args) {
		ArgsIterator iter = new ArgsIterator(args);
		if (!iter.hasNext())
			throw new SuException("usage: object.Eval(callable [, args...]");
		Object arg = iter.next();
		if (!(arg instanceof SuValue))
			throw new SuException("usage: object.Eval requires callable");
		return ((SuValue) arg).eval(self, iter.rest());
	}

	@Params("value")
	public static Object Find(Object self, Object a) {
		Object key = toObject(self).find(a);
		return key == null ? false : key;
	}

	@Params("member, block")
	public static Object GetDefault(Object self, Object a, Object b) {
		Object x = toObject(self).getIfPresent(a);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	public static Object Iter(Object self) {
		return new IterJtoS(toObject(self));
	}

	@Params("string = ''")
	public static Object Join(Object self, Object a) {
		String sep = Ops.toStr(a);
		StringBuilder sb = new StringBuilder();
		for (Object x : toObject(self).vec) {
			if (Ops.isString(x))
				sb.append(x.toString());
			else
				sb.append(Ops.display(x));
			sb.append(sep);
		}
		if (sb.length() > 0)
			sb.delete(sb.length() - sep.length(), sb.length());
		return sb.toString();
	}

	//TODO remove once everyone has switched to BinarySearch
	@Params("value, block=false")
	public static Object LowerBound(Object self, Object a, Object b) {
		return toObject(self).binarySearch(a, b);
	}

	@Params("key")
	public static Object MemberQ(Object self, Object a) {
		return toObject(self).containsKey(a);
	}

	public static Object Members(Object self, Object... args) {
		SuObject ob = toObject(self);
		if (args.length == 0)
			return new Sequence(ob.iterable(IterWhich.ALL, IterResult.KEY));
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		return new Sequence(ob.iterable(iterWhich(args), IterResult.KEY));
	}

	public static Object Size(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		SuObject c = toObject(self);
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
		boolean list = Ops.toIntBool(args[0]) == 1;
		boolean named = Ops.toIntBool(args[1]) == 1;
		if (list && !named)
			return IterWhich.LIST;
		else if (!list && named)
			return IterWhich.NAMED;
		else
			return IterWhich.ALL;
	}

	public static Object ReadonlyQ(Object self) {
		return toObject(self).getReadonly();
	}

	public static Object ReverseE(Object self) {
		SuObject c = toObject(self);
		c.reverse();
		return c;
	}

	public static Object Set_readonly(Object self) {
		SuObject c = toObject(self);
		c.setReadonly();
		return self;
	}

	@Params("block")
	public static Object SortE(Object self, Object a) {
		SuObject c = toObject(self);
		c.sort(a);
		return c;
	}

	public static Object Values(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		SuObject c = toObject(self);
		return new Sequence(c.iterable(iterWhich(args), IterResult.VALUE));
	}

	@Params("value=null")
	public static Object Set_default(Object self, Object a) {
		SuObject c = toObject(self);
		c.setDefault(a);
		return c;
	}

	public static Object UniqueE(Object self) {
		SuObject c = toObject(self);
		c.unique();
		return c;
	}

	public static SuCallable lookup(String method) {
		return methods.lookup(method);
	}

}
