/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class IntArrayList {
	private static final int DEFAULT_INITIAL_SIZE = 4;
	private int[] data;
	private int size = 0;

	public IntArrayList() {
		data = new int[DEFAULT_INITIAL_SIZE];
	}

	public IntArrayList(int n) {
		data = new int[n];
	}

	public void add(int value) {
		if (size >= data.length) {
			int[] data2 = new int[2 * data.length];
			System.arraycopy(data, 0, data2, 0, data.length);
			data = data2;
		}
		data[size++] = value;
	}

	public int get(int i) {
		assert i < size;
		return data[i];
	}

	public int size() {
		return size;
	}

}
