/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.SuContainer;
import suneido.SuValue;

public abstract class VirtualContainer extends SuValue {
	abstract protected SuContainer value();

	@Override
	public boolean equals(Object other) {
		return value().equals(other);
	}

	@Override
	public int hashCode() {
		return value().hashCode();
	}

	@Override
	public String toString() {
		return value().toString();
	}

	@Override
	public void pack(ByteBuffer buf) {
		value().pack(buf);
	}

	@Override
	public int packSize(int nest) {
		return value().packSize(nest);
	}

	@Override
	public Object get(Object key) {
		return value().get(key);
	}

	@Override
	public SuContainer toContainer() {
		return value();
	}
	
	@Override
	public SuValue lookup(String method) {
		return value().lookup(method);
	}
	
	@Override
	public Object eval(Object self, Object... args) {
		return super.eval(value(), args);
	}

}
