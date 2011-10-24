/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Add items in a random order, iterate them in order.
 * Fast (amortized) insertion.
 * Not very good for lookups (so none implemented)
 * Cache oblivious.
 * Cache friendly - mostly sequential access.
 * Optimal memory space (e.g. as opposed to array size doubling)
 */
public class FractalTree2<T> implements Iterable<T> {
	private static final int MAX_LEVELS = 32;
	private final Object[][] nodes = new Object[MAX_LEVELS][];
	@SuppressWarnings("unchecked")
	private final SoftReference<Object[]>[] cache = new SoftReference[MAX_LEVELS];
	private final Object[] tmp = new Object[1];
	private final Comparator<T> cmp;
	private int size = 0;
	
	public FractalTree2() {
		this(new Comparator<T>() {
				@SuppressWarnings("unchecked")
				@Override
				public int compare(T x, T y) {
					return ((Comparable<T>) x).compareTo(y);
				}
			});
	}
	
	public FractalTree2(Comparator<T> cmp) {
		this.cmp = cmp;
	}

	public void add(T x) {
		++size;
		merge(x, firstUnused());
	}

	private int firstUnused() {
		int firstUnused = 0;
		while (nodes[firstUnused] != null)
			firstUnused++;
		return firstUnused;
	}
	
	void merge(T x, int n) {
		Object[] dst = alloc(n);
		if (n == 0) { // 50% of the cases
			dst[0] = x;
			return;
		} else if (n == 1) { // another 25% of the cases
			if (cmp(x, nodes[0][0]) < 0) {
				dst[0] = x;
				dst[1] = nodes[0][0];
			} else {
				dst[0] = nodes[0][0];
				dst[1] = x;	
			}
		}
		tmp[0] = x;
		// TODO merge more efficiently, without Iter
		int i = 0;
		Iter iter = new Iter(tmp, n);
		while (iter.hasNext())
			dst[i++] = iter.next();
		for (i = 0; i < n; ++i)
			nodes[i] = null;
	}
	
	Object[] alloc(int i) {
		nodes[i] = cache[i] == null ? null : cache[i].get();
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
		
		Iter(Object[] extra, int n) {
			lists.add(new NodeIter(extra));
			for (int i = 0; i < n; ++i)
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
	
	public T[] toArray(T[] data) {
		if (data.length < size)
			data = Arrays.copyOf(data, size);
		int i = 0;
		for (T x : this)
			data[i++] = x;
		Arrays.fill(data, size, data.length, null);
		return data;
	}

}
