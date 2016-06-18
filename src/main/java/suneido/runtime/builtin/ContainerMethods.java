/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.runtime.Ops.toInt;
import static suneido.util.Util.array;

import java.math.BigDecimal;
import java.util.*;

import suneido.SuContainer;
import suneido.SuContainer.IterResult;
import suneido.SuContainer.IterWhich;
import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.*;
import suneido.util.Util.Range;

/** Used by {@link SuContainer} */
public final class ContainerMethods {
	private static final BuiltinMethods methods =
			new BuiltinMethods("object", ContainerMethods.class, "Objects");

	/** no instances, all static */
	private ContainerMethods() {
	}

	@SuppressWarnings("unchecked")
	public static Object Add(Object self, Object... args) {
		SuContainer c = (SuContainer) self;
		int numValuesToAdd = 0;
		Object atArg = null;
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object x = iter.next();
			if (x instanceof Map.Entry
					&& ((Map.Entry<Object, Object>) x).getKey().equals("at")) {
				atArg = ((Map.Entry<Object, Object>) x).getValue();
				if (iter.hasNext())
					addUsage();
				break;
			}
			++numValuesToAdd;
		}
		if (0 == numValuesToAdd) {
			return c;
		} else if (null == atArg) {
			addAt(c, c.vecSize(), numValuesToAdd, args);
			return c;
		} else if (atArg instanceof Number) {
			if (atArg instanceof Integer) {
				addAt(c, ((Integer) atArg).intValue(), numValuesToAdd, args);
				return c;
			} else {
				BigDecimal bd = Numbers.toBigDecimal(atArg);
				if (Numbers.integral(bd)
						&& Numbers.isInRange(bd, Numbers.BD_INT_MIN,
								Numbers.BD_INT_MAX)) {
					addAt(c, bd.intValue(), numValuesToAdd, args);
					return c;
				}
				// NOTE: This means if it's an integer but it doesn't fit in
				//       the range of primitive "int", it's going in as a
				//       dictionary put, not a vector add.
			}
		}
		putAt(c, atArg, numValuesToAdd, args); // Dictionary put "at"
		return c;
	}

	private static void addUsage() {
		throw new SuException("usage: object.Add(value, ... [ at: position ])");
	}

	private static void addAt(SuContainer c, int at, int count, Object...args) {
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

	private static void putAt(SuContainer c, Object atArg, int numValuesToAdd,
			Object... args) {
		if (1 == numValuesToAdd) {
			c.put(atArg, new ArgsIterator(args).next());
		} else {
			throw new SuException("can only Add multiple values to un-named "
					+ "or to numeric positions");
		}
	}

	private static SuContainer toContainer(Object x) {
		if (x instanceof SuContainer)
			return (SuContainer) x;
		if (x instanceof SuValue) {
			SuContainer c = Ops.toContainer(x);
			if (c != null)
				return c;
		}
		throw new SuException("can't convert to container");
	}

	public static Object Assocs(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args);
		return new Sequence(toContainer(self).iterable(iterWhich(args), IterResult.ASSOC));
	}

	public static Object Base(Object self) {
		return Builtins.get("Object");
	}

	@Params("value")
	public static Boolean BaseQ(Object self, Object a) {
		return a == Builtins.get("Object");
	}

	public static Object Copy(Object self) {
		return new SuContainer(toContainer(self));
	}

	public static Object Delete(Object self, Object... args) {
		return delete(self, args);
	}

	static Object delete(Object self, Object[] args) {
		SuContainer c = toContainer(self);
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
				c.clear();
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

	@Params("value, block=false")
	public static Object EqualRange(Object self, Object a, Object b) {
		Range r = toContainer(self).equalRange(a, b);
		return SuContainer.of(r.left, r.right);
	}

	public static Object Erase(Object self, Object... args) {
		SuContainer c = toContainer(self);
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
		SuContainer result = new SuContainer();
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
		Object key = toContainer(self).find(a);
		return key == null ? false : key;
	}

	@Params("key, block")
	public static Object GetDefault(Object self, Object a, Object b) {
		Object x = toContainer(self).getIfPresent(a);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	public static Object Iter(Object self) {
		return new Iterate(toContainer(self));
	}

	private static final class Iterate extends SuValue {
		SuContainer c;
		Iterator<Object> iter;

		Iterate(SuContainer c) {
			this.c = c;
			iter = c.iterator();
		}

		@Override
		public String typeName() {
			return "ObjectIter";
		}

		@Override
		public SuValue lookup(String method) {
			return IterateMethods.singleton.lookup(method);
		}
	}

	public static final class IterateMethods extends BuiltinMethods {
		public static final SuValue singleton = new IterateMethods();

		protected IterateMethods() {
			super("objectiter", IterateMethods.class, null);
		}

		public static Object Next(Object self) {
			Iterate iter = (Iterate) self;
			try {
				return iter.iter.hasNext() ? iter.iter.next() : self;
			} catch (ConcurrentModificationException e) {
				throw new SuException("object modified during iteration");
			}
		}

		public static Object Rewind(Object self) {
			Iterate iter = (Iterate) self;
			iter.iter = iter.c.iterator();
			return null;
		}

		public static Object Iter(Object self) {
			return self;
		}
	}

	@Params("string = ''")
	public static Object Join(Object self, Object a) {
		String sep = Ops.toStr(a);
		StringBuilder sb = new StringBuilder();
		for (Object x : toContainer(self).vec) {
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

	@Params("value, block=false")
	public static Object LowerBound(Object self, Object a, Object b) {
		return toContainer(self).lowerBound(a, b);
	}

	@Params("key")
	public static Object MemberQ(Object self, Object a) {
		return toContainer(self).containsKey(a);
	}

	public static Object Members(Object self, Object... args) {
		SuContainer ob = toContainer(self);
		if (args.length == 0)
			return new Sequence(ob.iterable(IterWhich.ALL, IterResult.KEY));
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		return new Sequence(ob.iterable(iterWhich(args), IterResult.KEY));
	}

	@Params("string")
	public static Object MethodQ(Object self, Object a) {
		return false;
	}

	public static Object Size(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		SuContainer c = toContainer(self);
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
		return toContainer(self).getReadonly();
	}

	public static Object ReverseE(Object self) {
		SuContainer c = toContainer(self);
		c.reverse();
		return c;
	}

	public static Object Set_readonly(Object self) {
		SuContainer c = toContainer(self);
		c.setReadonly();
		return self;
	}

	@Params("i, n=INTMAX")
	public static Object Slice(Object self, Object a, Object b) {
		SuContainer c = toContainer(self);
		int vecsize = c.vecSize();
		int i = toInt(a);
		if (i < 0)
			i += vecsize;
		i = max(0, min(i, vecsize));
		int n = toInt(b);
		if (n < 0)
			n += vecsize - i;
		n = max(0, min(n, vecsize - i));
		return new SuContainer(c.vec.subList(i, i + n));
	}

	@Params("block")
	public static Object Sort(Object self, Object a) {
		SuContainer c = toContainer(self);
		c.sort(a);
		return c;
	}

	@Params("block")
	public static Object SortE(Object self, Object a) {
		SuContainer c = toContainer(self);
		c.sort(a);
		return c;
	}

	public static Object Values(Object self, Object... args) {
		Args.massage(FunctionSpec.NO_PARAMS, args); // args must be named
		SuContainer c = toContainer(self);
		return new Sequence(c.iterable(iterWhich(args), IterResult.VALUE));
	}

	@Params("value=null")
	public static Object Set_default(Object self, Object a) {
		SuContainer c = toContainer(self);
		c.setDefault(a);
		return c;
	}

	public static Object UniqueE(Object self) {
		SuContainer c = toContainer(self);
		List<Object> v = c.vec;
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

	@Params("value, block=false")
	public static Object UpperBound(Object self, Object a, Object b) {
		return toContainer(self).upperBound(a, b);
	}

	public static SuCallable lookup(String method) {
		return methods.lookup(method);
	}

}
