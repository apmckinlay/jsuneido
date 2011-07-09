/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.*;

/**
 * Add items in a random order, iterate them in order.
 * Fast (amortized) insertion.
 * Not very good for lookups (so none implemented)
 * Cache oblivious.
 * Cache friendly - mostly sequential access.
 * Optimal memory space (e.g. as opposed to array size doubling)
 */
public class FractalTree<T> implements Iterable<T> {
	private static final int MAX_LEVELS = 32;
	private final Object[][] nodes = new Object[MAX_LEVELS][];
	private static final int CACHED_LEVELS = 4;
	private static final Object[][] tmp = new Object[][] {
		new Object[1], new Object[2], new Object[4], new Object[8] };
	private static final Object[][] cache = new Object[][] {
		new Object[1], new Object[2], new Object[4], new Object[8] };

	public void add(T x) {
		if (nodes[0] == null) {
			nodes[0] = cache[0];
			nodes[0][0] = x;
			return;
		}
		tmp[0][0] = x;
		int i = 1;
		for (; i < CACHED_LEVELS; ++i) {
			if (nodes[i] == null) {
				nodes[i] = merge(nodes[i - 1], tmp[i - 1], cache[i]);
				nodes[i - 1] = null;
				return;
			} else {
				merge(nodes[i - 1], tmp[i - 1], tmp[i]);
				nodes[i - 1] = null;
			}
		}
		--i;
		Object[] tmp2 = tmp[i];
		for (; nodes[i] != null; ++i) {
			tmp2 = merge(nodes[i], tmp2);
			nodes[i] = null;
		}
		nodes[i] = tmp2;
	}

	private Object[] merge(Object[] x, Object[] y) {
		return merge(x, y, new Object[x.length + y.length]);
	}
	private Object[] merge(Object[] x, Object[] y, Object[] z) {
		int xi = 0;
		int yi = 0;
		for (int zi = 0; zi < z.length; ++zi)
			if (yi >= y.length || (xi < x.length && cmp(x[xi], y[yi]) < 0))
				z[zi] = x[xi++];
			else
				z[zi] = y[yi++];
		return z;
	}

	@SuppressWarnings("unchecked")
	private int cmp(Object x, Object y) {
		return ((Comparable<Object>) x).compareTo(y);
	}

	@Override
	public Iterator<T> iterator() {
		return new Iter();
	}

	private class Iter implements Iterator<T> {
		private final ArrayList<NodeIter> lists = new ArrayList<NodeIter>();

		Iter() {
			for (int i = 0; i < MAX_LEVELS; ++i)
				if (nodes[i] != null)
					lists.add(new NodeIter(nodes[i]));
		}

		@Override
		public boolean hasNext() {
			return ! lists.isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T next() {
			int iMinList = 0;
			Object minValue = lists.get(0).peek();
			for (int i = 1; i < lists.size(); ++i)
				if (cmp(lists.get(i).peek(), minValue) < 0)
					minValue = lists.get(iMinList = i).peek();
			if (! lists.get(iMinList).next())
				lists.remove(iMinList);
			return (T) minValue;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private static class NodeIter {
		Object[] node;
		int pos = 0;

		NodeIter(Object[] node) {
			this.node = node;
		}

		Object peek() {
			return node[pos];
		}

		boolean next() {
			return ++pos < node.length;
		}
	}

	public void print() {
		for (int i = 0; i < MAX_LEVELS; ++i)
			if (nodes[i] != null)
				System.out.println((1 << i) + ": " + Arrays.toString(nodes[i]));
	}

}
