/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Add items in a random order, iterate them in order.
 * Fast (amortized) insertion.
 * Not very good for lookups (so none implemented)
 * Cache oblivious.
 * Cache friendly - mostly sequential access.
 * Optimal memory space (e.g. as opposed to array size doubling)
 */
public class MergeTree<T> implements Iterable<T> {
	private static final int MAX_LEVELS = 32;
	private final Object[][] nodes = new Object[MAX_LEVELS][];
	@SuppressWarnings("unchecked")
	private final SoftReference<Object[]>[] cache = new SoftReference[MAX_LEVELS];
	private final Object[] tmp = new Object[1];
	private final int[] pos = new int[MAX_LEVELS];
	private final Comparator<T> cmp;
	private int size = 0;

	public MergeTree() {
		this(new Comparator<T>() {
				@SuppressWarnings("unchecked")
				@Override
				public int compare(T x, T y) {
					return ((Comparable<T>) x).compareTo(y);
				}
			});
	}

	public MergeTree(Comparator<T> cmp) {
		this.cmp = cmp;
	}

	public void add(T x) {
		assert x != null;
		++size;
		merge(x, firstUnused());
	}

	private int firstUnused() {
		int firstUnused = 0;
		while (nodes[firstUnused] != null)
			firstUnused++;
		return firstUnused;
	}

	private void merge(T x, int n) {
		assert nodes[n] == null;
		Object[] dst = alloc(n);
		if (n == 0) { // 50% of the cases
			dst[0] = x;
			return;
		}
		if (n == 1) { // another 25% of the cases
			if (cmp(x, nodes[0][0]) < 0) {
				dst[0] = x;
				dst[1] = nodes[0][0];
			} else {
				dst[0] = nodes[0][0];
				dst[1] = x;
			}
			nodes[0] = null;
			return;
		}
		merge(x, n, dst);
	}

	private void merge(T x, int n, Object[] dst) {
		int di = 0;
		tmp[0] = x;
		nodes[n] = tmp;
		Arrays.fill(pos, 0, n + 1, 0);
		while (true) {
			int iMin = 0;
			Object min = null;
			for (int i = 0; i <= n; ++i)
				if (pos[i] < nodes[i].length) {
					Object val = nodes[i][pos[i]];
					assert val != null;
					if (min == null || cmp(val, min) < 0) {
						min = val;
						iMin = i;
					}
				}
			if (min == null)
				break;
			++pos[iMin];
			dst[di++] = min;
		}
		assert di == dst.length;
		Arrays.fill(nodes, 0, n, null);
		nodes[n] = dst;
	}

	Object[] alloc(int i) {
		nodes[i] = (cache[i] == null) ? null : cache[i].get();
		if (nodes[i] == null)
			cache[i] = new SoftReference<Object[]>(nodes[i] = new Object[1 << i]);
		return nodes[i];
	}

	@SuppressWarnings("unchecked")
	private int cmp(Object x, Object y) {
		return cmp.compare((T) x, (T) y);
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

	public int size() {
		return size;
	}

}
