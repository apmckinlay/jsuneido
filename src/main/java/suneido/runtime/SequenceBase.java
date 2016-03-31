/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.SuContainer;

public abstract class SequenceBase extends SuContainer {

	abstract protected void instantiate();

	@Override
	public boolean equals(Object other) {
		instantiate();
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		instantiate();
		return super.hashCode();
	}

	@Override
	public String toString() {
		instantiate();
		return super.toString();
	}

	@Override
	public void pack(ByteBuffer buf) {
		instantiate();
		super.pack(buf);
	}

	@Override
	public int packSize(int nest) {
		instantiate();
		return super.packSize(nest);
	}

	@Override
	public Object get(Object key) {
		instantiate();
		return super.get(key);
	}

	@Override
	public SuContainer toContainer() {
		instantiate();
		return this;
	}

}