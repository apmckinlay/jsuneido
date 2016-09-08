/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.runtime.FunctionSpec.NA;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.runtime.builtin.ContainerMethods;
import suneido.util.PairStack;

/**
 * <p>
 * An instance of a Suneido class. The class itself is represented by an
 * instance of {@link SuClass}.
 * </p>
 * <p>
 * NOTE: In cSuneido, there is no explicit {@code SuInstance} and class
 * instances are represented by {@code SuObject}s. See, <em>eg</em>
 * {@code SuObject::myclass}.
 * </p>
 */
// FIXME: Deal with thread safety issues
public class SuInstance extends SuValue {
	final SuClass myclass;
	private final Map<String, Object> ivars;
	private static final Map<String, SuCallable> methods =
			BuiltinMethods.methods("object", SuInstance.class);

	public SuInstance(SuClass myclass) {
		this.myclass = myclass;
		this.ivars = new HashMap<>();
	}

	/** copy constructor */
	public SuInstance(SuInstance other) {
		myclass = other.myclass;
		ivars = new HashMap<>(other.ivars);
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

	@Params("key=NA, all=NA")
	public static Object Delete(Object self, Object key, Object all) {
		if ((key == NA) == (all == NA))
			throw new SuException("usage: object.Delete(field) or object.Delete(all:)");
		if (key != NA)
			((SuInstance) self).ivars.remove(key);
		else // all:
			((SuInstance) self).ivars.clear();
		return self;
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
		return myclass.getDefault(this, k, b);
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

	@Params("all=false")
	public static SuContainer Members(Object self, Object all) {
		SuContainer c = new SuContainer(((SuInstance) self).ivars.keySet());
		if (all == Boolean.TRUE) {
			((SuInstance) self).myclass.members2(c, true);
			c.sort(false);
			c.unique();
		}
		return c;
	}

	public static Object Size(Object self) {
		return ((SuInstance) self).ivars.size();
	}

	@Override
	public String display() {
		Object toString = myclass.get2("ToString");
		if (toString instanceof SuCallable)
			return Ops.toStr(((SuCallable) toString).eval(this));
		return myclass.internalName() + "()";
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

	public Object classGet(Object member) {
		return myclass.get2(member);
	}

	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		if (! (value instanceof SuInstance))
			return false;
		return equals2(this, (SuInstance) value, null);
	}

	// avoid infinite recursion from self-reference
	public static boolean equals2(SuInstance x, SuInstance y, PairStack stack) {
		if (x.myclass != y.myclass || x.ivars.size() != y.ivars.size())
			return false;
		if (stack == null)
			stack = new PairStack();
		else if (stack.contains(x, y))
			return true; // comparison is already in progress
		stack.push(x, y);
		try {
			for (Map.Entry<String, Object> e : x.ivars.entrySet())
				if (! SuContainer.equals3(e.getValue(), y.ivars.get(e.getKey()), stack))
					return false;
			return true;
		} finally {
			stack.pop();
		}
	}

	@Override
	public void pack(ByteBuffer buf) {
		throw new SuException("can't pack class instance");
	}

	@Override
	public int packSize(int nest) {
		throw new SuException("can't pack class instance");
	}

	@Override
	public int hashCode() {
		return myclass.hashCode();
	}

	@Override
	public String typeName() {
		return "Object";
	}

}
