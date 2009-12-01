package suneido;

import static suneido.SuException.methodNotFound;

import java.nio.ByteBuffer;

import suneido.language.SuClass;

/**
 * Base class for Suneido data types:
 * @see SuContainer
 * @see SuRecord
 * @see SuClass
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public abstract class SuValue implements Packable {

	@Override
	public String toString() {
		return "a" + typeName();
	}

	public int hashCode(int nest) {
		return hashCode();
	}

	public Object call(Object... args) {
		throw new SuException("can't call " + typeName());
	}

	public Object invoke(String method, Object... args) {
		return invoke(this, method, args);
	}
	public Object invoke(Object self, String method, Object... args) {
		throw methodNotFound(self, method);
	}

	public Object eval(Object self, Object... args) {
		throw new SuException("can't eval " + typeName());
	}

	public String typeName() {
		String s = getClass().getName();
		if (s.startsWith("suneido.language.")) {
			s = s.substring(17);
			if (s.startsWith("builtin."))
				s = s.substring(8);
			if (s.endsWith("Instance"))
				s = s.substring(0, s.length() - 8);
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

	public int packSize(int nest) {
		throw new SuException(typeName() + " cannot be stored");
	}

	public void pack(ByteBuffer buf) {
		throw new SuException(typeName() + " cannot be stored");
	}

	public SuContainer toContainer() {
		return null;
	}

}
