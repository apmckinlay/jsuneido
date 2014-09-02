/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static java.lang.Boolean.FALSE;

import java.util.Iterator;
import java.util.NoSuchElementException;

import suneido.runtime.*;

public final class Seq extends BuiltinClass {
	private static final FunctionSpec initFS =
			FunctionSpec.from("from, to = false, by = 1");

	@Override
	public Object newInstance(Object... args) {
		args = Args.massage(initFS, args);
		return new SuSequence(new SuSeq(args[0], args[1], args[2]));
	}

	private static class SuSeq implements Iterable<Object> {
		private final Object from;
		private final Object to;
		private final Object by;

		SuSeq(Object from, Object to, Object by) {
			this.from = (to == FALSE) ? 0 : from;
			this.to = (to == FALSE) ? from : to;
			this.by = by;
		}

		@Override
		public String toString() {
			return "Seq(" + from + ", " + to + ", " + by + ")";
		}

		@Override
		public Iterator<Object> iterator() {
			return new Iter();
		}

		class Iter implements Iterator<Object> {
			Object i = from;

			@Override
			public boolean hasNext() {
				return Ops.cmp(i, to) < 0;
			}

			@Override
			public Object next() {
				if (! hasNext())
					throw new NoSuchElementException();
				Object x = i;
				i = Ops.add(i, by);
				return x;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		}

	}

}
