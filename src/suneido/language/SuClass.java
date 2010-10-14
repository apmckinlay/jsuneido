package suneido.language;

import static suneido.SuException.methodNotFound;

import java.util.Collections;
import java.util.Map;

import suneido.*;
import suneido.language.builtin.ContainerMethods;

/**
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuClass extends SuValue {
	private final String className;
	private final String baseGlobal;
	private final Map<String, Object> members; // must be synchronized
	private boolean hasGetters = true; // till we know different

	@SuppressWarnings("unchecked")
	public SuClass(String className, String baseGlobal, Object members) {
		this.className = className;
		this.baseGlobal = baseGlobal;
		this.members = (Map<String, Object>) (members == null ? Collections.emptyMap() : members);
		linkMethods();
	}

	private void linkMethods() {
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
					? new SuMethod(self, (SuFunction) value) : value;
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
		if (value != null || baseGlobal == null)
			return value;
		return base().get2(member);
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

		Object fn = get2(method);
		if (fn instanceof SuFunction)
			return ((SuFunction) fn).eval(self, args);

		if (method == "New")
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
		throw methodNotFound(self, (String) args[0]);
	}

	private static Object init(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return null;
	}

	private Object newInstance(Object... args) {
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

	private Object Base(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		if (baseGlobal == null)
			return Boolean.FALSE; // TODO Base should return Object class
		return Globals.get(baseGlobal);
	}

	private Object BaseQ(Object self, Object[] args) {
		args = Args.massage(FunctionSpec.value, args);
		return hasBase(args[0]);
	}

	private Boolean hasBase(Object base) {
		if (base == this)
			return Boolean.TRUE;
		if (baseGlobal == null)
			return Boolean.FALSE;
		return base().hasBase(base);
	}

	private static final FunctionSpec keyValueFS =
			new FunctionSpec("key", "block");

	private Object GetDefault(Object self, Object[] args) {
		args = Args.massage(keyValueFS, args);
		String key = Ops.toStr(args[0]);
		if (members.containsKey(key))
			return members.get(key);
		Object x = args[1];
		if (x instanceof SuBlock)
			x = Ops.call(x);
		return x;
	}

	private SuContainer Members(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		SuContainer c = new SuContainer();
		for (Map.Entry<String, Object> e : members.entrySet())
			if (e.getValue() != null)
				c.append(e.getKey());
		return c;
	}

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	private Boolean MemberQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		Object x = get2(key);
		return x == null ? Boolean.FALSE : Boolean.TRUE;
	}

	private Object MethodClass(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String method = Ops.toStr(args[0]);
		return methodClass(method);
	}

	private Object methodClass(String method) {
		Object value = members.get(method);
		if (value instanceof SuFunction)
			return this;
		if (value == null && baseGlobal != null)
			return base().methodClass(method);
		return Boolean.FALSE;
	}

	private Boolean MethodQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		Object x = get2(key);
		return x instanceof SuFunction;
	}

	@Override
	public String typeName() {
		return "Class";
	}

	@Override
	public String toString() {
		return className;
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
