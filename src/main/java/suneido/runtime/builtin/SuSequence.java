/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Iterator;

import suneido.SuContainer;
import suneido.SuValue;
import suneido.runtime.*;

import com.google.common.collect.AbstractIterator;

/**
 * A wrapper for a Suneido iterator (a class with Next and Rewind)
 * that can be treated like an SuContainer
 * but doesn't instantiate the object if you just iterate over it.
 * See also: {@link Sequence} which wraps a Java iterable
 */
public class SuSequence extends SequenceBase {
	private final Object iter;
	private boolean instantiated = false;

	@Params("iter")
	public static Object Sequence(Object a) {
		return new SuSequence(a);
	}

	public SuSequence(Object iter) {
		this.iter = iter;
	}

	@Override
	public SuValue lookup(String method) {
		if (method == "Next")
			return Next;
		if (method == "Copy" && ! instantiated)
			return Copy;
		if (iter instanceof SuInstance) {
			Object x = ((SuInstance) iter).classGet(method);
			if (x instanceof SuCallable)
				return new Invoke(method);
		}
		instantiate();
		return super.lookup(method);
	}

	private static final SuValue Next = new SuBuiltinMethod0("sequence.Next") {
		@Override
		public Object eval0(Object self) {
			SuSequence seq = ((SuSequence) self);
			return Ops.invoke0(seq.iter, "Next");
		}
	};

	/** Copy() builds an object but doesn't instantiate */
	private static final SuValue Copy = new SuBuiltinMethod0("sequence.Copy") {
		@Override
		public Object eval0(Object self) {
			Object iter = ((SuSequence) self).iter;
			SuContainer c = new SuContainer();
			Ops.invoke0(iter, "Rewind");
			Object x;
			while (iter != (x = Ops.invoke0(iter, "Next")))
				c.add(x);
			return c;
		}
	};

	/** Used to delegate method calls from SuSequence to it's iter */
	private class Invoke extends SuBuiltinMethod {
		private final String method;

		public Invoke(String method) {
			super("sequence.invoke", new FunctionSpec("@args"));
			this.method = method;
		}

		@Override
		public Object eval(Object self, Object... args) {
			return Ops.invoke(iter, method, args);
		}
	};

	@Override
	protected void instantiate() {
		if (instantiated)
			return;
		Ops.invoke0(iter, "Rewind");
		Object x;
		while (iter != (x = Ops.invoke0(iter, "Next")))
			add(x);
		instantiated = true;
	}

	/** Used by for-in loops */
	@Override
	public Iterator<Object> iterator() {
		return instantiated
				? super.iterator()
				: new Iter(iter);
	}

	/** Adapt iterator from Suneido to Java style */
	private static class Iter extends AbstractIterator<Object> {
		private final Object iter;

		Iter(Object iter) {
			this.iter = Ops.invoke0(iter, "Copy");
			Ops.invoke0(this.iter, "Rewind");
		}

		@Override
		protected Object computeNext() {
			Object x = Ops.invoke0(iter, "Next");
			return x != iter ? x : endOfData();
		}
	}

}
