package suneido.language;

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

	static class Get {
		protected final Object getter;
		public Get(Object getter) {
			this.getter = getter;
		}
		public Object call() {
			return ((SuMethod) getter).call();
		}
		@Override
		public String toString() {
			return "Get(" + getter + ")";
		}
	}
	static class GetMem extends Get {
		public GetMem(Object getter) {
			super(getter);
		}
		@Override
		public String toString() {
			return "GetMem(" + getter + ")";
		}
	}

	@Override
	public Object get(Object member) {
		Object value = get2(member);
		if (value instanceof Get)
			value = ((Get) value).call();
		else if (value == null)
			throw new SuException("member not found: " + member);
		return value;
	}

	private Object get2(Object member) {
		Object value = get3(member);
		if (value != null)
			return value;
		value = getBase(member);
		if (hasGetters) {
			String getter = ("Get_" + member).intern();
			if (value == null || value instanceof Get) {
				Object ourGetter = findGetter(getter);
				if (ourGetter != null)
					value = new GetMem(ourGetter); // our Get_member beats any base getter
				else if (!(value instanceof GetMem)
						&& null != (ourGetter = findGetter("Get_")))
					value = new Get(ourGetter); // our Get_ beats base Get_
			}
		}
		return value;
	}

	private static final Object NOMETHOD = new Object();

	private Object get3(Object member) {
		Object value = vars.get(member);
		if (value == null && member instanceof String) {
			value = findMethod(member);
			vars.put((String) member, value); // cache it for next time
		}
		return value == NOMETHOD ? null : value;
	}

	private Object findMethod(Object method) {
		for (FunctionSpec f : params)
			if (f.name == method)
				return new SuMethod(this, f.name);
		return NOMETHOD;
	}

	private Object findGetter(String getter) {
		Object value = get3(getter);
		if (!(value instanceof SuMethod))
			return null;
		SuMethod m = (SuMethod) value;
		assert m.method.equals(getter);
		return m.instance == this ? m : null;
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
		else if (method == "_init")
			return init(args);
		else if (method == "Type")
			return "Class";
		// TODO other standard methods on classes
		else if (baseGlobal != null)
			return base().invoke(self, method, args);
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
		// TODO massage args (shouldn't be any)
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
