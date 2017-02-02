/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A merge tree using fixed size blocks of memory
 * rather than contiguous runs of doubling sizes.
 * This is better for allocation and garbage collection.
 */
public class BlockMergeTree<T extends Comparable<T>> implements Iterable<T> {
	private final int BLOCKSIZE;
	private static final int DEFAULT_BLOCKSIZE = 256;
	private final Comparator<? super T> cmp;
	/**
	 * blocks is an array of levels,
	 * each level is an array of blocks,
	 * each block is an array of values
	 * The first level (one block) is not sorted during insertions.
	 * All other levels are sorted.
	 */
	private Level[] levels = newArray(0);
	private final ArrayDeque<T[]> freeBlocks = new ArrayDeque<>();
	/**
	 * The number of elements.
	 * size % BLOCKSIZE is the number of elements in the first block.
	 */
	private int size = 0;

	@SafeVarargs
	static <E> E[] newArray(int length, E... array) {
	    return Arrays.copyOf(array, length);
	}

	public BlockMergeTree() {
		this(DEFAULT_BLOCKSIZE);
	}

	public BlockMergeTree(int blocksize) {
		this(blocksize, (T x, T y) -> x.compareTo(y));
	}

	public BlockMergeTree(Comparator<? super T> cmp) {
		this(DEFAULT_BLOCKSIZE, cmp);
	}

	public BlockMergeTree(int blocksize, Comparator<? super T> cmp) {
		BLOCKSIZE = blocksize;
		this.cmp = cmp;
		expandLevels(); // create first level
		expandLevels(); // create second level
	}

	public void add(T value) {
		int i = (size % BLOCKSIZE);
		if (i == 0 && size > 0)
			merge();
		levels[0].add(value);
		++size;
	}

	/** Merge initial full levels into the first empty level */
	private void merge() {
		sortFirstBlock();
		// don't need to merge a single level
		if (levels[1].isEmpty()) { // 50% of the time
			// swap levels 0 and 1
			Level tmp = levels[1];
			levels[1] = levels[0];
			levels[0] = tmp;
		} else
			mergeTo(firstEmptyLevel());
	}

	private void sortFirstBlock() {
		Arrays.sort(levels[0].blocks[0], 0, levels[0].size, cmp);
	}

	/** merge levels 0 to destLevel - 1 to destLevel */
	private void mergeTo(int destLevel) {
		Level dest = levels[destLevel];
		dest.clear();
		int n = levelSize(destLevel);
		for (int i = 0; i < n; ++i) {
			T minValue = null;
			int minLevel = -1;
			for (int level = 0; level < destLevel; ++level) {
				T value = levels[level].first();
				if (value != null &&
						(minValue == null || cmp.compare(value, minValue) < 0)) {
					minValue = value;
					minLevel = level;
				}
			}
			dest.add(minValue);
			levels[minLevel].popFirst();
		}
		for (int level = 0; level < destLevel; ++level)
			levels[level].clear();
	}
	//TODO move whole blocks when possible

	/** @return the first empty level, expanding blocks as necessary */
	private int firstEmptyLevel() {
		for (int level = 0; ; ++level) {
			if (level >= levels.length)
				expandLevels();
			if (levels[level].isEmpty())
				return level;
		}
	}

	/** @return The number of items on a level */
	private int levelSize(int level) {
		return levelBlocks(level) * BLOCKSIZE;
	}

	/** @return the number of blocks in a level  */
	private static int levelBlocks(int level) {
		return level == 0 ? 1 : 1 << (level - 1);
	}

	/** add a level */
	private void expandLevels() {
		int level = levels.length;
		levels = Arrays.copyOf(levels, levels.length + 1);
		levels[level] = new Level(levelBlocks(level));
	}

	private void free(T[] block) {
		freeBlocks.add(block);
	}

	private T[] alloc() {
		return freeBlocks.isEmpty()
				? newArray(BLOCKSIZE)
				: freeBlocks.removeLast();
	}

	public void print() {
		for (Level level : levels)
			level.print();
		System.out.println();
	}

	/** check that each level is sorted (other than first) */
	public void check() {
		for (int level = 1; level < levels.length; ++level)
			levels[level].check();
	}

	//--------------------------------------------------------------------------

	private class Level {
		final T[][] blocks;
		int size = 0;
		int first = 0;

		Level(int nblocks) {
			blocks = newArray(nblocks);
		}

		void add(T x) {
			int b = size / BLOCKSIZE;
			if (blocks[b] == null)
				blocks[b] = alloc();
			int i = size % BLOCKSIZE;
			blocks[b][i] = x;
			++size;
		}

		boolean isEmpty() {
			return size == 0;
		}

		T first() {
			return first >= size
					? null
					: blocks[first / BLOCKSIZE][first % BLOCKSIZE];
		}

		void popFirst() {
			first++;
			if (first % BLOCKSIZE == 0) {
				int b = (first - 1) / BLOCKSIZE;
				free(blocks[b]);
				blocks[b] = null;
			}
		}

		void clear() {
			size = first = 0;
		}

		/** check that level is sorted */
		void check() {
			if (size == 0)
				return;
			T prev = blocks[0][0];
			for (T[] b : blocks)
				for (T val : b) {
					assert cmp.compare(val, prev) >= 0;
					prev = val;
				}
		}

		void print() {
			int i = 0;
			for (T[] b : blocks)
				if (b == null)
					System.out.print("null ");
				else {
					System.out.print("[");
					for (T val : b)
						if (i++ < size)
							System.out.print(val + " ");
						else
							break;
					System.out.print("] ");
				}
			System.out.println("size " + size + " first " + first);
		}

		Iter iter() {
			return new Iter();
		}

		class Iter {
			int bi = 0;
			int i = 0;
			int iLimit = Math.min(size, BLOCKSIZE);

			T peek() {
				return blocks[bi][i];
			}

			boolean hasNext() {
				return bi < blocks.length;
			}

			boolean next() {
				if (bi >= blocks.length)
					return false;
				if (++i < iLimit)
					return true;
				i = 0;
				return ++bi < blocks.length;
			}

			@Override
			public String toString() {
				return "len " + blocks.length + " bi " + bi + " i " + i +
						(hasNext() ? " hasNext" : "");
			}
		}
	}

	//--------------------------------------------------------------------------

	@Override
	public Iterator<T> iterator() {
		sortFirstBlock();
		return new Iter();
	}

	private class Iter implements Iterator<T> {
		Level.Iter[] iters;
		int n;

		Iter() {
			iters = newArray(levels.length);
			for (int level = 0; level < levels.length; ++level)
				if (!levels[level].isEmpty())
					iters[n++] = levels[level].iter();
		}

		@Override
		public boolean hasNext() {
			for (int i = 0; i < n; ++i)
				if (iters[i].hasNext())
					return true;
			return false;
		}

		@Override
		public T next() {
			int iMin = -1;
			T min = null;
			for (int i = 0; i < n; ++i)
				if (iters[i].hasNext())
					if (min == null || cmp.compare(iters[i].peek(), min) <= 0)
						min = iters[iMin = i].peek();
			iters[iMin].next();
			return min;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < n; ++i)
				sb.append(iters[i]).append(" / ");
			return sb.toString();
		}
	}

}
