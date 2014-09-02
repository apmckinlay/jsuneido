/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.lang.ref.SoftReference;
import java.util.Arrays;

import com.google.common.primitives.Ints;

/**
 * An int version of MergeTree.
 * Because we don't have null, Integer.MIN_VALUE and MAX_VALUE
 * are used as special values and cannot be stored.
 * Uses IntComparator and int versions of Util.lowerBound and upperBound.
 */
public class IntMergeTree {
	private static final int MAX_LEVELS = 32;
	private final int[][] nodes = new int[MAX_LEVELS][];
	private final IntComparator cmp;
	@SuppressWarnings("unchecked")
	private final SoftReference<int[]>[] cache = new SoftReference[MAX_LEVELS];
	private final int[] pos = new int[MAX_LEVELS]; // temp for merge
	private int size = 0;

	public IntMergeTree() {
		this.cmp = Ints::compare;
	}

	public IntMergeTree(IntComparator cmp) {
		this.cmp = cmp;
	}

	public void add(int x) {
		assert x != Integer.MIN_VALUE && x != Integer.MAX_VALUE;
		++size;
		merge(x, firstUnused());
	}

	private int firstUnused() {
		int firstUnused = 0;
		while (nodes[firstUnused] != null)
			firstUnused++;
		return firstUnused;
	}

	private void merge(int x, int n) {
		assert nodes[n] == null;
		int[] dst = alloc(n);
		if (n == 0) { // 50% of the cases
			dst[0] = x;
			return;
		}
		if (n == 1) { // another 25% of the cases
			if (cmp.compare(x, nodes[0][0]) < 0) {
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

	private void merge(int x, int n, int[] dst) {
		int di = 0;
		Arrays.fill(pos, 0, n, 0);
		boolean xValue = true;
		while (true) {
			int iMin = -1;
			int min = Integer.MAX_VALUE;
			for (int i = 0; i < n; ++i)
				if (pos[i] < nodes[i].length) {
					int val = nodes[i][pos[i]];
					if (iMin == -1 || cmp.compare(val, min) <= 0) {
						min = val;
						iMin = i;
					}
				}
			if (xValue && (iMin == -1 || cmp.compare(x, min) < 0)) {
				min = x;
				xValue = false;
			} else if (iMin == -1)
				break;
			else
				++pos[iMin];
			dst[di++] = min;
		}
		assert di == dst.length;
		Arrays.fill(nodes, 0, n, null);
	}

	int[] alloc(int i) {
		nodes[i] = (cache[i] == null) ? null : cache[i].get();
		if (nodes[i] == null)
			cache[i] = new SoftReference<>(nodes[i] = new int[1 << i]);
		return nodes[i];
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

		public int next() {
			if (dir == null)
				first();
			else if (dir == Dir.PREV)
				next2(); // have to skip when changing direction
			return next2();
		}

		private int next2() {
			int iMin = 0;
			int min = Integer.MAX_VALUE;
			for (int i = 0; i < n; ++i)
				if (iters[i].hasNext())
					if (min == Integer.MAX_VALUE ||
							cmp.compare(iters[i].peekNext(), min) <= 0)
						min = iters[iMin = i].peekNext();
			if (min == Integer.MAX_VALUE)
				dir = Dir.PREV;
			else {
				dir = Dir.NEXT;
				iters[iMin].next();
			}
			return min;
		}

		public int prev() {
			if (dir == null)
				last();
			else if (dir == Dir.NEXT)
				prev2(); // have to skip when changing direction
			return prev2();
		}

		private int prev2() {
			int iMax = 0;
			int max = Integer.MIN_VALUE;
			for (int i = n - 1; i >= 0; --i)
				if (iters[i].hasPrev())
					if (max == Integer.MIN_VALUE ||
							cmp.compare(iters[i].peekPrev(), max) >= 0)
						max = iters[iMax = i].peekPrev();
			if (max == Integer.MIN_VALUE)
				dir = Dir.NEXT;
			else {
				dir = Dir.PREV;
				iters[iMax].prev();
			}
			return max;
		}

		private void first() {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(0);
		}

		private void last() {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(iters[i].node.length);
		}

		public void seekFirst(int x) {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(Util.lowerBound(iters[i].node, x, cmp));
			dir = Dir.NEXT;
		}

		public void seekLast(int x) {
			for (int i = 0; i < n; ++i)
				iters[i].setPos(Util.upperBound(iters[i].node, x, cmp));
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
		int[] node;
		int pos = 0;

		NodeIter(int[] node) {
			this.node = node;
			assert node.length > 0;
		}

		boolean hasNext() {
			return pos < node.length;
		}

		boolean hasPrev() {
			return (pos - 1) >= 0;
		}

		int peekNext() {
			return node[pos];
		}

		int peekPrev() {
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