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
	private final Comparator<? super T> cmp;
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

	public MergeTree(Comparator<? super T> cmp) {
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

	private enum Dir { NEXT, PREV };

	public class Iter {
		private final int n;
		private final NodeIter[] iters;
		private Dir dir = null;

		private Iter() {
			n = Integer.bitCount(size);
			iters = new NodeIter[n];
			int di = 0;
			for (int i = 0; i < MAX_LEVELS; ++i)
				if (nodes[i] != null)
					iters[di++] = new NodeIter(nodes[i]);
		}

		public T next() {
			if (dir == null)
				first();
			else if (dir == Dir.PREV)
				next2(); // have to skip when changing direction
			return next2();
		}

		@SuppressWarnings("unchecked")
		private T next2() {
			int iMin = 0;
			Object min = null;
			for (int i = 0; i < n; ++i)
				if (iters[i].hasNext())
					if (min == null || cmp(iters[i].peekNext(), min) <= 0)
						min = iters[iMin = i].peekNext();
			if (min == null)
				dir = Dir.PREV;
			else {
				dir = Dir.NEXT;
				iters[iMin].next();
			}
			return (T) min;
		}

		public T prev() {
			if (dir == null)
				last();
			else if (dir == Dir.NEXT)
				prev2(); // have to skip when changing direction
			return prev2();
		}

		@SuppressWarnings("unchecked")
		private T prev2() {
			int iMax = 0;
			Object max = null;
			for (int i = n - 1; i >= 0; --i)
				if (iters[i].hasPrev())
					if (max == null || cmp(iters[i].peekPrev(), max) >= 0)
						max = iters[iMax = i].peekPrev();
			if (max == null)
				dir = Dir.NEXT;
			else {
				dir = Dir.PREV;
				iters[iMax].prev();
			}
			return (T) max;
		}

		private void first() {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(0);
		}

		private void last() {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(iters[i].node.length);
		}

		@SuppressWarnings("unchecked")
		public void seekFirst(T x) {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(Util.lowerBound((T[]) iters[i].node, x, cmp));
			dir = Dir.NEXT;
		}

		@SuppressWarnings("unchecked")
		public void seekLast(T x) {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(Util.upperBound((T[]) iters[i].node, x, cmp));
			dir = Dir.PREV;
		}

		public void print() {
			System.out.println("\nIter: dir " + dir);
			if (dir != null)
				for (int i = 0; i < n; ++i)
					System.out.println(iters[i]);
		}

	}

	/**
	 * pos points to next, prev is pos - 1
	 */
	private static class NodeIter {
		Object[] node;
		int pos = 0;

		NodeIter(Object[] node) {
			this.node = node;
			assert node.length > 0;
		}

		boolean hasNext() {
			return pos < node.length;
		}

		boolean hasPrev() {
			return (pos - 1) >= 0;
		}

		Object peekNext() {
			return node[pos];
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

		@Override
		public String toString() {
			return pos + " " + Arrays.toString(node);
		}

	}

	public void print() {
		for (int i = 0; i < MAX_LEVELS; ++i)
			if (nodes[i] != null)
				System.out.println(Arrays.toString(nodes[i]));
	}

	public int size() {
		return size;
	}

}
