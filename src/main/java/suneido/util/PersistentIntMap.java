/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A persistent immutable map from int to reference.
 * Since keys are ints, don't have to worry about overflow.
 *
 * @see PersistentMap
 */
public abstract class PersistentIntMap<V> {
	private static final int BITS_PER_LEVEL = 5;
	private static final int LEVEL_MASK = (1 << BITS_PER_LEVEL) - 1;

	@SuppressWarnings("rawtypes")
	private static final Node emptyNode = new Node(0, new Object[0]);

	@SuppressWarnings("unchecked")
	public static final <V> PersistentIntMap<V> empty() {
		return emptyNode;
	}

	/** @return The corresponding value or null if not found */
	public abstract V get(int key);

	/** @return A new version of the map with the key/value added or updated */
	public abstract PersistentIntMap<V> with(int key, V value);

	private static class Node<V> extends PersistentIntMap<V> {
		int bitmap;
		Object data[];

		private Node(int bitmap, Object[] data) {
			this.bitmap = bitmap;
			this.data = data;
		}

		@Override
		public V get(int key) {
			return get(key, 0);
		}
		@SuppressWarnings("unchecked")
		protected V get(int key, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			if ((bitmap & bit) == 0)
				return null;
			int i = Integer.bitCount(bitmap & (bit - 1));
			if (data[i] instanceof Entry) {
				Entry entry = (Entry) data[i];
				return entry.key == key ? (V) entry.value : null;
			} else {
				Node<V> child = (Node<V>) data[i];
				return child.get(key, shift + BITS_PER_LEVEL);
			}
		}

		protected static int bit(int key, int shift) {
			int h = (key >>> shift) & LEVEL_MASK;
			return 1 << h;
		}

		@Override
		public PersistentIntMap<V> with(int key, V value) {
			checkNotNull(value);
			return with(key, value, 0);
		}

		@SuppressWarnings("unchecked")
		public Node<V> with(int key, V value, int shift) {
			Entry assoc;
			int bm;
			Object data2[];
			int bit = bit(key, shift);
			int i = Integer.bitCount(bitmap & (bit - 1));
			if ((bitmap & bit) == 0) { // not found
				data2 = new Object[data.length + 1];
				bm = bitmap | bit;
				System.arraycopy(data, 0, data2, 0, i);
				System.arraycopy(data, i, data2, i + 1, data.length - i);
				// fall thru to bottom
			} else if (data[i] instanceof Entry) {
				assoc = (Entry) data[i];
				if (assoc.key == key) {
					if (assoc.value.equals(value))
						return this; // entry already exists
					data2 = data.clone(); // same length since key already existed
					bm = bitmap;
					// fall through to bottom
				} else { // collision
					// push entry into child node along with new entry
					data2 = data.clone();
					data2[i] = newChild(assoc, key, value, shift + BITS_PER_LEVEL);
					return new Node<V>(bitmap, data2);
				}
			} else { // slot points to child node
				Node<V> child = (Node<V>) data[i];
				Node<V> newchild = child.with(key, value, shift + BITS_PER_LEVEL);
				if (newchild == child)
					return this; // entry already exists
				data2 = data.clone();
				data2[i] = newchild;
				return new Node<V>(bitmap, data2);
			}
			data2[i] = new Entry(key, value);
			return new Node<V>(bm, data2);
		}

		private static <V> Node<V> newChild(Entry assoc,
				int key, V value, int shift) {
			int ha = (assoc.key >> shift) & LEVEL_MASK;
			int h = (key >>> shift) & LEVEL_MASK;
			if (ha == h) { // collision
				Object[] aa = new Object[1];
				aa[0] = newChild(assoc, key, value, shift + BITS_PER_LEVEL);
				return new Node<V>(1 << h, aa);
			}
			Object[] aa = new Object[2];
			Entry newAssoc = new Entry(key, value);
			if (h < ha) {
				aa[0] = newAssoc;
				aa[1] = assoc;
			} else {
				aa[0] = assoc;
				aa[1] = newAssoc;
			}
			int bm = (1 << h) | (1 << ha);
			return new Node<V>(bm, aa);
		}

	} // end of Node

	private static class Entry {
		final int key;
		final Object value;

		Entry(int key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

}
