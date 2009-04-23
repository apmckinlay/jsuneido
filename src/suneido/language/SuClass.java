package suneido.language;

import static suneido.language.SuClass.Marker.*;

import java.util.Map;

import suneido.SuException;
import suneido.SuValue;

/**
 * The Java base class for compiled Suneido classes.
 * @see SuMethod
 * @see SuFunction
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuClass extends SuCallable {
	protected String baseGlobal = null;
	protected Map<String, Object> vars;
	protected boolean hasGetters = false;

	// NOMETHOD is used instead of null
	// to differentiate from map.get returning null for missing
	static enum Marker {
		NOMETHOD, METHOD, GETTER, GETMEM
	};

	@Override
	public Object get(Object member) {
		Object value = get2(member);
		if (value == GETTER)
			value = invoke(this, "Get_", member);
		else if (value == GETMEM)
			value = invoke(this, ("Get_" + (String) member).intern());
		else if (value == METHOD)
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

	private Object get3(Object member) {
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
				return METHOD;
		return NOMETHOD;
	}

	private boolean haveGetter(String getter) {
		Object value = vars.get(getter);
		if (value != null)
			return value == GETTER ? true : false; // cached from last time
		value = findMethod(getter);
		if (value != METHOD)
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
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>" || method == "CallClass")
			return newInstance(args);
		else if (method == "Type")
			return "Class";
		// TODO other standard methods on classes
		else if (baseGlobal != null)
			return base().invoke(self, method, args);
		else if (method == "_init")
			return init(args);
		else if (method != "Default") {
			// if we get here, method was not found
			// add method to beginning of args and call Default
			Object newargs[] = new Object[1 + args.length];
			System.arraycopy(args, 0, newargs, 1, args.length);
			newargs[0] = method;
			return Ops.invoke(self, "Default", newargs);
			// COULD make a defaultMethod and bypass invoke (like "call")
		} else
			// method == "Default"
			throw unknown_method((String) args[0]);
	}

	private Object newInstance(Object[] args) {
		SuInstance x = new SuInstance(this);
		x.invoke(x, "_init", args);
		return x;
	}

	private static Object init(Object[] args) {
		args = Args.massage(FunctionSpec.noParams, args);
		if (args.length != 0)
			throw new SuException("wrong number of arguments");
		return null;
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
	protected Object superInvokeN(Object self, String member, Object a,
			Object b, Object c) {
		return superInvoke(self, member, a, b, c);
	}
	// TODO more args

	protected Object superInvoke(Object self, String member, Object... args) {
		if (baseGlobal == null) {
			if (member == "_init")
				return null;
			else
				throw new SuException("must have base class to use super");
		}
		return base().invoke(self, member, args);
	}

	private SuValue base() {
		Object base = Globals.get(baseGlobal);
		if (!(base instanceof SuValue))
			throw new SuException("class base must be a Suneido value");
		return (SuValue) base;
	}

}
