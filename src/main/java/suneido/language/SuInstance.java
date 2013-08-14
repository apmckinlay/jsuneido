/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.HashMap;
import java.util.Map;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.language.builtin.ContainerMethods;
import suneido.util.PairStack;

/**
 * An instance of a Suneido class
 * (which will be an instance of {@link SuClass})
 */
public class SuInstance extends SuValue {
	final SuClass myclass;
	private final Map<String, Object> ivars;
	private boolean isHashing;
	private static final Map<String, SuCallable> methods =
			BuiltinMethods.methods(SuInstance.class);

	public SuInstance(SuClass myclass) {
		this.myclass = myclass;
		this.ivars = new HashMap<String, Object>();
	}

	/** copy constructor */
	public SuInstance(SuInstance other) {
		myclass = other.myclass;
		ivars = new HashMap<String, Object>(other.ivars);
	}

	@Override
	public Object call(Object... args) {
		return myclass.lookup("Call").eval(this, args);
	}

	@Override
	public Object call0() {
		return myclass.lookup("Call").eval0(this);
	}
	@Override
	public Object call1(Object a) {
		return myclass.lookup("Call").eval1(this, a);
	}
	@Override
	public Object call2(Object a, Object b) {
		return myclass.lookup("Call").eval2(this, a, b);
	}
	@Override
	public Object call3(Object a, Object b, Object c) {
		return myclass.lookup("Call").eval3(this, a, b, c);
	}
	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		return myclass.lookup("Call").eval4(this, a, b, c, d);
	}

	@Override
	public SuValue lookup(String method) {
		// Prioritize looking up SuInstance built-ins first: Base(), Base?(),
		// and so on.
		SuValue m = methods.get(method);
		if (m != null)
			return m;
		// If not found in the instance, methods, look in the class. This
		// includes class built-ins -- Method?() -- as well as user-defined
		// methods. The class will also look for user-defined methods.
		return myclass.lookup(method);
	}

	public static SuClass Base(Object self) {
		return ((SuInstance) self).myclass;
	}

	@Params("value")
	public static Boolean BaseQ(Object self, Object a) {
		SuClass c = ((SuInstance) self).myclass;
		if (c == a)
			return true;
		return c.hasBase(a);
	}

	public static SuInstance Copy(Object self) {
		return new SuInstance((SuInstance) self);
	}

	@Params("key")
	public static Object Delete(Object self, Object a) {
		return ((SuInstance) self).ivars.remove(a) == null ? false : self;
	}

	public static Object Eval(Object self, Object... args) {
		return ContainerMethods.Eval(self, args);
	}

	public static Object Eval2(Object self, Object... args) {
		return ContainerMethods.Eval2(self, args);
	}

	@Params("key, block")
	public static Object GetDefault(Object self, Object a, Object b) {
		return ((SuInstance) self).getDefault(a, b);
	}

	private Object getDefault(Object k, Object b) {
		Object x = ivars.get(k);
		if (x != null)
			return x;
		return myclass.getDefault(k, b);
	}

	@Params("key")
	public static Boolean MemberQ(Object self, Object a) {
		return ((SuInstance) self).hasMember(a);
	}

	private boolean hasMember(Object key) {
		if (ivars.containsKey(key))
			return true;
		return myclass.hasMember(key);
	}

	public static SuContainer Members(Object self) {
		return new SuContainer(((SuInstance) self).ivars.keySet());
	}

	@Override
	public String toString() {
		Object toString = myclass.get2("ToString");
		if (toString instanceof SuCallable)
			return Ops.toStr(((SuCallable) toString).eval(this));
		return myclass + "()";
	}

	@Override
	public Object get(Object member) {
		Object value = ivars.get(member);
		if (value != null)
			return value;
		return myclass.get(this, member);
	}

	@Override
	public void put(Object member, Object value) {
		if (! Ops.isString(member))
			throw new SuException("non-string member name: "
					+ Ops.typeName(member));
		ivars.put(member.toString(), value);
	}

	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		if (! (value instanceof SuInstance))
			return false;
		return equals2(this, (SuInstance) value, new PairStack());
	}

	// avoid infinite recursion from self-reference
	private static boolean equals2(SuInstance x, SuInstance y, PairStack stack) {
		if (x.myclass != y.myclass || x.ivars.size() != y.ivars.size())
			return false;
		if (stack.contains(x, y))
			return true; // comparison is already in progress
		stack.push(x, y);
		try {
			for (Map.Entry<String, Object> e : x.ivars.entrySet())
				if (! equals3(e.getValue(), y.ivars.get(e.getKey()), stack))
					return false;
			return true;
		} finally {
			stack.pop();
		}
	}

	private static boolean equals3(Object x, Object y, PairStack stack) {
		if (x == y)
			return true;
		if (! (x instanceof SuInstance))
			return Ops.is_(x, y);
		return (y instanceof SuInstance)
				? equals2((SuInstance) x, (SuInstance) y, stack)
				: false;
	}

	@Override
	public synchronized int hashCode() {
		// Like SuContainer, SuInstance can be self-referential because its
		// instance variables can contain self-references. This implementation
		// of hashCode() is mainly stolen from java.util.Hashtable.hashCode(),
		// which also handled self-reference. [See on GrepCode]
		int h = myclass.hashCode();
		if (isHashing || ivars.isEmpty())
			return h;
		try {
			isHashing = true;
			h += 31 * ivars.hashCode();
		}
		finally {
			isHashing = false;
		}
		return h;
	}

	@Override
	public String typeName() {
		return "Object";
	}

}