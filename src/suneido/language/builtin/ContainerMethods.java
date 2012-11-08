/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static suneido.language.FunctionSpec.NA;
import static suneido.language.Ops.toInt;
import static suneido.util.Util.array;

import java.util.*;

import suneido.*;
import suneido.SuContainer.IterResult;
import suneido.SuContainer.IterWhich;
import suneido.language.*;
import suneido.util.Util.Range;

/** Used by {@link SuContainer} */
public final class ContainerMethods {
	private static final BuiltinMethods2 methods =
			new BuiltinMethods2(ContainerMethods.class, "Objects");

	/** no instances, all static */
	private ContainerMethods() {
	}

	@SuppressWarnings("unchecked")
	public static Object Add(Object self, Object... args) {
		SuContainer c = (SuContainer) self;
		Object at = c.vecSize();
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

	public static Object Assocs(Object self, Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuSequence(((SuContainer) self).iterable(iterWhich(args), IterResult.ASSOC));
	}

	public static Object Base(Object self) {
		return Builtins.get("Object");
	}

	public static Object Copy(Object self) {
		return new SuContainer((SuContainer) self);
	}

	@Params("key=NA, all=NA")
	public static Object Delete(Object self, Object key, Object all) {
		return delete(self, key, all);
	}

	static Object delete(Object self, Object key, Object all) {
		if ((key == NA) == (all == NA))
			throw new SuException("usage: object.Delete(field) or object.Delete(all:)");
		SuContainer c = (SuContainer) self;
		if (key != NA)
			return c.delete(key) ? c : false;
		else if (all == Boolean.TRUE)
			c.clear();
		return c;
	}

	@Params("value, block=false")
	public static Object EqualRange(Object self, Object a, Object b) {
		Range r = ((SuContainer) self).equalRange(a, b);
		return SuContainer.of(r.left, r.right);
	}

	@Params("key")
	public static Object Erase(Object self, Object a) {
		SuContainer c = (SuContainer) self;
		return c.erase(a) ? c : false;
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
		// BUG copyOfRange won't work in all cases e.g. @args
		return ((SuValue) arg).eval(self,
				Arrays.copyOfRange(args, 1, args.length));
	}

	@Params("value")
	public static Object Find(Object self, Object a) {
		Object key = ((SuContainer) self).find(a);
		return key == null ? false : key;
	}

	@Params("key, block")
	public static Object GetDefault(Object self, Object a, Object b) {
		Object x = ((SuContainer) self).getIfPresent(a);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	public static Object Iter(Object self) {
		return new Iterate((SuContainer) self);
	}

	private static final class Iterate extends SuValue {
		SuContainer c;
		Iterator<Object> iter;

		Iterate(SuContainer c) {
			this.c = c;
			iter = c.iterator();
		}

		@Override
		public SuValue lookup(String method) {
			return IterateMethods.singleton.lookup(method);
		}
	}

	public static final class IterateMethods extends BuiltinMethods2 {
		public static final SuValue singleton = new IterateMethods();

		protected IterateMethods() {
			super(IterateMethods.class, null);
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

		@Override
		public String typeName() {
			return "ObjectIter";
		}

	}

	@Params("value")
	public static Object Join(Object self, Object a) {
		String sep = Ops.toStr(a);
		StringBuilder sb = new StringBuilder();
		for (Object x : ((SuContainer) self).vec) {
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
		return ((SuContainer) self).lowerBound(a, b);
	}

	@Params("key")
	public static Object MemberQ(Object self, Object a) {
		return ((SuContainer) self).containsKey(a);
	}

	public static Object Members(Object self, Object... args) {
		if (args.length == 0)
			return new SuSequence(((SuContainer) self)
					.iterable(IterWhich.ALL, IterResult.KEY));
		Args.massage(FunctionSpec.noParams, args); // args must be named
		return new SuSequence(((SuContainer) self)
				.iterable(iterWhich(args), IterResult.KEY));
	}

	public static Object Size(Object self, Object... args) {
		Args.massage(FunctionSpec.noParams, args); // args must be named
		SuContainer c = (SuContainer) self;
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
		return ((SuContainer) self).getReadonly();
	}

	public static Object ReverseE(Object self) {
		SuContainer c = (SuContainer) self;
		c.reverse();
		return c;
	}

	public static Object Set_readonly(Object self) {
		SuContainer c = (SuContainer) self;
		c.setReadonly();
		return c.getReadonly();
	}

	@Params("i, n=INTMAX")
	public static Object Slice(Object self, Object a, Object b) {
		SuContainer c = (SuContainer) self;
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
		SuContainer c = (SuContainer) self;
		c.sort(a);
		return c;
	}

	@Params("block")
	public static Object SortE(Object self, Object a) {
		SuContainer c = (SuContainer) self;
		c.sort(a);
		return c;
	}

	public static Object Values(Object self, Object... args) {
		Args.massage(FunctionSpec.noParams, args); // args must be named
		SuContainer c = (SuContainer) self;
		return new SuSequence(c.iterable(iterWhich(args), IterResult.VALUE));
	}

	@Params("value=null")
	public static Object Set_default(Object self, Object a) {
		SuContainer c = (SuContainer) self;
		c.setDefault(a);
		return c;
	}

	public static Object UniqueE(Object self) {
		SuContainer c = (SuContainer) self;
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
		return ((SuContainer) self).upperBound(a, b);
	}

	public static SuCallable lookup(String method) {
		return methods.lookup(method);
	}

	public static void main(String[] args) {
		System.out.println(IterateMethods.singleton);
	}

}
