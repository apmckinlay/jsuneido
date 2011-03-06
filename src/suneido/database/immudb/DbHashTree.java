/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.util.IntArrayList;

import com.google.common.base.Strings;

// TODO persist values that are Tran

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

	public static DbHashTree empty(Tran tran) {
		return new MemNode(tran);
	}

	public static DbHashTree from(Tran tran, int at) {
		return new DbNode(tran, at);
	}

	public abstract int get(int key);

	public abstract DbHashTree with(int key, int value);

	public abstract int persist(MmapFile mmf);

	public void print() {
		((Node) this).print(0);
	}

	private abstract static class Node extends DbHashTree {
		protected final Tran tran;

		Node(Tran tran) {
			this.tran = tran;
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
				Node child = intToRef(tran, value(i));
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
					intToRef(tran, value(i)).print(shift + BITS_PER_LEVEL);
				}
			}
		}

	}

	/** DbNode consists of:
	 * 		present	- bitmap of which entries are present
	 * 		entries	- up to 32 entries, each a pair of int's
	 * 		if entry key is 0 then value points to a child node
	 */
	@Immutable
	private static class DbNode extends Node {
		private static final int ENTRIES = INT_BYTES;
		private static final int ENTRY_SIZE = 2 * INT_BYTES;
		private final int adr;
		private final ByteBuffer buf;

		DbNode(Tran tran, int adr) {
			super(tran);
			this.adr = adr;
			buf = tran.mmf().buffer(adr);
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
				int ptr = tran.refToInt(intToRef(tran, value(i)).with(key, value,
						shift + BITS_PER_LEVEL));
				return new MemNode(this, i, ptr);
			} else { // collision
				int ptr = tran.refToInt(new MemNode(tran, key(i), value(i), key, value,
						shift + BITS_PER_LEVEL));
				return new MemNode(this, i, ptr);
			}
		}

		@Override
		public int persist(MmapFile mmf) {
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
		private final IntArrayList values;

		MemNode(Tran tran) {
			super(tran);
			present = 0;
			keys = new IntArrayList();
			values = new IntArrayList();
		}

		MemNode(DbNode dbn) {
			super(dbn.tran);
			present = dbn.present();
			int n = size();
			keys = new IntArrayList(n + 1);
			values = new IntArrayList(n + 1);
			for (int i = 0; i < n; ++i) {
				keys.add(dbn.key(i));
				values.add(dbn.value(i));
			}
		}

		MemNode(DbNode dbn, int i, int ptr) {
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
				values.set(i, tran.refToInt(intToRef(tran,
						values.get(i)).with(key, value, shift + BITS_PER_LEVEL)));
			} else { // collision, change entry to pointer
				values.set(i, tran.refToInt(new MemNode(tran, keys.get(i), values.get(i), key, value,
					shift + BITS_PER_LEVEL)));
				keys.set(i, KEY_FOR_CHILD);
			}
			return this;
		}

		private MemNode(Tran tran, int key1, int value1, int key2, int value2, int shift) {
			this(tran);
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
				values.add(tran.refToInt(new MemNode(tran, key1, value1, key2, value2,
						shift + BITS_PER_LEVEL)));
				present = (1 << bits1);
			}
		}

		@Override
		public int persist(MmapFile mmf) {
			for (int i = 0; i < size(); ++i)
				if (isPointer(i)) {
					if (IntRefs.isIntRef(values.get(i)))
						values.set(i, intToRef(tran, values.get(i)).persist(mmf));
				} else {
					if (IntRefs.isIntRef(values.get(i))) {
						int adr = tran.getAdr(values.get(i));
						if (adr == 0)
							throw new Error("redirect still intref at persist");
						values.set(i, adr);
					}
				}
			int adr = mmf.alloc(byteBufSize());
			ByteBuffer buf = mmf.buffer(adr);
			toByteBuf(buf);
			return adr;
		}


		public void toByteBuf(ByteBuffer buf) {
			buf.putInt(present);
			for (int i = 0; i < size(); ++i) {
				buf.putInt(keys.get(i));
				buf.putInt(values.get(i));
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
			return values.get(i);
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

	private static Node intToRef(Tran tran, int at) {
		if (IntRefs.isIntRef(at))
			return (Node) tran.intToRef(at);
		else
			return new DbNode(tran, at);
	}

}
