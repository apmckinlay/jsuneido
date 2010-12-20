/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.HashMap;
import java.util.Map;

import suneido.*;
import suneido.language.builtin.ContainerMethods;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

/**
 * An instance of a Suneido class
 * (which will be an instance of {@link SuClass})
 */
public class SuInstance extends SuValue {
	final SuValue myclass;
	private final Map<String, Object> ivars;
	private static final Map<String, SuMethod> methods = methods();

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
		return myclass.lookup("Call").eval(this, args);
	}
	// TODO call0...9

	// TODO use BuiltinMethods
	private static Map<String, SuMethod> methods() {
		ImmutableMap.Builder<String, SuMethod> b = ImmutableMap.builder();
		b.put("Base", new Base());
		b.put("Base?", new BaseQ());
		b.put("Copy", new Copy());
		b.put("Delete", new Delete());
		b.put("Eval", new ContainerMethods.Eval());
		b.put("Eval2", new ContainerMethods.Eval2());
		b.put("Member?", new MemberQ());
		b.put("Members", new Members());
		return b.build();
	}

	@Override
	public SuValue lookup(String method) {
		SuMethod m = methods.get(method);
		if (m != null)
			return m;
		return ((SuClass) myclass).lookup(method);
	}

	public static class Base extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuInstance) self).myclass;
		}
	}

	public static class BaseQ extends SuMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			SuValue c = ((SuInstance) self).myclass;
			if (c == a)
				return true;
			return c.lookup("Base?").eval1(this, a);
		}
	}

	public static class Copy extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
	        return new SuInstance((SuInstance) self);
        }
	}

	public static class Delete extends SuMethod1 {
		{ params = new FunctionSpec("key"); }
		@Override
		public Object eval1(Object self, Object a) {
			return ((SuInstance) self).ivars.remove(a) == null ? false : this;
		}
	}

	public static class MemberQ extends SuMethod1 {
		{ params = new FunctionSpec("key"); }
		@Override
		public Object eval1(Object self, Object a) {
			return ((SuInstance) self).hasMember(Ops.toStr(a));
		}
	}

	private Object hasMember(String key) {
		if (ivars.containsKey(key))
			return true;
		return myclass.lookup("Member?").eval1(this, key);
	}

	public static class Members extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return new SuContainer(((SuInstance) self).ivars.keySet());
		}
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
