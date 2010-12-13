/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.HashMap;
import java.util.Map;

import suneido.*;
import suneido.language.builtin.ContainerMethods;

import com.google.common.base.Objects;

/**
 * An instance of a Suneido class
 * (which will be an instance of {@link SuClass})
 */
public class SuInstance extends SuValue {
	private final SuValue myclass;
	private final Map<String, Object> ivars;

	public SuInstance(SuValue myclass) {
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
		return myclass.invoke(this, "Call", args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		// self will normally be the same as "this"
		// except when object.Eval is used
		if (method == "Base")
			return Base(self, args);
		if (method == "Delete")
			return Delete(self, args);
		if (method == "Copy")
			return Copy(self, args);
		if (method == "Eval")
			return ContainerMethods.Eval(self, args);
		if (method == "Eval2")
			return ContainerMethods.Eval2(self, args);
		if (method == "Members")
			return Members(self, args);
		if (method == "Member?")
			return MemberQ(self, args);
		return myclass.invoke(self, method, args);
	}

	private Object Base(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return myclass;
	}

	private Object Copy(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
	        return new SuInstance(this);
        }

	private static final FunctionSpec keyFS = new FunctionSpec("key");

	private Object Delete(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		return ivars.remove(args[0]) == null ? false : this;
	}

	private Object MemberQ(Object self, Object[] args) {
		args = Args.massage(keyFS, args);
		String key = Ops.toStr(args[0]);
		if (ivars.containsKey(key))
			return true;
		return myclass.invoke(myclass, "Member?", args);
	}

	private Object Members(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(ivars.keySet());
	}

	@Override
	public String toString() {
		if (myclass != null && myclass instanceof SuClass) {
			Object toString = ((SuClass) myclass).get2("ToString");
			if (toString instanceof SuCallable)
				return Ops.toStr(((SuCallable) toString).eval(this));
		}
		return myclass + "()";
	}

	@Override
	public Object get(Object member) {
		Object value = ivars.get(member);
		if (value != null)
			return value;
		return ((SuClass) myclass).get(this, member);
	}

	@Override
	public void put(Object member, Object value) {
		if (! Ops.isString(member))
			throw new SuException("non-string member name: "
					+ Ops.typeName(member));
		ivars.put(member.toString(), value);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof SuInstance))
			return false;
		SuInstance that = (SuInstance) other;
		return this.myclass == that.myclass &&
				Objects.equal(ivars, that.ivars);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(myclass, ivars);
	}

	@Override
	public String typeName() {
		return "Object";
	}

}
