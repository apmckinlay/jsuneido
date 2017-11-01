/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.runtime.builtin.SuSequence;

/**
 * A wrapper for a Java Iterable that can be treated like an SuContainer
 * but doesn't instantiate the object if you just iterate over it.
 * See also: {@link SuSequence} which wraps a Suneido iterator
 */
public class Sequence extends SequenceBase {
	private final Iterable<Object> iterable;

	public Sequence(Iterable<Object> iterable) {
		this.iterable = iterable;
	}

	@Override
	protected SuValue copy() {
		return new SuContainer(iterable);
	}

	@Override
	protected void instantiate() {
		addAll(iterable);
	}

	@Override
	protected Object iter() {
		duped = true;
		return new IterJtoS(iterable);
	}

	// called by for-in loops
	@Override
	public Iterator<Object> iterator() {
		duped = true;
		return instantiated ? super.iterator() : iterable.iterator();
	}

	@Override
	protected boolean infinite() {
		return (iterable instanceof Infinitable &&
				((Infinitable) iterable).infinite());
	}

	public interface Infinitable {
		boolean infinite();
	}

	@Override
	public String toString() {
		if (infinite())
			return "infiniteSequence";
		return super.toString();
	}

}
