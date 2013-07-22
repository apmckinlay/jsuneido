package suneido.language.jsdi.tools;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Simple class to tally up the number of occurrences of various events and
 * print a summary in descending order of the number of occurrences.
 * @author Victor Schappert
 * @since 20130709
 * @see DllSummarizer
 *
 * @param <K> "Key" class which represents a unique occurrence. For example, if
 * you wanted to count the number of occurrences of the numbers 1..10, you
 * might use {@link Integer} as the key.
 */
public final class SummaryBucket<K> {

	//
	// TYPES
	//

	private static final class Entry<H> {
		public final H   key;
		public       int count;
		public Entry(H key) {
			this.key = key;
			this.count = 1;
		}
	}

	//
	// DATA
	//

	private final HashMap<K, Entry<K>> map;

	//
	// CONSTRUCTORS
	//

	/**
	 * Construct an empty summary bucket.
	 */
	public SummaryBucket() {
		map = new HashMap<K, Entry<K>>();
	}

	//
	// MUTATORS
	//

	/**
	 * Increments the tally for a given key. If the key was not previously in
	 * the bucket, it is added with a count of 1.
	 * @param key Key to tally
	 */
	public void tally(K key) {
		final Entry<K> entry = map.get(key);
		if (null == entry)
			map.put(key, new Entry<K>(key));
		else
			++entry.count;
	}

	//
	// ACCESSORS
	//

	/**
	 * Prints each key, followed by its tally, to the given stream. The keys
	 * are printed in reverse order of tally (key with the most tallies at the
	 * top), with ties being broken by the "natural" ordering of the keys.
	 * @param ps Stream to print to
	 * @see #top(PrintStream, int)
	 * @see #top(PrintStream, int, Comparator)
	 */
	public void summarize(PrintStream ps) {
		top(ps, Integer.MAX_VALUE);
	}

	/**
	 * Summarizes up to {@code number} keys to the given stream. The keys are
	 * printed in reverse order of tally (key with the most allies at the top),
	 * with ties broken according to the "natural" ordering of the keys.
	 * @param ps Stream to print to
	 * @param number Maximum number of lines to print
	 * @see #summarize(PrintStream)
	 * @see #top(PrintStream, int, Comparator)
	 */
	public void top(PrintStream ps, int number) {
		top(ps, number, new Comparator<K>()
		{
			@Override
			public int compare(K k1, K k2) {
				@SuppressWarnings("unchecked")
				Comparable<K> kk1 = (Comparable<K>)k1;
				return kk1.compareTo(k2);
			}
		});
	}

	/**
	 * Summarizes up to {@code number} keys to the given stream. The keys are
	 * printed in reverse order of tally (key with the most allies at the top),
	 * with ties broken according the given comparator.
	 * @param ps Stream to print to
	 * @param number Maximum number of lines to print
	 * @param comparator Comparator to use to break ties
	 * @see #summarize(PrintStream)
	 * @see #top(PrintStream, int)
	 */
	public void top(PrintStream ps, int number, final Comparator<? super K> comparator) {
		@SuppressWarnings("unchecked")
		Entry<K>[] arr = map.values().toArray(new Entry[map.size()]);
		Arrays.sort(arr, new Comparator<Entry<K>>() {
			@Override
			public int compare(Entry<K> a, Entry<K> b) {
				int diff = b.count - a.count; // sort high to low
				return 0 == diff ? comparator.compare(a.key, b.key) : diff;
			}
		});
		final int N = arr.length;
		final int M = Math.min(number, N);
		int k = 0, total = 0;
		for (; k < M; ++k) {
			ps.print(arr[k].key.toString());
			ps.print('\t');
			ps.println(arr[k].count);
			total += arr[k].count;
		}
		int remainder = 0;
		for (; k < N; ++k)
			remainder += arr[k].count;
		ps.print("REMAINDER\t");
		ps.println(remainder);
		ps.print("TOTAL\t");
		ps.println(total);
	}
}
