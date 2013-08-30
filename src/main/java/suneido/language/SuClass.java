/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.SuException.methodNotFound;

import java.util.Collections;
import java.util.Map;

import suneido.SuContainer;
import suneido.SuException;
import suneido.SuValue;
import suneido.Suneido;
import suneido.language.builtin.ContainerMethods;

/**
 * Suneido classes are instances of SuClass
 * with the methods stored in members.
 * The methods are instances of generated classes derived from {@link SuCallable})
 * Suneido instances are instances of {@link SuInstance}
 */
public class SuClass extends SuValue {
	private final String name;
	private final String baseGlobal; // TODO could be int slot
	private final Map<String, Object> members; // must be synchronized
	private boolean hasGetters = true; // till we know different
	private static final Map<String, SuCallable> basicMethods =
			BuiltinMethods.methods(SuClass.class);
	private static final BuiltinMethods userGeneralMethods = new BuiltinMethods(
			Object.class, "Objects");
	protected Context context = Suneido.context; // TODO pass it in

	@SuppressWarnings("unchecked")
	public SuClass(String className, String baseGlobal, Object members) {
		this.name = className.replace(AstCompile.METHOD_SEPARATOR, '.');
		this.baseGlobal = baseGlobal;
		this.members = (Map<String, Object>) (members == null ? Collections.emptyMap() : members);
	}

	@Override
	public Object get(Object member) {
		return get(this, member);
	}

	public Object get(SuValue self, Object member) {
		Object value = get2(member);
		if (value != null)
			return value instanceof SuCallable && self != null
					? new SuBoundMethod(self, (SuCallable) value) : value;
		if (hasGetters) {
			String getter = ("Get_" + member).intern();
			value = get2(getter);
			if (value instanceof SuCallable)
				return ((SuCallable) value).eval(self);
			value = get2("Get_");
			if (value instanceof SuCallable)
				return ((SuCallable) value).eval(self, member);
			hasGetters = false;
		}
		throw new SuException("member not found: " + member);
	}

	protected Object get2(Object member) {
		Object value = members.get(member);
		return (value != null || baseGlobal == null)
			? value
			: base().get2(member);
	}

	@Override
	public SuValue lookup(String method) {
		if (method == "<new>")
			return newInstanceMethod;
		SuCallable f = basicMethods.get(method);
		if (f != null)
			return f;
		Object o = get2(method);
		if (o instanceof SuCallable)
			return (SuCallable) o;
		if (method == "New")
			return initMethod;
		if (method == "CallClass")
			return newInstanceMethod;
		if (method == "Eval")
			return eval;
		f = userGeneralMethods.getMethod(method);
		if (f != null)
			return f;
		return new NotFound(method);
	}

	private static final SuCallable initMethod = new SuCallable() {
		@Override
		public Object eval(Object self, Object... args) {
			return init(args);
		}
	};

	private final SuCallable newInstanceMethod = new SuCallable() {
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
			this.method = method;
		}
		@Override
		public Object eval(Object self, Object... args) {
			if (method != "Default") {
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
		Args.massage(FunctionSpec.noParams, args);
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

	private static SuCallable eval = ContainerMethods.lookup("Eval");

	@Params("key, block")
	public static Object GetDefault(Object self, Object a, Object b) {
			return ((SuClass) self).getDefault(a, b);
	}

	public Object getDefault(Object k, Object b) {
		String key = Ops.toStr(k);
		Object x = members.get(key);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	public static Object Members(Object self) {
		SuContainer c = new SuContainer();
		for (Map.Entry<String, Object> e : ((SuClass) self).members.entrySet())
			if (e.getValue() != null)
				c.add(e.getKey());
		return c;
	}

	@Params("key")
	public static Object MemberQ(Object self, Object a) {
		return ((SuClass) self).hasMember(a);
	}

	boolean hasMember(Object k) {
		return get2(Ops.toStr(k)) != null;
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
	public String valueName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" = ").append("class");
		if (baseGlobal != null)
			sb.append(" : ").append(baseGlobal);
		sb.append(" {").append("\n");
		for (Map.Entry<String, Object> e : members.entrySet())
			sb.append(e).append("\n");
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
