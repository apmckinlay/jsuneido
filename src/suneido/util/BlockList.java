/* Copyright 2019 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.google.common.primitives.Ints;

/**
 *  Similar to an ArrayList but using fixed size blocks
 *  rather than an array that is grown by realloc and copy.
 */
public class BlockList {
	final static int BLOCKSIZE = 4096; // should be power of 2
	private int[][] blocks = new int[1][];
	private int nblocks = 1;
	private int[] curblock; // shortcut to last of blocks
	private int cursize = 0; // size of curblock (NOT total size)
	private final Alloc alloc;
	private Deque<int[]> freeList = new ArrayDeque<>();
	private final IntComparator cmp;

	interface Alloc {
		int[] alloc();
	}
	static int[] defaultAlloc() {
		return new int[BLOCKSIZE];
	}

	public BlockList() {
		this(Ints::compare, BlockList::defaultAlloc);
	}

	public BlockList(IntComparator cmp) {
		this(cmp, BlockList::defaultAlloc);
	}

	private BlockList(IntComparator cmp, Alloc alloc) {
		this.cmp = cmp;
		this.alloc = alloc;
		blocks[0] = curblock = alloc.alloc();
	}

	public void add(int x) {
		if (cursize >= BLOCKSIZE)
			addBlock();
		curblock[cursize++] = x;
	}

	private void addBlock() {
		if (nblocks >= blocks.length) {
			blocks = Arrays.copyOf(blocks, blocks.length * 2);
		}
		blocks[nblocks] = curblock = alloc.alloc();
		++nblocks;
		cursize = 0;
	}

	public int size() {
		return (nblocks-1) * BLOCKSIZE + cursize;
	}

	public int get(int i) {
		return blocks[i / BLOCKSIZE][i % BLOCKSIZE];
	}

	@Override
	public String toString() {
		var s = "[";
		var sep = "";
		for (int i = 0; i < size(); ++i) {
			s += sep + get(i);
			sep = " ";
			if (i > 20) {
				s += "...";
				break;
			}
		}
		return s + "]";
	}

	// --------------------------------------------------------------

	/**
	 *  sort sorts each block and then merges the blocks.
	 *  Blocks are reused so at most (and usually)
	 *  an extra two blocks will be allocated.
	 */
	public void sort() {
		if (nblocks == 1) {
			sortBlock(0);
			return;
		}
		for (int inSize = 1; inSize < nblocks; inSize *= 2)
			merge(inSize);
	}

	// merge runs of inSize (number of blocks)
	private void merge(int inSize) {
		for (int bi = 0; bi < nblocks; bi += 2 * inSize) {
			if (inSize == 1) {
				// first pass has to sort the blocks
				// do this right before merge to be cache friendly
				sortBlock(bi);
				if (bi+1 < nblocks)
					sortBlock(bi + 1);
			}
			if (bi+inSize < nblocks)
				merge(bi, inSize);
		}
	}

	private void sortBlock(int bi) {
		if (bi == nblocks - 1)
			Sort.sort(blocks[bi], 0, cursize, cmp);
		else
			Sort.sort(blocks[bi], cmp);
	}

	// merge [i, i+inSize) with [i+inSize, i+2*inSize)
	// equivalent to sorting the range [i, i+2*inSize)
	private void merge(int startBlock, int inSize) {
		var result = new BlockList(cmp, this::freeListAlloc);
		int start = startBlock * BLOCKSIZE;
		int n = inSize * BLOCKSIZE;
		int i2 = start + n, lim2 = Math.min(i2 + n, size());
		int i1 = start, lim1 = i2;
		int x1 = get(i1), x2 = get(i2);
		if (cmp.compare(get(i2-1), x2) < 0) {
System.out.println("SKIP MERGE");
			return; // already in order
		}
		while (i1 < lim1 || i2 < lim2) {
			if (i2 >= lim2 || (i1 < lim1 && cmp.compare(x1, x2) < 0)) {
				result.add(x1);
				x1 = safeget(++i1, lim1);
				if (i1 % BLOCKSIZE == 0)
					free((i1-1) / BLOCKSIZE);
			} else {
				result.add(x2);
				x2 = safeget(++i2, lim2);
				if (i2 % BLOCKSIZE == 0 || i2 >= lim2)
					free((i2-1) / BLOCKSIZE);
			}
		}
		for (int i = 0; i < result.nblocks; ++i)
			blocks[startBlock + i] = result.blocks[i];
	}

	// returns max for out of range
	private int safeget(int i, int lim) {
		if (i >= lim)
			return Integer.MAX_VALUE;
		return get(i);
	}

	private void free(int bi) {
		freeList.push(blocks[bi]);
		blocks[bi] = null;
	}

	private int[] freeListAlloc() {
		if (freeList.isEmpty())
			return defaultAlloc();
		return freeList.pop();
	}

	// --------------------------------------------------------------

	public Iter iter() {
		return new Iter();
	}

	private enum Dir { NEXT, PREV }

	public class Iter {
		private int i;
		private Dir dir = null;

		private Iter() {
		}

		/** @return Integer.MAX_VALUE at end */
		public int next() {
			if (dir == null)
				i = 0;
			else if (dir == Dir.PREV)
				next2(); // have to skip when changing direction
			return next2();
		}

		private int next2() {
			if (i < size()) {
				dir = Dir.NEXT;
				return get(i++);
			}
			dir = Dir.PREV;
			return Integer.MAX_VALUE;
		}

		/** @return Integer.MIN_VALUE at beginning */
		public int prev() {
			if (dir == null)
				i = size();
			else if (dir == Dir.NEXT)
				prev2(); // have to skip when changing direction
			return prev2();
		}

		private int prev2() {
			if (i > 0) {
				dir = Dir.PREV;
				return get(--i);
			}
			dir = Dir.NEXT;
			return Integer.MIN_VALUE;
		}

		public void seekFirst(int x) {
			i = lowerBound(x);
			dir = Dir.NEXT;
		}

		public void seekLast(int x) {
			i = upperBound(x);
			dir = Dir.PREV;
		}

	}

	private int upperBound(int value) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (cmp.compare(value, get(middle)) < 0)
				len = half;
			else {
				first = middle + 1;
				len -= half + 1;
			}
		}
		return first;
	}

	private int lowerBound(int value) {
		int first = 0;
		int len = size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (cmp.compare(get(middle), value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

}
