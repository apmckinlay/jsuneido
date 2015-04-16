/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;

/**
 * A wrapper for an Iterable that can be treated like an SuContainer but doesn't
 * instantiate the object if you just iterate over it.
 * See also: {@link SuSequence} which wraps a Suneido iterator
 */
public class Sequence extends SequenceBase {
	private final Iterable<Object> iterable;
	private final Iterator<Object> iter;
	private boolean instantiated = false;

	public Sequence(Iterable<Object> iterable) {
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

	private static final SuValue Next = new SuBuiltinMethod0("seq.Next") {
		@Override
		public Object eval0(Object self) {
			Iterator<Object> iter = ((Sequence) self).iter;
			return iter.hasNext() ? iter.next() : this;
		}
	};

	private static final SuValue Copy = new SuBuiltinMethod0("seq.Copy") {
		@Override
		public Object eval0(Object self) {
			// avoid two copies (instantiate & copy)
			// for common usage: for m in ob.Members().Copy()
			Sequence seq = (Sequence) self;
			return seq.instantiated
					? new SuContainer(seq)
					: new SuContainer(seq.iterable);
		}
	};

	@Override
	protected void instantiate() {
		if (instantiated)
			return;
		addAll(iterable);
		instantiated = true;
	}

	@Override
	public Iterator<Object> iterator() {
		return instantiated ? super.iterator() : iterable.iterator();
	}

}
