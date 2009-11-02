package suneido.language;

import static suneido.language.SuClass.Marker.*;

import java.util.Map;

import suneido.*;
import suneido.language.builtin.ContainerMethods;

/**
 * The Java base class for compiled Suneido classes.
 * Immutable after initialization by {@link CompileGenerator.finishClass}
 * @see SuMethod
 * @see SuFunction
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuClass extends SuCallable {
	protected String baseGlobal = null;
	protected Map<String, Object> vars; // must be synchronized
	protected boolean hasGetters = false;

	// NOMETHOD is used instead of null
	// to differentiate from map.get returning null for missing
	public static class Marker {
		public static final Marker NOMETHOD = new Marker();
		public static final Marker GETTER = new Marker();
		public static final Marker GETMEM = new Marker();
	};
	public static final class Method extends Marker {
		final SuClass c;
		public Method(SuClass c) {
			this.c = c;
		}
	}

	@Override
	public Object get(Object member) {
		Object value = get2(member);
		if (value == GETTER)
			value = invoke(this, "Get_", member);
		else if (value == GETMEM)
			value = invoke(this, ("Get_" + (String) member).intern());
		else if (value instanceof Method)
			value = new SuMethod(this, (String) member);
		if (value == null)
			throw new SuException("member not found: " + member);
		return value;
	}

	public Object get2(Object member) {
		Object value = get3(member);
		if (value != null)
			return value;
		value = getBase(member);
		if (hasGetters) {
			String getter = ("Get_" + member).intern();
			if (value == null || value == GETTER || value == GETMEM) {
				if (haveGetter(getter))
					value = GETMEM; // our Get_member beats any base getter
				else if (value != GETMEM && haveGetter("Get_"))
					value = GETTER; // our Get_ beats base Get_
			}
		}
		return value;
	}

	public Object get3(Object member) {
		if (vars == null)
			return null;
		Object value = vars.get(member);
		if (value == null && member instanceof String) {
			value = findMethod(member);
			vars.put((String) member, value); // cache for next time
		}
		return value == NOMETHOD ? null : value;
	}

	private Object findMethod(Object method) {
		for (FunctionSpec f : params)
			if (f.name == method)
				return new Method(this);
		return NOMETHOD;
	}

	private boolean haveGetter(String getter) {
		Object value = vars.get(getter);
		if (value != null)
			return value == GETTER ? true : false; // cached from last time
		value = findMethod(getter);
		if (!(value instanceof Method))
			return false;
		vars.put(getter, GETTER); // cache for next time
		return true;
	}

	private Object getBase(Object member) {
		if (baseGlobal == null)
			return null;
		Object base = Globals.get(baseGlobal);
		if (!(base instanceof SuClass))
			throw new SuException("base must be class");
		return ((SuClass) base).get2(member);
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
	public Object invoke(Object self, String method, Object... args) {
		// if the original method call was to an instance
		// then self will be the instance (not the class)
		if (method == "<new>")
			return newInstance(args);
		if (method == "Base")
			return Base(self, args);
		if (method == "Base?")
			return BaseQ(self, args);
		if (method == "Eval")
			return ContainerMethods.Eval(self, args);
		if (method == "GetDefault")
			return GetDefault(self, args);
		if (method == "Members")
			return Members(self, args);
		if (method == "Member?")
			return MemberQ(self, args);
		if (method == "Method?")
			return MethodQ(self, args);
		if (method == "MethodClass")
			return MethodClass(self, args);
		if (method == "Type")
			return "Class";

		if (baseGlobal != null)
			return base().invoke(self, method, args);

		if (method == "_init")
			return init(args);
		if (method == "CallClass")
			return Ops.invoke(self, "<new>", args);
		if (method != "Default") {
			// if we get here, method was not found
			// add method to beginning of args and call Default
			Object newargs[] = new Object[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = method;
			return Ops.invoke(self, "Default", newargs);
			// COULD make a defaultMethod and bypass invoke (like "call")
		}
		throw methodNotFound((String) args[0]);
	}

	private static Object init(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return null;
	}

	private Object newInstance(Object[] args) {
		SuInstance x = new SuInstance(this);
		x.invoke(x, "_init", args);
		return x;
	}

	private SuValue base() {
		Object base = Globals.get(baseGlobal);
		if (!(base instanceof SuValue))
			throw new SuException("class base must be a Suneido value");
		return (SuValue) base;
	}

	private Object Base(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (baseGlobal == null)
			return Boolean.FALSE; // TODO Base should return Object class
		return Globals.get(baseGlobal);
	}

	private Object BaseQ(Object self, Object[] args) {
		args = Args.massage(FunctionSpec.value, args);
		if (args[0] == this)
			return Boolean.TRUE;
		if (baseGlobal == null)
			return Boolean.FALSE;
		return base().invoke("Base?", args);
	}

	private static final FunctionSpec keyValueFS =
			new FunctionSpec("key", "block");

	private Object GetDefault(Object self, Object[] args) {
		args = Args.massage(keyValueFS, args);
		String key = Ops.toStr(args[0]);
		if (vars.containsKey(key))
			return vars.get(key);
		Object x = args[1];
		if (x instanceof SuBlock)
			x = Ops.call(x);
		return x;
	}

	private SuContainer Members(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		SuContainer c = new SuContainer();
		for (Map.Entry<String, Object> e : vars.entrySet())
			if (e.getValue() != null && !(e.getValue() instanceof Marker))
				c.append(e.getKey());
		for (FunctionSpec fs : params)
			if (fs.name != "_init") // TODO skip blocks & nested functions
				c.append(fs.name);
		return c;
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	private Boolean MemberQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		Object x = get2(key);
		return x == null || x instanceof Marker ? Boolean.FALSE : Boolean.TRUE;
	}

	private Object MethodClass(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		Object x = get2(key);
		if (x instanceof Method)
			return ((Method) x).c;
		else
			return Boolean.FALSE;
	}

	private Boolean MethodQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		Object x = get2(key);
		return x instanceof Method;
	}

	@Override
	public String typeName() {
		return "Class";
	}

	@Override
	public String toString() {
		return super.typeName();
	}

	protected Object superInvoke(Object self, String member, Object... args) {
		if (baseGlobal == null) {
			if (member == "_init")
				return null;
			else
				throw new SuException("must have base class to use super");
		}
		return base().invoke(self, member, args);
	}

	protected Object superInvokeN(Object self, String member) {
		return superInvoke(self, member);
	}
	protected Object superInvokeN(Object self, String member, Object a) {
		return superInvoke(self, member, a);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b) {
		return superInvoke(self, member, a, b);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c) {
		return superInvoke(self, member, a, b, c);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d) {
		return superInvoke(self, member, a, b, c, d);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e) {
		return superInvoke(self, member, a, b, c, d, e);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f) {
		return superInvoke(self, member, a, b, c, d, e, f);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
		return superInvoke(self, member, a, b, c, d, e, f, g);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y);
	}
	protected Object superInvokeN(Object self, String member, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y, Object z) {
		return superInvoke(self, member, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z);
	}

}
