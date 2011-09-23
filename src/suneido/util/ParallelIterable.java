/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;

public class ParallelIterable<T> implements Iterable<List<T>> {
	private final Iterable<T>[] srcs;

	public ParallelIterable(Iterable<T>... srcs) {
		this.srcs = srcs;
	}

	public static <T> ParallelIterable<T> of(Iterable<T>... srcs) {
		return new ParallelIterable<T>(srcs);
	}

	@Override
	public Iterator<List<T>> iterator() {
		return new ParallelIterator<T>(srcs);
	}

	private static class ParallelIterator<T> extends UnmodifiableIterator<List<T>> {
		private final List<Iterator<T>> srcs;

		public ParallelIterator(Iterable<T>... args) {
			srcs = Lists.newArrayListWithCapacity(args.length);
			for (Iterable<T> iterable : args)
				srcs.add(iterable.iterator());
		}

		@Override
		public boolean hasNext() {
			for (Iterator<T> iter : srcs)
				if (iter.hasNext())
					return true;
			return false;
		}

		@Override
		public List<T> next() {
			List<T> x = Lists.newArrayListWithCapacity(srcs.size());
			for (Iterator<T> iter : srcs)
				x.add(iter.hasNext() ? iter.next() : null);
			return x;
		}

	}

}