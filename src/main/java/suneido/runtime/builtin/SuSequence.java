/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.runtime.*;

/**
 * A wrapper for a Suneido iterator (with Next)
 * that can be treated like an SuContainer
 * but doesn't instantiate the object if you just iterate over it.
 * See also: {@link Sequence} which wraps a Java iterable
 */
public class SuSequence extends SequenceBase {
	private final Object iter;

	@Params("iter")
	public static Object Sequence(Object a) {
		return new SuSequence(a);
	}

	public SuSequence(Object iter) {
		this.iter = iter;
	}

	@Override
	protected Object iter() {
		duped = true;
		return Ops.invoke0(iter, "Dup");
	}

	@Override
	protected SuValue copy() {
		Object iter = iter();
		SuContainer c = new SuContainer();
		Object x;
		while (iter != (x = Ops.invoke0(iter, "Next")))
			c.add(x);
		return c;
	}

	@Override
	protected void instantiate() {
		Object x;
		while (iter != (x = Ops.invoke0(iter, "Next")))
			add(x);
	}

	/** Used by for-in loops */
	@Override
	public Iterator<Object> iterator() {
		return instantiated
				? super.iterator()
				: new IterStoJ(iter());
	}

	@Override
	protected boolean infinite() {
		try {
			return Ops.toBoolean_(Ops.invoke0(iter, "Infinite?"));
		} catch(Throwable e) {
			return false;
		}
	}

	@Override
	public String toString() {
		if (infinite())
			return "infiniteSequence";
		return super.toString();
	}

}
