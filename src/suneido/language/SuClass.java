/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.SuException.methodNotFound;

import java.util.Collections;
import java.util.Map;

import suneido.*;
import suneido.language.builtin.ContainerMethods;

import com.google.common.collect.ImmutableMap;

/**
 * Suneido classes are instances of SuClass
 * with the methods stored in members.
 * The methods are instances of generated classes derived from {@link SuFunction})
 * Suneido instances are instances of {@link SuInstance}
 */
public class SuClass extends SuValue {
	private final String className;
	private final String baseGlobal;
	private final Map<String, Object> members; // must be synchronized
	private boolean hasGetters = true; // till we know different
	private final Map<String, Object> basicMethods;

	@SuppressWarnings("unchecked")
	public SuClass(String className, String baseGlobal, Object members) {
		this.className = className;
		this.baseGlobal = baseGlobal;
		this.members = (Map<String, Object>) (members == null ? Collections.emptyMap() : members);
		basicMethods = basicMethods();
		linkMethods();
	}

	private Map<String, Object> basicMethods() {
		ImmutableMap.Builder<String, Object> b = ImmutableMap.builder();
		b.put("<new>", newInstanceMethod);
		b.put("Base", new Base());
		b.put("Base?", new BaseQ());
		b.put("Eval", new Eval());
		b.put("GetDefault", new GetDefault());
		b.put("Members", new Members());
		b.put("Member?", new MemberQ());
		b.put("MethodClass", new MethodClass());
		b.put("Method?", new MethodQ());
		return b.build();
	}

	protected void linkMethods() {
		for (Object v : members.values())
			if (v instanceof SuFunction) {
				SuFunction f = (SuFunction) v;
				f.myClass = this;
				for (Object c : f.constants)
					if (c instanceof SuCallable) // blocks
						((SuCallable) c).myClass = this;
			}
	}

	@Override
	public Object get(Object member) {
		return get(this, member);
	}

	public Object get(SuValue self, Object member) {
		Object value = get2(member);
		if (value != null)
			return value instanceof SuFunction && self != null
					? new SuBoundMethod(self, (SuFunction) value) : value;
		if (hasGetters) {
			String getter = ("Get_" + member).intern();
			value = get2(getter);
			if (value instanceof SuFunction)
				return ((SuFunction) value).eval(self);
			value = get2("Get_");
			if (value instanceof SuFunction)
				return ((SuFunction) value).eval(self, member);
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

	private SuValue lookup(String method) {
		Object f = basicMethods.get(method);
		if (f != null)
			return (SuValue) f;
		f = get2(method);
		if (f instanceof SuCallable)
			return (SuCallable) f;
		if (method == "New")
			return initMethod;
		if (method == "CallClass")
			return newInstanceMethod;
		return new NotFound(method);
	}

	private final static SuCallable initMethod = new SuCallable() {
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

	private class NotFound extends SuCallable {
		String method;
		NotFound(String method) {
			this.method = method;
		}
		@Override
		public Object eval(Object self, Object... args) {
			return notFound(self, method, args);
		}
	}

	@Override
	public Object call(Object... args) {
		return invoke(this, "CallClass", args);
	}

	@Override
	public Object eval(Object self, Object... args) {
		return invoke(self, "CallClass", args);
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		return lookup(method).eval(self, args);
	}

	@Override
	public Object invoke0(Object self, String method) {
		return lookup(method).eval0(self);
	}
	@Override
	public Object invoke1(Object self, String method, Object a) {
		return lookup(method).eval1(self, a);
	}
	@Override
	public Object invoke2(Object self, String method, Object a, Object b) {
		return lookup(method).eval2(self, a, b);
	}
	@Override
	public Object invoke3(Object self, String method, Object a, Object b,
			Object c) {
		return lookup(method).eval3(self, a, b, c);
	}
	@Override
	public Object invoke4(Object self, String method, Object a, Object b,
			Object c, Object d) {
		return lookup(method).eval4(self, a, b, c, d);
	}
	@Override
	public Object invoke5(Object self, String method, Object a, Object b,
			Object c, Object d, Object e) {
		return lookup(method).eval5(self, a, b, c, d, e);
	}
	@Override
	public Object invoke6(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f) {
		return lookup(method).eval6(self, a, b, c, d, e, f);
	}
	@Override
	public Object invoke7(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g) {
		return lookup(method).eval7(self, a, b, c, d, e, f, g);
	}
	@Override
	public Object invoke8(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g, Object h) {
		return lookup(method).eval8(self, a, b, c, d, e, f, g, h);
	}
	@Override
	public Object invoke9(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return lookup(method).eval9(self, a, b, c, d, e, f, g, h, i);
	}

	protected Object notFound(Object self, String method, Object... args) {
		// if there is a Default method
		// call it with the method added to the beginning of args
		Object fn = get2("Default");
		if (fn instanceof SuFunction) {
			Object newargs[] = new Object[1 + args.length];
			newargs[0] = method;
			System.arraycopy(args, 0, newargs, 1, args.length);
			return ((SuFunction) fn).eval(self, newargs);
		}
		throw methodNotFound(self, method);
	}

	private static Object init(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return null;
	}

	protected Object newInstance(Object... args) {
		SuInstance x = new SuInstance(this);
		x.invoke(x, "New", args);
		return x;
	}

	private SuClass base() {
		Object base = Globals.get(baseGlobal);
		if (!(base instanceof SuClass))
			throw new SuException("class base must be a Suneido value");
		return (SuClass) base;
	}

	private class Base extends BuiltinMethod0 {
		@Override
		public Object eval0(Object self) {
			if (baseGlobal == null)
				return Boolean.FALSE; // TODO Base should return Object class
			return Globals.get(baseGlobal);
		}
	}

	private class BaseQ extends BuiltinMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			return hasBase(a);
		}
	}

	private Boolean hasBase(Object base) {
		if (base == this)
			return Boolean.TRUE;
		if (baseGlobal == null)
			return Boolean.FALSE;
		return base().hasBase(base);
	}

	private static class Eval extends SuCallable {
		@Override
		public Object eval(Object self, Object... args) {
			return ContainerMethods.Eval(self, args);
		}
	};

	private class GetDefault extends BuiltinMethod2 {
		{ params = new FunctionSpec("key", "block"); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			String key = Ops.toStr(a);
			if (members.containsKey(key))
				return members.get(key);
			Object x = b;
			if (x instanceof SuBlock)
				x = Ops.call(x);
			return x;
		}
	}

	private class Members extends BuiltinMethod0 {
		@Override
		public Object eval0(Object self) {
			SuContainer c = new SuContainer();
			for (Map.Entry<String, Object> e : members.entrySet())
				if (e.getValue() != null)
					c.append(e.getKey());
			return c;
		}
	}

	private class MemberQ extends BuiltinMethod1 {
		{ params = new FunctionSpec("key"); }
		@Override
		public Object eval1(Object self, Object a) {
			String key = Ops.toStr(a);
			Object x = get2(key);
			return x == null ? Boolean.FALSE : Boolean.TRUE;
		}
	}

	private class MethodClass extends BuiltinMethod1 {
		{ params = new FunctionSpec("key"); }
		@Override
			public Object eval1(Object self, Object a) {
			String method = Ops.toStr(a);
			return methodClass(method);
		}
	}

	private Object methodClass(String method) {
		Object value = members.get(method);
		if (value instanceof SuFunction)
			return this;
		if (value == null && baseGlobal != null)
			return base().methodClass(method);
		return Boolean.FALSE;
	}

	private class MethodQ extends BuiltinMethod1 {
		{ params = new FunctionSpec("key"); }
		@Override
		public Object eval1(Object self, Object a) {
			String key = Ops.toStr(a);
			Object x = get2(key);
			return x instanceof SuFunction;
		}
	}

	@Override
	public String typeName() {
		return "Class";
	}

	@Override
	public String toString() {
		return className;
	}

	public String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append(className).append(" = ").append("class");
		if (baseGlobal != null)
			sb.append(" : ").append(baseGlobal);
		sb.append(" {").append("\n");
		for (Map.Entry<String, Object> e : members.entrySet())
			sb.append(e).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/** called by {@link SuFunction.superInvokeN} */
	public Object superInvoke(Object self, String member, Object... args) {
		if (baseGlobal == null) {
			if (member == "New")
				return null;
			else
				throw new SuException("must have base class to use super");
		}
		return base().invoke(self, member, args);
	}

}
