/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import suneido.util.ByteBuf;

/**
 * Persistent immutable hash tree used for storing redirections.
 * Key and value are both int's so no hashing or overflow is required.
 * <p>
 * Based on <a href="http://lampwww.epfl.ch/papers/idealhashtrees.pdf">Bagwell's</a>
 */
public class DbHashTree {
	private static final int BITS_PER_LEVEL = 4;
	private static final int HASH_BITS = 1 << BITS_PER_LEVEL;
	private static final int LEVEL_MASK = HASH_BITS - 1;

	/** Node consists of:
	 * 		short present	// bitmap of which keys are in entries
	 * 		short value		// bitmap of which keys have values (not pointers)
	 * 		Entry entries	// up to 16 entries, each a pair of int's
	 */
	private static class Node {
		private static final int PRESENT = 0;
		private static final int VALUE = PRESENT + Short.SIZE;
		private static final int ENTRIES = VALUE + Short.SIZE;
		private static final int ENTRY_SIZE = 2 * Integer.SIZE;
		private ByteBuf buf;

		@SuppressWarnings("unused")
                public ByteBuf with(int key, int value) {
			return with(key, value, 0);
		}
		private ByteBuf with(int key, int value, int shift) {
			int bit = bit(key, shift);
			int i = Integer.bitCount(present() & (bit - 1));
			ByteBuf nu;
			if ((present() & bit) == 0) {
				nu = ByteBuf.allocate(buf.size() + ENTRY_SIZE);
				nu.putInt(PRESENT, present() | bit);
				nu.putInt(VALUE, value() | bit);
				nu.put(ENTRIES, buf.slice(0, i * ENTRY_SIZE));
				nu.putInt(ENTRIES + i * ENTRY_SIZE, key);
				nu.put(ENTRIES, buf.slice(ENTRIES + i * ENTRY_SIZE));
			} else if ((value() & bit) != 0) {
				int entryKey = key(i);
				int entryValue = valueOrPointer(i);
				if (entryKey == key) {
					if (entryValue == value)
						return buf; // entry already exists
					nu = buf.copy();
					nu.putInt(PRESENT, present() | bit);
					nu.putInt(VALUE, value() | bit);
					nu.put(ENTRIES, buf.slice(0, i * ENTRY_SIZE));
					nu.putInt(ENTRIES + i * ENTRY_SIZE, key);
				} else { // collision
					// push entry into child node along with new entry
					nu = buf.copy();
					nu.putInt(VALUE, value() & ~bit); // change to pointer
					value = newChild(key, value, shift + BITS_PER_LEVEL);
				}
			} else { // slot points to child node
				Node child = getNode(valueOrPointer(i));
				ByteBuf newchild = child.with(key, value, shift + BITS_PER_LEVEL);
				if (newchild == child.buf)
					return buf; // entry already existed
				nu = buf.copy();
				value = pointerTo(newchild);
			}
			valueOrPointer(nu, i, value);
			return nu;
		}

		private int newChild(int key, int value, int i) {
			// TODO newChild
			return 0;
		}
		/** returns 0 if key not present */
		@SuppressWarnings("unused")
                public int get(int key) {
			return get(key, 0);
		}
		private int get(int key, int shift) {
			int bit = bit(key, shift);
			if ((present() & bit) == 0)
				return 0;
			int i = Integer.bitCount(present() & (bit - 1));
			int entryKey = key(i);
			if ((value() & bit) != 0)
				return entryKey == key ? valueOrPointer(i) : 0;
			else {
				Node child = getNode(valueOrPointer(i));
				return child.get(key, shift + BITS_PER_LEVEL);
			}
		}

		private static int bit(int key, int shift) {
			int h = (key >>> shift) & LEVEL_MASK;
			int bit = 1 << h;
			return bit;
		}

		short present() {
			return buf.getShort(0);
		}
		short value() {
			return buf.getShort(VALUE);
		}
		int key(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE);
		}
		int valueOrPointer(int i) {
			return buf.getInt(ENTRIES + i * ENTRY_SIZE + Integer.SIZE);
		}
		void valueOrPointer(ByteBuf buf, int i, int value) {
			buf.putInt(ENTRIES + i * ENTRY_SIZE + Integer.SIZE, value);
		}
	}

	private static Node getNode(int pointer) {
		return null;
	}

	private static int pointerTo(ByteBuf buf) {
		// TODO pointerTo
		// keep a bimap of sequential id to bytebuf
		// used to have int pointers to in-memory data
		// prior to it being written out
		// maybe share with other types of data
		return 0;
	}

}
