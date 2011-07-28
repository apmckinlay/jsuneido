/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Strings;

/**
 * Persistent semi-immutable hash tree used for storing redirections.
 * <p>
 * Based on <a href="http://lampwww.epfl.ch/papers/idealhashtrees.pdf">
 * Bagwell's Ideal Hash Trees</a>
 * <p>
 * Keys are int's so no hashing or overflow is required.
 * <p>
 * Nodes are stored as:<ul>
 * <li>bitmap	- an int where the bits specify which entries are present
 * <li>entries	- up to 32 entries, each a pair of int's (key and value)
 * 		if key is 0 then value points to a child node
 * </ul>
 * Similar to {@link suneido.util.PersistentMap}
 */
abstract class DbHashTrie {
	private static final int BITS_PER_LEVEL = 5;
	private static final int HASH_BITS = 1 << BITS_PER_LEVEL;
	private static final int LEVEL_MASK = HASH_BITS - 1;
	private static final int INT_BYTES = Integer.SIZE / 8;

	/** NOTE: not stored, so mutable */
	static DbHashTrie empty() {
		return empty(null);
	}

	static DbHashTrie empty(Storage stor) {
		return new Node(stor);
	}

	static DbHashTrie from(Storage stor, int at) {
		return new Node(stor, at);
	}

	abstract static class Entry {
		abstract int key();
		abstract int value();
	}

	/** returns null if key not present */
	Entry get(int key) {
		checkArgument(key != 0);
		return get(key, 0);
	}
	protected abstract Entry get(int key, int shift);

	/** key must be non-zero */
	DbHashTrie with(Entry e) {
		checkArgument(e.key() != 0);
		return with(e, 0);
	}
	protected abstract DbHashTrie with(Entry e, int shift);

	/** call proc.apply(adr) for each new entry (where value is an intref) */
	abstract void traverseChanges(Process proc);

	abstract int store(Translator translator);

	void print() {
		print(0);
	}
	protected abstract void print(int shift);

	private static class Node extends DbHashTrie {
		private static final int ENTRIES = INT_BYTES;
		private static final int ENTRY_SIZE = 2 * INT_BYTES;
		protected final Storage stor;
		private int adr = 0;
		private int bitmap;
		private Object data[];

		private Node(Storage stor) {
			this.stor = stor;
			bitmap = 0;
			data = new Object[4];
		}

		private Node(Storage stor, int adr) {
			this.stor = stor;
			this.adr = adr;
			ByteBuffer buf = stor.buffer(adr);
			bitmap = buf.getInt(0);
			data = new Object[size()];
			for (int i = 0; i < size(); ++i) {
				int key = buf.getInt(ENTRIES + i * ENTRY_SIZE);
				int val = buf.getInt(ENTRIES + i * ENTRY_SIZE + INT_BYTES);
				data[i] = (key == 0) ? val : new StoredIntEntry(key, val);
			}
		}

		@Override
		protected Entry get(int key, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			if ((bitmap & bit) == 0)
				return null;
			int i = Integer.bitCount(bitmap & (bit - 1));
			if (data[i] instanceof Entry) {
				Entry e = (Entry) data[i];
				return e.key() == key ? e : null;
			} else { // pointer to child
				if (data[i] instanceof Integer)
					synchronized(this) {
						data[i] = new Node(stor, ((Integer) data[i]));
					}
				return ((Node) data[i]).get(key, shift + BITS_PER_LEVEL);
			}
		}

		private static int bit(int key, int shift) {
			int h = (key >>> shift) & LEVEL_MASK;
			return 1 << h;
		}

		private int size() {
			return Integer.bitCount(bitmap);
		}

		@Override
		protected Node with(Entry e, int shift) {
			if (stored())
				return new Node(this, e, shift);

			assert shift < 32;
			int key = e.key();
			int bit = bit(key, shift);
			int i = Integer.bitCount(bitmap & (bit - 1));
			if ((bitmap & bit) == 0) { // not present
				insert(i, e);
				bitmap |= bit;
			} else if (data[i] instanceof Entry) {
				Entry de = (Entry) data[i];
				if (de.key() == key)	// key already present
					data[i] = e; 		// so update in place
				else // collision, change entry to pointer
					data[i] = new Node(stor, de, e, shift + BITS_PER_LEVEL);
			} else { // pointer to child
				Node child;
				Object ptr = data[i];
				if (ptr instanceof Integer)
					child = new Node(stor, ((Integer) ptr));
				else
					child = (Node) ptr;
				data[i] = child.with(e, shift + BITS_PER_LEVEL);
			}
			return this;
		}
		private Node(Node node, Entry e, int shift) {
			stor = node.stor;
			bitmap = node.bitmap;
			int n = size();
			data = Arrays.copyOf(node.data, n + 1);
			with(e, shift);
		}
		private void insert(int i, Entry e) {
			int n = size();
			data = ensureCapacity(data, n + 1, 2);
			if (n > i)
				System.arraycopy(data, i, data, i + 1, n - i);
			data[i] = e;
		}
		private static Object[] ensureCapacity(Object[] a, int minLength, int padding) {
			return (a.length >= minLength)
				? a
				: Arrays.copyOf(a, minLength + padding);
		}
		private Node(Storage stor, Entry e1, Entry e2, int shift) {
			this.stor = stor;
			assert shift < 32;
			int key1 = e1.key();
			int key2 = e2.key();
			assert key1 != key2;
			int bits1 = (key1 >>> shift) & LEVEL_MASK;
			int bits2 = (key2 >>> shift) & LEVEL_MASK;
			if (bits1 == bits2) { // collision
				Node child = new Node(stor, e1, e2, shift + BITS_PER_LEVEL);
				data = new Object[] { child };
				bitmap = (1 << bits1);
			} else {
				if (bits1 < bits2)
					data = new Object[] { e1, e2 };
				else
					data = new Object[] { e2, e1 };
				bitmap = (1 << bits1) | (1 << bits2);
			}
		}

		@Override
		int store(Translator translator) {
			if (stored())
				return adr;

			adr = stor.alloc(byteBufSize());
			ByteBuffer buf = stor.buffer(adr);
			buf.putInt(bitmap);
			for (int i = 0; i < size(); ++i) {
				int key = 0;
				int value;
				if (data[i] instanceof Entry) {
					Entry e = (Entry) data[i];
					key = e.key();
					e = translator.translate(e);
					assert key == e.key();
					value = e.value();
					data[i] = e;
				} else if (data[i] instanceof Integer)
					value = (Integer) data[i];
				else
					value = ((Node) data[i]).store(translator);
				buf.putInt(key);
				buf.putInt(value);
			}
			return adr;
		}
		private int byteBufSize() {
			return INT_BYTES + // bitmap
					(2 * size() * INT_BYTES); // keys and values
		}

		@Override
		void traverseChanges(Process proc) {
			if (stored())
				return ;
			for (int i = 0; i < size(); ++i) {
				if (data[i] instanceof Node)
					((Node) data[i]).traverseChanges(proc);
				else if (data[i] instanceof Entry &&
						! (data[i] instanceof StoredIntEntry))
					proc.apply((Entry) data[i]);
			}
		}

		private boolean stored() {
			return adr != 0;
		}

		@Override
		protected void print(int shift) {
			String indent = Strings.repeat(" ", shift);
			System.out.println(indent + (adr == 0 ? "" : "db") + "Node");
			for (int i = 0; i < size(); ++i) {
				if (data[i] instanceof Entry) {
					Entry e = (Entry) data[i];
					System.out.println(indent + fmt(e.key()) + "\t" + e);
				} else if (data[i] instanceof Node) {
					System.out.println(indent + ">>>>>>>>");
					Node child = (Node) data[i];
					child.print(shift + BITS_PER_LEVEL);
				} else
					System.out.println(indent + "adr(" + data[i] + ")");
			}
		}
		private static String fmt(int n) {
			if (n == 0)
				return "0";
			String s = "";
			for (; n != 0; n >>>= 5)
				s = (n & 0x1f) + "." + s;
			return s.substring(0, s.length() - 1);
		}

	} // end of Node

	interface Translator {
		Entry translate(Entry e);
	}

	interface Process {
		void apply(Entry e);
	}

	@Immutable
	static class IntEntry extends Entry {
		final int key;
		final int value;

		IntEntry(int key, int value) {
			this.key = key;
			this.value = value;
		}

		@Override
		int key() {
			return key;
		}

		@Override
		int value() {
			return value;
		}

		@Override
		public String toString() {
			return "IntEntry(" + key + ", " + value + ")";
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof IntEntry) {
				IntEntry that = (IntEntry) other;
				return this.key == that.key && this.value == that.value;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return key ^ value;
		}
	}

	/** used to identify old entries */
	static class StoredIntEntry extends IntEntry {
		StoredIntEntry(int key, int value) {
			super(key, value);
		}
	}

	static class RefEntry<T> extends Entry {
		private final int key;
		private final T value;

		RefEntry(int key, T value) {
			this.key = key;
			this.value = value;
		}

		@Override
		int key() {
			return key;
		}

		@Override
		int value() {
			throw new UnsupportedOperationException("RefEntry doesn't support value()");
		}

		@Override
		public String toString() {
			return "RefEntry(" + key + ", " + value + ")";
		}

	}

}
