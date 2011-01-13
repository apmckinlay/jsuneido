/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

public class IntArrayList {
	private static final int DEFAULT_INITIAL_SIZE = 4;
	private int[] data;
	private int size = 0;

	public IntArrayList() {
		data = new int[DEFAULT_INITIAL_SIZE];
	}

	public IntArrayList(int n) {
		data = new int[Math.max(n, DEFAULT_INITIAL_SIZE)];
	}

	public int get(int i) {
		assert i < size;
		return data[i];
	}

	public int size() {
		return size;
	}

	public void set(int i, int value) {
		assert i < size;
		data[i] = value;
	}

	public void add(int value) {
		allow1();
		data[size++] = value;
	}

	public void add(int i, int value) {
		allow1();
		if (i == size)
			data[size++] = value;
		else {
			assert i < size;
			System.arraycopy(data, i, data, i + 1, size - i);
			data[i] = value;
			++size;
		}
	}

	private void allow1() {
		if (size >= data.length) {
			int[] data2 = new int[2 * data.length];
			System.arraycopy(data, 0, data2, 0, data.length);
			data = data2;
		}
	}

}
