/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.util.IntArrayList;

import com.google.common.base.Strings;

/**
 * Persistent hash tree used for storing redirections.
 * Immutable in the database but mutable in memory.
 * <p>
 * Based on <a href="http://lampwww.epfl.ch/papers/idealhashtrees.pdf">
 * Bagwell's Ideal Hash Trees</a>
 * Key and value are both int's so no hashing or overflow is required.
 * <p>
 * Similar to {@link suneido.util.PersistentMap}
 */
public abstract class DbHashTree {
	private static final int BITS_PER_LEVEL = 5;
	private static final int HASH_BITS = 1 << BITS_PER_LEVEL;
	private static final int LEVEL_MASK = HASH_BITS - 1;
	private static final int INT_BYTES = Integer.SIZE / 8;

	public static DbHashTree empty(Storage stor) {
		return new MemNode(stor);
	}

	public static DbHashTree from(Storage stor, int at) {
		return new DbNode(stor, at);
	}

	public abstract int get(int key);

	public abstract DbHashTree with(int key, int value);

	public abstract int store(Storage stor, Translator translator);

	public void print() {
		((Node) this).print(0);
	}

	private abstract static class Node extends DbHashTree {
		protected final Storage stor;

		Node(Storage stor) {
			this.stor = stor;
		}

		/** returns 0 if key not present */
		@Override
		public int get(int key) {
			checkArgument(key != 0);
			return get(key, 0);
		}
		protected int get(int key, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			int present = present();
			if ((present & bit) == 0)
				return 0;
			int i = Integer.bitCount(present & (bit - 1));
			int entryKey = key(i);
			if (entryKey != 0)
				return entryKey == key ? value(i) : 0;
			else { // pointer
				Node child = child(i);
				return child.get(key, shift + BITS_PER_LEVEL);
			}
		}

		protected static int bit(int key, int shift) {
			int h = (key >>> shift) & LEVEL_MASK;
			return 1 << h;
		}

		protected int size() {
			return Integer.bitCount(present());
		}

		abstract int present();
		abstract int key(int i);
		abstract int value(int i);
		abstract Node child(int i);

		@Override
		public Node with(int key, int value) {
			checkArgument(key != 0);
			checkArgument(value != 0);
			return with(key, value, 0);
		}
		abstract protected Node with(int key, int value, int shift);

		protected boolean isPointer(int i) {
			return key(i) == 0;
		}

		private void print(int shift) {
			String indent = Strings.repeat(" ", shift);
			System.out.println(indent + this.getClass().getSimpleName());
			for (int i = 0; i < size(); ++i) {
				if (! isPointer(i))
					System.out.println(indent + fmt(key(i)) +
							"\t" + Integer.toHexString(value(i)));
				else {
					System.out.println(indent + ">>>>>>>>");
					load(stor, value(i)).print(shift + BITS_PER_LEVEL);
				}
			}
		}

	}

	/** DbNode consists of:
	 * 		present	- bitmap (in an int) specifying which entries are present
	 * 		entries	- up to 32 entries, each a pair of int's
	 * 		if entry key is 0 then value points to a child node
	 */
	@Immutable
	private static class DbNode extends Node {
		private static final int ENTRIES = INT_BYTES;
		private static final int ENTRY_SIZE = 2 * INT_BYTES;
		private final ByteBuffer buf;

		DbNode(Storage stor, int adr) {
			super(stor);
			buf = stor.buffer(adr);
		}

		@Override
		protected Node with(int key, int value, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			int i = Integer.bitCount(present() & (bit - 1));
			if ((present() & bit) == 0) {
				return new MemNode(this).with(key, value, shift);
			}
			int entryKey = key(i);
			if (entryKey == key) {
				return (value(i) == value)
					? this
					: new MemNode(this).with(key, value, shift);
			}
			if (entryKey == 0) { // value is pointer to child
				Node node = load(stor, value(i)).with(key, value, shift + BITS_PER_LEVEL);
				return new MemNode(this, i, node);
			} else { // collision
				Node node = new MemNode(stor, key(i), value(i), key, value, shift + BITS_PER_LEVEL);
				return new MemNode(this, i, node);
			}
		}

		@Override
		public int store(Storage stor, Translator translator) {
			throw new UnsupportedOperationException();
		}

		@Override
		int present() {
			return buf.getInt(0);
		}

		@Override
		int key(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE);
		}
		@Override
		int value(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE + INT_BYTES);
		}

		@Override
		Node child(int i) {
			return load(stor, value(i));
		}

	}

	/**
	 * In-memory mutable node used while transaction is in progress.
	 * If entry value is 0 then entry is unused/empty
	 */
	@NotThreadSafe
	private static class MemNode extends Node {
		private static final int KEY_FOR_CHILD = 0;
		private int present;
		private final IntArrayList keys;
		private final ArrayList<Object> values;

		MemNode(Storage stor) {
			super(stor);
			present = 0;
			keys = new IntArrayList();
			values = new ArrayList<Object>();
		}

		MemNode(DbNode dbn) {
			super(dbn.stor);
			present = dbn.present();
			int n = size();
			keys = new IntArrayList(n + 1);
			values = new ArrayList<Object>(n + 1);
			for (int i = 0; i < n; ++i) {
				keys.add(dbn.key(i));
				values.add(dbn.value(i));
			}
		}

		MemNode(DbNode dbn, int i, Object ptr) {
			this(dbn);
			keys.set(i, KEY_FOR_CHILD);
			values.set(i, ptr);
		}

		@Override
		protected MemNode with(int key, int value, int shift) {
			assert shift < 32;
			int bit = bit(key, shift);
			int i = Integer.bitCount(present & (bit - 1));
			if ((present & bit) == 0) {
				keys.add(i, key);
				values.add(i, value);
				present |= bit;
			} else if (keys.get(i) == key) {
				values.set(i, value);
			} else if (isPointer(i)) {
				Object ptr = values.get(i);
				if (ptr instanceof Integer)
					ptr = load(stor, (Integer) ptr);
				values.set(i, ((Node) ptr).with(key, value, shift + BITS_PER_LEVEL));
			} else { // collision, change entry to pointer
				values.set(i, new MemNode(stor, keys.get(i), values.get(i), key, value,
					shift + BITS_PER_LEVEL));
				keys.set(i, KEY_FOR_CHILD);
			}
			return this;
		}

		private MemNode(Storage stor, int key1, Object value1, int key2, int value2, int shift) {
			this(stor);
			assert shift < 32;
			assert key1 != key2;
			int bits1 = (key1 >>> shift) & LEVEL_MASK;
			int bits2 = (key2 >>> shift) & LEVEL_MASK;
			if (bits1 != bits2) {
				if (bits1 < bits2) {
					keys.add(key1);
					keys.add(key2);
					values.add(value1);
					values.add(value2);
				} else {
					keys.add(key2);
					keys.add(key1);
					values.add(value2);
					values.add(value1);
				}
				present = (1 << bits1) | (1 << bits2);
			} else { // collision
				keys.add(KEY_FOR_CHILD);
				values.add(new MemNode(stor, key1, value1, key2, value2,
						shift + BITS_PER_LEVEL));
				present = (1 << bits1);
			}
		}

		@Override
		public int store(Storage stor, Translator translator) {
			for (int i = 0; i < size(); ++i)
				if (isPointer(i)) {
					if (values.get(i) instanceof MemNode) {
						int adr = ((MemNode) values.get(i)).store(stor, translator);
						values.set(i, adr);
					}
				} else {
					int value = (Integer) values.get(i);
					value = translator.translate(value);
assert(value != 0);
					values.set(i, value);
				}
			int adr = stor.alloc(byteBufSize());
			ByteBuffer buf = stor.buffer(adr);
			toByteBuf(buf);
			return adr;
		}


		public void toByteBuf(ByteBuffer buf) {
			buf.putInt(present);
			for (int i = 0; i < size(); ++i) {
				buf.putInt(keys.get(i));
				buf.putInt((Integer) values.get(i));
			}
		}

		public int byteBufSize() {
			return INT_BYTES + // present
					(2 * size() * INT_BYTES); // keys and values
		}

		@Override
		int present() {
			return present;
		}

		@Override
		int key(int i) {
			return keys.get(i);
		}

		@Override
		int value(int i) {
			return (Integer) values.get(i);
		}

		@Override
		Node child(int i) {
			Object ptr = values.get(i);
			if (ptr instanceof Node)
				return (Node) ptr;
			else
				return new DbNode(stor, (Integer) ptr);
		}

	}

	public static String fmt(int n) {
		if (n == 0)
			return "0";
		String s = "";
		for (; n != 0; n >>>= 5)
			s = (n & 0x1f) + "." + s;
		return s.substring(0, s.length() - 1);
	}

	private static Node load(Storage stor, int at) {
		return new DbNode(stor, at);
	}

}
