package suneido.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import net.jcip.annotations.Immutable;

/**
 * An persistent immutable map. Based on Phil Bagwell's Hash Array Mapped Trie
 * with some help from Rich Hickey's implementation in Clojure. Uses
 * OverflowNodes instead of extended hashing and does not resize root.
 *
 * @author Andrew McKinlay
 */
@Immutable
public abstract class PersistentMap<K, V> {

	private static final int BITS_PER_LEVEL = 5;
	private static final int LEVEL_MASK = (1 << BITS_PER_LEVEL) - 1;
	private static final int HASH_BITS = 32;

	@SuppressWarnings("unchecked")
	private static TrieNode emptyNode = new TrieNode(0, new Object[0]);

	@SuppressWarnings("unchecked")
	public static final <K, V> PersistentMap<K, V> empty() {
		return emptyNode;
	}

	public abstract V get(Object key);

	public abstract PersistentMap<K, V> with(K key, V value);

	public abstract PersistentMap<K, V> without(K key);

	// TODO size

	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	private static abstract class Node<K, V> extends PersistentMap<K, V> {
		@Override
		public V get(Object key) {
			return get(key, key.hashCode(), 0);
		}

		@Override
		public PersistentMap<K, V> with(K key, V value) {
			checkNotNull(key);
			checkNotNull(value);
			return with(key, value, key.hashCode(), 0);
		}

		@Override
		@SuppressWarnings("unchecked")
		public PersistentMap<K, V> without(Object key) {
			Node<K, V> n = (Node<K, V>) without(key, key.hashCode(), 0);
			return n == null ? emptyNode : n;
		}

		protected abstract Node<K, V> with(K key, V value, int hash, int shift);

		/**
		 * @return either a Node, or null if empty, or sole remaining
		 *         SimpleImmutableEntry from OverflowNode
		 */
		protected abstract Object without(Object key, int hash, int shift);

		protected abstract V get(Object key, int hash, int shift);

		// Only used by Builder
		protected void add(K key, V value) {
			checkNotNull(key);
			checkNotNull(value);
			add(key, value, key.hashCode(), 0);
		}

		// Only used by Builder
		protected abstract void add(K key, V value, int hash, int shift);
	}

	@Immutable
	@SuppressWarnings("unchecked")
	private static class TrieNode<K, V> extends Node<K, V> {

		// normally final/immutable but modified by Builder via add on private copy
		int bitmap; // 1's indicate existing slots in a
		Object a[]; // size = number of 1's in bitmap

		protected TrieNode(int bitmap, Object a[]) {
			this.bitmap = bitmap;
			this.a = a;
		}

		@Override
		protected V get(Object key, int hash, int shift) {
			int bit = bit(hash, shift);
			if ((bitmap & bit) == 0)
				return null; // slot empty
			int i = Integer.bitCount(bitmap & (bit - 1));
			if (a[i] instanceof SimpleImmutableEntry<?, ?>) {
				SimpleImmutableEntry<K, V> assoc =
						(SimpleImmutableEntry<K, V>) a[i];
				return assoc.getKey().equals(key) ? assoc.getValue() : null;
			} else {
				Node<K, V> child = (Node<K, V>) a[i];
				return child.get(key, hash, shift + BITS_PER_LEVEL);
			}
		}

		@Override
		protected void add(K key, V value, int hash, int shift) {
			SimpleImmutableEntry<K, V> assoc;
			int bit = bit(hash, shift);
			int i = Integer.bitCount(bitmap & (bit - 1));
			if ((bitmap & bit) == 0) { // not found
				int len = length();
				if (len + 1 > a.length)
					a = Arrays.copyOf(a, a.length * 2);
				bitmap |= bit;
				System.arraycopy(a, i, a, i + 1, len - i);
				// fall thru to bottom
			} else if (a[i] instanceof SimpleImmutableEntry) {
				assoc = (SimpleImmutableEntry<K, V>) a[i];
				if (assoc.getKey().equals(key)) {
					if (assoc.getValue().equals(value))
						return ; // entry already exists
					// fall through to bottom
				} else { // collision
					// push entry into child node along with new entry
					a[i] = newChild(assoc, key, value, hash,
							shift + BITS_PER_LEVEL);
					return ;
				}
			} else { // slot points to child node
				Node<K, V> child = (Node<K, V>) a[i];
				Node<K, V> newchild = child.with(key, value, hash,
						shift + BITS_PER_LEVEL);
				a[i] = newchild;
				return;
			}
			a[i] = new SimpleImmutableEntry<K, V>(key, value);
		}

		@Override
		protected TrieNode<K, V> with(K key, V value, int hash, int shift) {
			SimpleImmutableEntry<K, V> assoc;
			int bm;
			Object aa[];
			int bit = bit(hash, shift);
			int i = Integer.bitCount(bitmap & (bit - 1));
			if ((bitmap & bit) == 0) { // not found
				aa = new Object[a.length + 1];
				bm = bitmap | bit;
				System.arraycopy(a, 0, aa, 0, i);
				System.arraycopy(a, i, aa, i + 1, a.length - i);
				// fall thru to bottom
			} else if (a[i] instanceof SimpleImmutableEntry) {
				assoc = (SimpleImmutableEntry<K, V>) a[i];
				if (assoc.getKey().equals(key)) {
					if (assoc.getValue().equals(value))
						return this; // entry already exists
					aa = a.clone(); // same length since key already existed
					bm = bitmap;
					// fall through to bottom
				} else { // collision
					// push entry into child node along with new entry
					aa = a.clone();
					aa[i] = newChild(assoc, key, value, hash,
							shift + BITS_PER_LEVEL);
					return new TrieNode<K, V>(bitmap, aa);
				}
			} else { // slot points to child node
				Node<K, V> child = (Node<K, V>) a[i];
				Node<K, V> newchild =
						child.with(key, value, hash, shift + BITS_PER_LEVEL);
				if (newchild == child)
					return this; // entry already exists
				aa = a.clone();
				aa[i] = newchild;
				return new TrieNode<K, V>(bitmap, aa);
			}
			aa[i] = new SimpleImmutableEntry<K, V>(key, value);
			return new TrieNode<K, V>(bm, aa);
		}

		/** really static but needs K,V */
		private Node<K, V> newChild(SimpleImmutableEntry<K, V> assoc, K key,
				V value, int hash, int shift) {
			if (shift >= HASH_BITS)
				return new OverflowNode<K, V>(assoc, new SimpleImmutableEntry(key,
						value));
			int ha = (assoc.getKey().hashCode() >> shift) & LEVEL_MASK;
			int h = (hash >>> shift) & LEVEL_MASK;
			if (ha == h) { // collision
				Object[] aa = new Object[1];
				aa[0] = newChild(assoc, key, value, hash, shift	+ BITS_PER_LEVEL);
				return new TrieNode<K, V>(1, aa);
			}
			Object[] aa = new Object[2];
			SimpleImmutableEntry<K, V> newAssoc =
					new SimpleImmutableEntry(key, value);
			if (h < ha) {
				aa[0] = newAssoc;
				aa[1] = assoc;
			} else {
				aa[0] = assoc;
				aa[1] = newAssoc;
			}
			int bm = (1 << h) | (1 << ha);
			return new TrieNode<K, V>(bm, aa);
		}

		@Override
		protected Object without(Object key, int hash, int shift) {
			int bit = bit(hash, shift);
			if ((bitmap & bit) == 0)
				return this; // slot empty
			int i = Integer.bitCount(bitmap & (bit - 1));
			if (a[i] instanceof Node<?, ?>) {
				Object newChild =
						((Node<K, V>) a[i]).without(key, hash, shift
								+ BITS_PER_LEVEL);
				if (newChild == a[i])
					return this; // not present
				if (newChild != null) {
					Object aa[] = a.clone();
					aa[i] = newChild;
					return new TrieNode<K, V>(bitmap, aa);
				} // else fall through
			} else if (!((SimpleImmutableEntry<K, V>) a[i]).getKey().equals(key))
				return this; // slot has different key, key not present
			if (a.length == 1)
				return null;
			Object aa[] = new Object[a.length - 1];
			System.arraycopy(a, 0, aa, 0, i);
			System.arraycopy(a, i + 1, aa, i, a.length - i - 1);
			return new TrieNode<K, V>(bitmap & ~bit, aa);
		}

		private int bit(int hash, int shift) {
			int h = (hash >>> shift) & LEVEL_MASK;
			int bit = 1 << h;
			return bit;
		}

		private int length() {
			// could use bitCount instead
			int i = a.length - 1;
			while (i > 0 && a[i] == null)
				--i;
			return i + 1;
		}

	}

	/** Used for overflow leaf nodes when multiple identical hash */
	@Immutable
	@SuppressWarnings("unchecked")
	private static class OverflowNode<K, V> extends Node<K, V> {
		// normally final but modified by Builder via add on private copy
		private SimpleImmutableEntry<K, V> assocs[];

		private OverflowNode(SimpleImmutableEntry<K, V> assoc1,
				SimpleImmutableEntry<K, V> assoc2) {
			this.assocs = new SimpleImmutableEntry[2];
			assocs[0] = assoc1;
			assocs[1] = assoc2;
		}

		private OverflowNode(SimpleImmutableEntry<K, V>[] assocs) {
			this.assocs = assocs;
		}

		@Override
		protected V get(Object key, int hash, int shift) {
			int i = find(key);
			return i == -1 ? null : assocs[i].getValue();
		}

		@Override
		protected void add(K key, V value, int hash, int shift) {
			int i = find(key);
			if (i == -1) { // key not found
				i = length();
				if (i > assocs.length - 1)
					assocs = Arrays.copyOf(assocs, assocs.length * 2);
			}
			assocs[i] = new SimpleImmutableEntry<K, V>(key, value);
		}

		@Override
		protected OverflowNode<K, V> with(K key, V value, int hash, int shift) {
			int i = find(key);
			SimpleImmutableEntry<K, V>[] a;
			if (i == -1) { // key not found
				i = length();
				a = Arrays.copyOf(assocs, i + 1);
			} else if (assocs[i].getValue().equals(value))
				return this; // already there
			else
				// key exists but value wrong
				a = Arrays.copyOf(assocs, length());
			a[i] = new SimpleImmutableEntry<K, V>(key, value);
			return new OverflowNode<K, V>(a);
		}

		@Override
		protected Object without(Object key, int hash, int shift) {
			int i = find(key);
			if (i == -1)
				return this; // not there
			if (length() == 2)
				return assocs[i ^ 1];
			SimpleImmutableEntry<K, V> a[] = new SimpleImmutableEntry[length() - 1];
			System.arraycopy(assocs, 0, a, 0, i);
			System.arraycopy(assocs, i + 1, a, i, a.length - i);
			return new OverflowNode<K, V>(a);
		}

		private int find(Object key) {
			for (int i = 0; i < length(); ++i)
				if (assocs[i].getKey().equals(key))
					return i;
			return -1;
		}

		private int length() {
			int i = assocs.length - 1;
			while (i > 0 && assocs[i] == null)
				--i;
			return i + 1;
		}

	}

	public static class Builder<K, V> {

		private Node<K, V> map = new TrieNode<K, V>(0, new Object[4]);

		public Builder<K, V> put(K key, V value) {
			map.add(key, value);
			return this;
		}

		public Builder<K, V> putAll(Map<? extends K, ? extends V> m) {
			for (Entry<? extends K, ? extends V> e : m.entrySet())
				map.add(e.getKey(), e.getValue());
			return this;
		}

		/**
		 * The Builder cannot be used after calling build()
		 * @return The working map, which may contain some unused space, but
		 *         doesn't require copying.
		 */
		public PersistentMap<K, V> build() {
			PersistentMap<K, V> result = map;
			map = null;
			return result;
		}
	}

	public static <K, V> Builder<K, V> builder() {
		return new Builder<K, V>();
	}

}
