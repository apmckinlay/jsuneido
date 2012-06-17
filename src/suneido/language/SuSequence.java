/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;

/**
 * A wrapper for an Iterable that can be treated like an SuContainer but doesn't
 * instantiate the object if you just iterate over it.
 */
public class SuSequence extends SuContainer {
	private final Iterable<Object> iterable;
	private final Iterator<Object> iter;
	private boolean instantiated = false;

	public SuSequence(Iterable<Object> iterable) {
		this.iterable = iterable;
		iter = iterable.iterator();
	}

	@Override
	public SuValue lookup(String method) {
		if (method == "Next")
			return Next;
		if (method == "Copy" && ! instantiated)
			return Copy;
		instantiate();
		return super.lookup(method);
	}

	private static final SuValue Next = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			Iterator<Object> iter = ((SuSequence) self).iter;
			return iter.hasNext() ? iter.next() : this;
		}
	};

	private static final SuValue Copy = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			return new SuContainer(((SuSequence) self).iterable);
		}
	};

	// TODO handle compareTo (also in Ops.cmp)

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

	private void instantiate() {
		if (instantiated)
			return;
		addAll(iterable);
		instantiated = true;
	}

	@Override
	public Iterator<Object> iterator() {
		return instantiated ? super.iterator() : iterable.iterator();
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
