/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.util.ByteBuf;

import com.google.common.base.Strings;

/**
 * Persistent hash tree used for storing redirections.
 * Immutable in the database but mutable in memory.
 * <p>
 * Based on <a href="http://lampwww.epfl.ch/papers/idealhashtrees.pdf">
 * Bagwell's Ideal Hash Trees</a>
 * Key and value are both int's so no hashing or overflow is required.
 */
public abstract class DbHashTree {
	private static final int BITS_PER_LEVEL = 5;
	private static final int HASH_BITS = 1 << BITS_PER_LEVEL;
	private static final int LEVEL_MASK = HASH_BITS - 1;

	public static DbHashTree empty() {
		return new MemoryNode();
	}

	public abstract int get(int key);

	public abstract DbHashTree with(int key, int value);

	public int countNodes() {
		return ((MemoryNode) this).countNodes(0);
	}
	public void print() {
		((MemoryNode) this).print(0);
	}

	private abstract static class Node extends DbHashTree {
		/** returns 0 if key not present */
		@Override
		public int get(int key) {
			checkArgument(key != 0);
			return get(key, 0);
		}
		abstract protected int get(int key, int shift);

		@Override
		public Node with(int key, int value) {
			checkArgument(key != 0);
			checkArgument(value != 0);
			return with(key, value, 0);
		}
		abstract protected Node with(int key, int value, int shift);
	}

	/** DbNode consists of:
	 * 		present	- bitmap of which entries are present
	 * 		entries	- up to 32 entries, each a pair of int's
	 * 		if entry key is 0 then value points to a child node
	 */
	@Immutable
	private static class DbNode extends Node {
		private static final int ENTRIES = Integer.SIZE;
		private static final int ENTRY_SIZE = 2 * Integer.SIZE;
		private ByteBuf buf;

		@Override
		protected int get(int key, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			if ((present() & bit) == 0)
				return 0;
			int i = Integer.bitCount(present() & (bit - 1));
			int entryKey = key(i);
			if (entryKey != 0)
				return entryKey == key ? value(i) : 0;
			else { // pointer
				Node child = intToRef(value(i));
				return child.get(key, shift + BITS_PER_LEVEL);
			}
		}

		@Override
		protected Node with(int key, int value, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			int i = Integer.bitCount(present() & (bit - 1));
			if ((present() & bit) == 0) {
				return new MemoryNode(this).with(key, value, shift);
			} else if (key(i) == key) {
				return (value(i) == value)
					? this
					: new MemoryNode(this).with(key, value, shift);
			}
			int entryKey = key(i);
			if (entryKey == 0) {
				int ptr = refToInt(intToRef(value(i)).with(key, value,
						shift + BITS_PER_LEVEL));
				return new MemoryNode(this, i, ptr);
			} else { // collision
				int ptr = refToInt(new MemoryNode(key(i), value(i), key, value,
						shift + BITS_PER_LEVEL));
				return new MemoryNode(this, i, ptr);
			}
		}

		private static int bit(int key, int shift) {
			int h = (key >>> shift) & LEVEL_MASK;
			return 1 << h;
		}

		short present() {
			return buf.getShort(0);
		}

		int key(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE);
		}
		int value(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE + Integer.SIZE);
		}

	}

	/**
	 * In-memory mutable node used while transaction is in progress.
	 * If entry value is 0 then entry is unused/empty
	 */
	@NotThreadSafe
	private static class MemoryNode extends Node {
		private final int[] keys = new int[HASH_BITS];
		private final int[] values = new int[HASH_BITS];

		MemoryNode() {
		}

		MemoryNode(DbNode dbn) {
			int present = dbn.present();
			for (int i = 0, j = 0; i < HASH_BITS; ++i)
				if ((present & (1 << i)) != 0) {
					keys[i] = dbn.key(j);
					values[i] = dbn.value(j);
					++j;
				}
		}

		MemoryNode(DbNode dbn, int i, int ptr) {
			this(dbn);
			keys[i] = 0;
			values[i] = ptr;
		}

		@Override
		protected int get(int key, int shift) {
			assert shift < 32;
			int i = index(key, shift);
			if (isEmpty(i))
				return 0;
			else if (isPointer(i))
				return intToRef(values[i]).get(key, shift + BITS_PER_LEVEL);
			else
				return values[i];
		}

		@Override
		protected MemoryNode with(int key, int value, int shift) {
			assert shift < 32;
			int i = index(key, shift);
			if (isEmpty(i)) {
				keys[i] = key;
				values[i] = value;
			} else if (keys[i] == key) {
				values[i] = value;
			} else if (isPointer(i)) {
				intToRef(values[i]).with(key, value, shift + BITS_PER_LEVEL);
			} else { // collision, change entry to pointer
				values[i] = refToInt(new MemoryNode(keys[i], values[i], key, value,
					shift + BITS_PER_LEVEL));
				keys[i] = 0;
			}
			return this;
		}

		private MemoryNode(int key1, int value1, int key2, int value2, int shift) {
			assert shift < 32;
			assert key1 != key2;
			int i1 = index(key1, shift);
			int i2 = index(key2, shift);
			if (i1 != i2) {
				keys[i1] = key1;
				values[i1] = value1;
				keys[i2] = key2;
				values[i2] = value2;
			} else // collision
				values[i1] = refToInt(new MemoryNode(key1, value1, key2, value2,
						shift + BITS_PER_LEVEL));
		}

		private boolean isEmpty(int i) {
			return values[i] == 0;
		}

		private boolean isPointer(int i) {
			return keys[i] == 0;
		}

		private static int index(int key, int shift) {
			return (key >>> shift) & LEVEL_MASK;
		}

		private int countNodes(int shift) {
			int n = 1;
			for (int i = 0; i < 32; ++i)
				if (! isEmpty(i) && isPointer(i))
					n += ((MemoryNode) intToRef(values[i])).countNodes(shift + BITS_PER_LEVEL);
			return n;
		}

		private void print(int shift) {
			for (int i = 0; i < 32; ++i)
				if (isEmpty(i))
					continue;
				else if (! isPointer(i))
					System.out.println(Strings.repeat(" ", shift) + fmt(keys[i]));
				else {
					System.out.println(Strings.repeat(" ", shift) + ">>>>>>>>");
					((MemoryNode) intToRef(values[i])).print(shift + BITS_PER_LEVEL);
				}
		}

		private String fmt(int n) {
			String s = "";
			for (; n != 0; n >>>= 5)
				s = (n & 0x1f) + "." + s;
			return s.substring(0, s.length() - 1);
		}

	}

	private static int refToInt(Node ref) {
		return IntRefs.refToInt(ref);
	}

	private static Node intToRef(int index) {
		if (IntRefs.isIntRef(index))
			return (Node) IntRefs.intToRef(index);
		else
			throw new RuntimeException("reading db not implemented"); // TODO
	}

}
