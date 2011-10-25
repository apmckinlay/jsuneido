/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Add items in a random order, iterate them in order. Stable sort.
 * Fast (amortized) insertion.
 * Not very good for lookups (so none implemented)
 * Cache oblivious.
 * Cache friendly - mostly sequential access.
 * Optimal memory space (e.g. as opposed to array size doubling)
 */
public class MergeTree<T> {
	private static final int MAX_LEVELS = 32;
	private final Object[][] nodes = new Object[MAX_LEVELS][];
	@SuppressWarnings("unchecked")
	private final SoftReference<Object[]>[] cache = new SoftReference[MAX_LEVELS];
	private final int[] pos = new int[MAX_LEVELS]; // temp for merge
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
		Arrays.fill(pos, 0, n, 0);
		while (true) {
			int iMin = -1;
			Object min = x;
			for (int i = 0; i < n; ++i)
				if (pos[i] < nodes[i].length) {
					Object val = nodes[i][pos[i]];
					if (min == null || cmp(val, min) <= 0) {
						min = val;
						iMin = i;
					}
				}
			if (min == null)
				break;
			if (iMin == -1)
				x = null;
			else
				++pos[iMin];
			dst[di++] = min;
		}
		assert di == dst.length;
		Arrays.fill(nodes, 0, n, null);
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

	public void clear() {
		size = 0;
		Arrays.fill(nodes, 0, MAX_LEVELS, null);
	}

	public Iter iter() {
		return new Iter();
	}

	public class Iter {
		private final int n;
		private final NodeIter[] data;
		private boolean rewound = true;

		private Iter() {
			n = Integer.bitCount(size);
			data = new NodeIter[n];
			int di = 0;
			for (int i = 0; i < MAX_LEVELS; ++i)
				if (nodes[i] != null)
					data[di++] = new NodeIter(nodes[i]);
		}

		@SuppressWarnings("unchecked")
		public T next() {
			if (rewound)
				setPos(true);
			int iMin = 0;
			Object min = null;
			for (int i = 0; i < n; ++i)
				if (data[i].hasNext())
					if (min == null || cmp(data[i].peekNext(), min) <= 0)
						min = data[iMin = i].peekNext();
			if (min == null)
				rewound = true;
			else
				data[iMin].next();
			return (T) min;
		}

		@SuppressWarnings("unchecked")
		public T prev() {
			if (rewound)
				setPos(false);
			int iMax = 0;
			Object max = null;
			for (int i = n - 1; i >= 0; --i)
				if (data[i].hasPrev())
					if (max == null || cmp(data[i].peekPrev(), max) >= 0)
						max = data[iMax = i].peekPrev();
			if (max == null)
				rewound = true;
			else
				data[iMax].prev();
			return (T) max;
		}

		private void setPos(boolean first) {
			for (int i = 0; i < n; ++i)
				data[i].setPos(first ? -1 : data[i].node.length);
			rewound = false;
		}

		@SuppressWarnings("unchecked")
		public void seekFirst(T x) {
			for (int i = 0; i < n; ++i)
				data[i].setPos(Util.lowerBound((T[]) data[i].node, x, cmp) - 1);
			rewound = false;
		}

		@SuppressWarnings("unchecked")
		public void seekLast(T x) {
			for (int i = 0; i < n; ++i)
				data[i].setPos(Util.upperBound((T[]) data[i].node, x, cmp));
			rewound = false;
		}

	}

	private static class NodeIter {
		Object[] node;
		int pos = 0;

		NodeIter(Object[] node) {
			this.node = node;
			assert node.length > 0;
		}

		boolean hasNext() {
			return pos + 1 < node.length;
		}

		boolean hasPrev() {
			return pos > 0;
		}

		Object peekNext() {
			return node[pos + 1];
		}

		Object peekPrev() {
			return node[pos - 1];
		}

		void next() {
			++pos;
		}

		void prev() {
			--pos;
		}

		void setPos(int pos) {
			this.pos = pos;
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
