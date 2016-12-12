/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.Lists;

/**
 * Equivalent to {@link ArraysList} but for int's
 */
@NotThreadSafe
public class IntArraysList {
	final static int CHUNK_SIZE = 512;
	private final ArrayList<int[]> chunks = Lists.newArrayList();
	private int size = 0;

	public boolean add(int value) {
		if (chunkFor(size) >= chunks.size())
			chunks.add(new int[CHUNK_SIZE]);
		chunks.get(chunkFor(size))[indexFor(size)] = value;
		++size;
		return true;
	}

	public void clear() {
		chunks.clear();
		size = 0;
	}

	public int get(int i) {
		return chunks.get(chunkFor(i))[indexFor(i)];
	}

	public int set(int i, int value) {
		int[] chunk = chunks.get(chunkFor(i));
		int prev = chunk[indexFor(i)];
		chunk[indexFor(i)] = value;
		return prev;
	}

	public int size() {
		return size;
	}

	private static int chunkFor(int i) {
		return i / CHUNK_SIZE;
	}

	private static int indexFor(int i) {
		return i % CHUNK_SIZE;
	}

}
