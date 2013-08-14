/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.SuException.methodNotFound;

import java.nio.ByteBuffer;

import suneido.language.Ops;
import suneido.language.SuCallable;
import suneido.language.SuClass;

/**
 * Base class for Suneido data types:
 * @see SuContainer
 * @see SuRecord
 * @see SuClass
 */
public abstract class SuValue implements Packable {

	@Override
	public String toString() {
		return "a" + typeName();
	}

	/**
	 * used for non-method calls
	 */
	public Object call(Object... args) {
		throw new SuException("can't call " + typeName());
	}

	/**
	 * used for method calls
	 */
	public Object eval(Object self, Object... args) {
		throw new SuException("can't call " + typeName());
	}

	public String typeName() {
		String s = getClass().getName();
		if (s.startsWith("suneido.language.")) {
			s = s.substring(17);
			if (s.startsWith("builtin."))
				s = s.substring(8);
			if (s.endsWith("Instance"))
				s = s.substring(0, s.length() - 8);
			if (s.endsWith("$"))
				s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	public Object get(Object member) {
		throw new SuException(typeName() + " " + this
				+ " does not support get " + member);
	}

	public void put(Object member, Object value) {
		throw new SuException(typeName() + " does not support put");
	}

	public int packSize() {
		return packSize(0);
	}

	@Override
	public int packSize(int nest) {
		throw new SuException(typeName() + " cannot be stored");
	}

	@Override
	public void pack(ByteBuffer buf) {
		throw new SuException(typeName() + " cannot be stored");
	}

	public SuContainer toContainer() {
		return null;
	}

	public static boolean isCallable(Object x) {
		return x instanceof SuValue && ((SuValue) x).isCallable();
	}
	protected boolean isCallable() {
		return false;
	}

	public Object call0() {
		return call();
	}
	public Object call1(Object a) {
		return call(a);
	}
	public Object call2(Object a, Object b) {
		return call(a, b);
	}
	public Object call3(Object a, Object b, Object c) {
		return call(a, b, c);
	}
	public Object call4(Object a, Object b, Object c, Object d) {
		return call(a, b, c, d);
	}

	public Object eval0(Object self) {
		return eval(self);
	}
	public Object eval1(Object self, Object a) {
		return eval(self, a);
	}
	public Object eval2(Object self, Object a, Object b) {
		return eval(self, a, b);
	}
	public Object eval3(Object self, Object a, Object b,
			Object c) {
		return eval(self, a, b, c);
	}
	public Object eval4(Object self, Object a, Object b,
			Object c, Object d) {
		return eval(self, a, b, c, d);
	}

	/** used by {@link Ops} invoke to get a named method */
	public SuValue lookup(String method) {
		return new NotFound(method);
	}

	public static class NotFound extends SuCallable {
		String method;
		public NotFound(String method) {
			this.method = method;
		}
		@Override
		public Object eval(Object self, Object... args) {
			throw methodNotFound(self, method);
		}
	}

}
