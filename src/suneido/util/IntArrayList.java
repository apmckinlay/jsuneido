/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

/**
 * not Iterable because that would require boxing
 */
public class IntArrayList {
	private static final int DEFAULT_INITIAL_SIZE = 4;
	private int[] data;
	private int size = 0;

	/**
	 * Constructs an empty list with a small initial capacity.
	 */
	public IntArrayList() {
		data = new int[DEFAULT_INITIAL_SIZE];
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 */
	public IntArrayList(int initialCapacity) {
		data = new int[initialCapacity];
	}

	public int get(int i) {
		assert i < size;
		return data[i];
	}

	public int size() {
		return size;
	}

	public void set(int i, int value) {
		assert i < size : "index " + i + " greater than size " + size;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < size(); ++i)
			sb.append(data[i]).append(",");
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}

}
