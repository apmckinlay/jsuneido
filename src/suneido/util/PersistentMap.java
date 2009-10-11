package suneido.util;

import java.util.*;

import net.jcip.annotations.Immutable;

/**
 * An implementation of an immutable persistent map. Based on Phil Bagwell's
 * Hash Array Mapped Trie with some help from Rich Hickey's implementation in
 * Clojure. Uses OverflowNodes instead of extended hashing. The primary goal was
 * simplicity, not speed or space performance.
 *
 * @author Andrew McKinlay
 */
@Immutable
public class PersistentMap<K, V> extends AbstractMap<K, V> {

	private final int size;
	private final Node<K, V> node;

	private static final int BITS_PER_LEVEL = 5;
	private static final int LEVEL_MASK = (1 << BITS_PER_LEVEL) - 1;
	private static final int HASH_BITS = 32;

	@SuppressWarnings("unchecked")
	public static final <K, V> PersistentMap<K, V> empty() {
		return EmptyMap.emptyMap;
	}

	/** lazy initialization */
	@SuppressWarnings("unchecked")
	private static class EmptyMap {
		static PersistentMap emptyMap = new PersistentMap(0, emptyNode);
	}

	private PersistentMap(int size, Node<K, V> node) {
		this.size = size;
		this.node = node;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public V get(Object key) {
		return node.get(key, key.hashCode(), 0);
	}

	// PERF would be nice to eliminate the Added allocation,
	// maybe by creating and using the PersistentMap object
	// since it will normally be created anyway
	// although that would require making size not final
	public PersistentMap<K, V> with(K key, V value) {
		assert key != null;
		assert value != null;
		Added added = new Added();
		Node<K, V> n = node.with(key, value, key.hashCode(), 0, added);
		return n == node ? this // map already had it
				: new PersistentMap<K, V>(size + added.n, n);
	}

	@SuppressWarnings("unchecked")
	public PersistentMap<K, V> without(Object key) {
		Node<K, V> n = (Node<K, V>) node.without(key, key.hashCode(), 0);
		return n == node ? this // map didn't have it
				: n == null ? EmptyMap.emptyMap
				: new PersistentMap<K, V>(size - 1, n);
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	private static class Added {
		public int n = 0;
	}

	@SuppressWarnings("unchecked")
	private static TrieNode emptyNode = new TrieNode(0, new Entry[0]);

	private static abstract class Node<K, V> {
		public abstract V get(Object key, int hash, int shift);

		public abstract Node<K, V> with(K key, V value, int hash, int shift,
				Added added);

		/**
		 * @return either a Node, or null if empty, or sole remaining
		 *         SimpleImmutableEntry from OverflowNode
		 */
		public abstract Object without(Object key, int hash, int shift);
	}

	@Immutable
	@SuppressWarnings("unchecked")
	private static class TrieNode<K, V> extends Node<K, V> {

		final int bitmap; // 1's indicate existing slots in a
		final Object a[]; // size = number of 1's in bitmap

		TrieNode(int bitmap, Object a[]) {
			this.bitmap = bitmap;
			this.a = a;
		}

		@Override
		public V get(Object key, int hash, int shift) {
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
		public TrieNode<K, V> with(K key, V value, int hash, int shift, Added added) {
			SimpleImmutableEntry<K, V> assoc;
			int bm;
			Object aa[];
			int bit = bit(hash, shift);
			int i = Integer.bitCount(bitmap & (bit - 1));
			if ((bitmap & bit) == 0) { // not found
				aa = new Object[a.length + 1];
				added.n = 1;
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
					added.n = 1;
					aa = a.clone();
					aa[i] =
							newChild(assoc, key, value, hash, shift
									+ BITS_PER_LEVEL);
					return new TrieNode<K, V>(bitmap, aa);
				}
			} else { // slot points to child node
				Node<K, V> child = (Node<K, V>) a[i];
				Node<K, V> newchild =
						child.with(key, value, hash, shift + BITS_PER_LEVEL,
								added);
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
				aa[0] =
						newChild(assoc, key, value, hash, shift
								+ BITS_PER_LEVEL);
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
		public Object without(Object key, int hash, int shift) {
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

	}

	/** used for overflow leaf nodes when multiple identical hash */
	@Immutable
	@SuppressWarnings("unchecked")
	private static class OverflowNode<K, V> extends Node<K, V> {
		private final SimpleImmutableEntry<K, V> assocs[];

		public OverflowNode(SimpleImmutableEntry<K, V> assoc1,
				SimpleImmutableEntry<K, V> assoc2) {
			this.assocs = new SimpleImmutableEntry[2];
			assocs[0] = assoc1;
			assocs[1] = assoc2;
		}

		private OverflowNode(SimpleImmutableEntry<K, V>[] assocs) {
			this.assocs = assocs;
		}

		@Override
		public V get(Object key, int hash, int shift) {
			int i = find(key);
			return i == -1 ? null : assocs[i].getValue();
		}

		@Override
		public OverflowNode<K, V> with(K key, V value, int hash, int shift,
				Added added) {
			int i = find(key);
			SimpleImmutableEntry<K, V>[] a;
			if (i == -1) { // key not found
				a = Arrays.copyOf(assocs, assocs.length + 1);
				i = assocs.length;
				added.n = 1;
			} else if (assocs[i].getValue().equals(value))
				return this; // already there
			else
				a = assocs.clone();
			a[i] = new SimpleImmutableEntry<K, V>(key, value);
			return new OverflowNode<K, V>(a);
		}

		@Override
		public Object without(Object key, int hash, int shift) {
			int i = find(key);
			if (i == -1)
				return this; // not there
			if (assocs.length == 2)
				return assocs[i ^ 1];
			SimpleImmutableEntry<K, V> a[] =
					new SimpleImmutableEntry[assocs.length - 1];
			System.arraycopy(assocs, 0, a, 0, i);
			System.arraycopy(assocs, i + 1, a, i, assocs.length - i - 1);
			return new OverflowNode<K, V>(a);
		}

		private int find(Object key) {
			for (int i = 0; i < assocs.length; ++i)
				if (assocs[i].getKey().equals(key))
					return i;
			return -1;
		}

	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

}
