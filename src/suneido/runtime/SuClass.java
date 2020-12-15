/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.SuException.methodNotFound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import suneido.SuException;
import suneido.SuObject;
import suneido.SuValue;
import suneido.Suneido;
import suneido.compiler.AstCompile;
import suneido.runtime.builtin.ObjectMethods;

/**
 * Suneido classes are instances of SuClass
 * with the methods stored in members.
 * The methods are instances of generated classes derived from {@link SuCallable})
 * Suneido instances are instances of {@link SuInstance}
 */
public class SuClass extends SuValue implements Showable {
	private final String library;
	private final String name;
	private final String baseGlobal; // TODO could be int slot
	private final Map<String, Object> members; // must be synchronized
	private boolean hasGet_ = true; // till we know different
	private static final Map<String, SuCallable> basicMethods =
			BuiltinMethods.methods("class", SuClass.class);
	private static final BuiltinMethods userGeneralMethods = new BuiltinMethods(
			"object", null, "Objects");
	protected Context context = Suneido.context; // TODO pass it in

	@SuppressWarnings("unchecked")
	public SuClass(String library, String className, String baseGlobal, Object members) {
		this.library = library;
		this.name = className.replace(AstCompile.METHOD_SEPARATOR, '.');
		this.baseGlobal = baseGlobal;
		this.members = (Map<String, Object>) (members == null ? Collections.emptyMap() : members);
	}

	@Override
	public Object get(Object member) {
		return get(this, member);
	}

	public Object get(SuValue self, Object member) {
		Object value = get1(self, member);
		if (value == null)
			throw new SuException("uninitialized member: " + member);
		return value;
	}

	public Object get1(SuValue self, Object member) {
		Object value = get2(member);
		if (value != null)
			return value instanceof SuCallable && self != null
					? new SuBoundMethod(self, (SuCallable) value) : value;
		if (hasGet_) {
			if (null != (value = get2("Getter_")))
				return meval1(self, value, member, "Getter_");
			hasGet_ = false; // avoid future attempts
		}
		String name;
		if (null != (value = get2(name = ("Getter_" + member).intern())))
			return meval0(self, value, name);
		return null;
	}

	private static Object meval0(Object self, Object method, String name) {
		if (method instanceof SuValue)
			return ((SuValue) method).eval0(self);
		throw new SuException("can't call " + Ops.typeName(self) + " (" + name + ")");
	}

	private static Object meval1(Object self, Object method, Object member, String name) {
		if (method instanceof SuValue)
			return ((SuValue) method).eval1(self, member);
		throw new SuException("can't call " + Ops.typeName(self) + " (" + name + ")");
	}

	Object get2(Object member) {
		Object value = members.get(member);
		return (value != null || baseGlobal == null)
			? value
			: base().get2(member);
	}

	@Override
	public SuValue lookup(String method) {
		if ("<new>".equals(method))
			return newInstanceMethod;
		SuCallable f = basicMethods.get(method);
		if (f != null)
			return f;
		Object o = get2(method);
		if (o instanceof SuCallable)
			return (SuCallable) o;
		switch (method) {
		case "New":
			return initMethod;
		case "CallClass":
			return newInstanceMethod;
		}
		f = userGeneralMethods.getMethod(method);
		if (f != null)
			return f;
		return new NotFound(method);
	}

	private static final SuCallable initMethod =
			new SuBuiltinMethod("class.New", null) {
				@Override
				public Object eval(Object self, Object... args) {
					return init(args);
				}
	};

	private final SuCallable newInstanceMethod =
			new SuBuiltinMethod("class.CallClass", null) {
				@Override
				public Object eval(Object self, Object... args) {
					return newInstance(args);
				}
	};

	@Override
	public Object call(Object... args) {
		return lookup("CallClass").eval(this, args);
	}

	@Override
	public Object call0() {
		return lookup("CallClass").eval0(this);
	}
	@Override
	public Object call1(Object a) {
		return lookup("CallClass").eval1(this, a);
	}
	@Override
	public Object call2(Object a, Object b) {
		return lookup("CallClass").eval2(this, a, b);
	}
	@Override
	public Object call3(Object a, Object b, Object c) {
		return lookup("CallClass").eval3(this, a, b, c);
	}
	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		return lookup("CallClass").eval4(this, a, b, c, d);
	}

	@Override
	public Object eval(Object self, Object... args) {
		return lookup("CallClass").eval(self, args);
	}

	@Override
	public Object eval0(Object self) {
		return lookup("CallClass").eval0(self);
	}
	@Override
	public Object eval1(Object self, Object a) {
		return lookup("CallClass").eval1(self, a);
	}
	@Override
	public Object eval2(Object self, Object a, Object b) {
		return lookup("CallClass").eval2(self, a, b);
	}
	@Override
	public Object eval3(Object self, Object a, Object b, Object c) {
		return lookup("CallClass").eval3(self, a, b, c);
	}
	@Override
	public Object eval4(Object self, Object a, Object b, Object c, Object d) {
		return lookup("CallClass").eval4(self, a, b, c, d);
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	private static class NotFound extends SuCallable {
		String method;
		NotFound(String method) {
			this.callableType = CallableType.METHOD_NOT_FOUND;
			this.method = method;
		}
		@Override
		public Object eval(Object self, Object... args) {
			if (! "Default".equals(method)) {
				Object fn = toClass(self).get2("Default");
				if (fn instanceof SuCallable) {
					Object newargs[] = new Object[1 + args.length];
					newargs[0] = method;
					System.arraycopy(args, 0, newargs, 1, args.length);
					return ((SuCallable) fn).eval(self, newargs);
				}
			}
			throw methodNotFound(self, method);
		}
	}

	private static Object init(Object[] args) {
		Args.massage(FunctionSpec.NO_PARAMS, args);
		return null;
	}

	protected Object newInstance(Object... args) {
		SuInstance x = new SuInstance(this);
		x.lookup("New").eval(x, args);
		return x;
	}

	private SuClass base() {
		Object base = context.get(baseGlobal);
		if (! (base instanceof SuClass))
			throw new SuException("class base must be a Suneido value");
		return (SuClass) base;
	}

	public static Object Base(Object self) {
		SuClass c = (SuClass) self;
		return (c.baseGlobal == null) ? Boolean.FALSE : c.context.get(c.baseGlobal);
	}

	@Params("value")
	public static Object BaseQ(Object self, Object a) {
			return ((SuClass) self).hasBase(a);
	}

	boolean hasBase(Object base) {
		if (base == this)
			return Boolean.TRUE;
		if (baseGlobal == null)
			return Boolean.FALSE;
		return base().hasBase(base);
	}

	public static Object Eval(Object self, Object... args) {
		return ObjectMethods.Eval(self, args);
	}

	public static Object Eval2(Object self, Object... args) {
		return ObjectMethods.Eval2(self, args);
	}

	@Params("key, block")
	public static Object GetDefault(Object self, Object a, Object b) {
			return ((SuClass) self).getDefault(self, a, b);
	}

	public Object getDefault(Object self, Object key, Object b) {
		Object x = get1((SuValue) self, key);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	@Params("all=false")
	public static Object Members(Object self, Object all) {
		SuObject c = new SuObject();
		((SuClass) self).members2(c, all == Boolean.TRUE);
		if (all == Boolean.TRUE) {
			c.sort(false);
			c.unique();
		}
		return c;
	}

	public void members2(SuObject c, boolean all) {
		for (Map.Entry<String, Object> e : members.entrySet())
			if (e.getValue() != null)
				c.add(e.getKey());
		if (all && baseGlobal != null)
			base().members2(c, all);
	}

	@Params("key")
	public static Object MemberQ(Object self, Object a) {
		return ((SuClass) self).hasMember(a);
	}

	boolean hasMember(Object k) {
		return get2(k) != null;
	}

	@Params("key")
	public static Object MethodClass(Object self, Object a) {
		String method = Ops.toStr(a);
		SuClass c = toClass(self);
		if (c == null)
			return Boolean.FALSE;
		return c.methodClass(method);
	}

	private Object methodClass(String method) {
		Object value = members.get(method);
		if (value instanceof SuCallable)
			return this;
		if (value == null && baseGlobal != null)
			return base().methodClass(method);
		return Boolean.FALSE;
	}

	@Params("key")
	public static Object MethodQ(Object self, Object a) {
		String method = Ops.toStr(a);
		SuClass c = toClass(self);
		if (c == null)
			return Boolean.FALSE;
		// can't use lookup because it throws
		Object x = c.get2(method);
		return x instanceof SuCallable;
	}

	public static Object Size(Object self) {
		return ((SuClass) self).members.size();
	}

	public static Object ReadonlyQ(Object self) {
		return true;
	}

	//==========================================================================

	private static SuClass toClass(Object x) {
		if (x instanceof SuInstance)
			x = ((SuInstance) x).myclass;
		if (x instanceof SuClass)
			return (SuClass) x;
		return null;
	}

	@Override
	public String typeName() {
		return "Class";
	}

	@Override
	public final String internalName() {
		return name;
	}

	@Override
	public String display() {
		StringBuilder sb = new StringBuilder();
		if (name != "" && ! name.endsWith("$c") && !name.equals("eval"))
			sb.append(name).append(' ');
		sb.append("/* ");
		if (library != null && ! library.isEmpty())
			sb.append(library).append(' ');
		sb.append("class");
		if (baseGlobal != null)
			sb.append(" : ").append(baseGlobal);
		sb.append(" */");
		return sb.toString();
	}

	@Override
	public String show() {
		StringBuilder sb = new StringBuilder();
		if (baseGlobal == null)
			sb.append("class");
		else
			sb.append(baseGlobal);
		sb.append("{");
		var keys = new ArrayList<>(members.keySet());
		Collections.sort(keys);
		var sep = "";
		for (var key : keys) {
			sb.append(sep).append(key);
			var val = members.get(key);
			if (val instanceof SuCallable)
				sb.append("()");
			else
				sb.append(": ").append(val);
			sep = "; ";
		}
		sb.append("}");
		return sb.toString();
	}

	public Object superInvoke(Object self, String member, Object... args) {
		if (baseGlobal == null) {
			if (member == "New")
				return null;
			else
				throw new SuException("must have base class to use super");
		}
		return base().lookup(member).eval(self, args);
	}

}
