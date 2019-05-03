package suneido.util;

public class Sort {
	private static final int QUICKSORT_NO_REC = 16;
	private static final int QUICKSORT_MEDIAN_OF_9 = 128;

	@FunctionalInterface
	public interface Swapper {
		void swap(int a, int b);
	}

	public static void sort(int[] array, IntComparator comp) {
		quickSort(0, array.length,
			(i, j) -> comp.compare(array[i], array[j]),
			(i, j) -> { int tmp = array[i]; array[i] = array[j]; array[j] = tmp; });
	}

	public static void sort(int[] array, int from, int to, IntComparator comp) {
		quickSort(from, to,
			(i, j) -> comp.compare(array[i], array[j]),
			(i, j) -> { int tmp = array[i]; array[i] = array[j]; array[j] = tmp; });
	}

	public static void quickSort(final int from, final int to,
			final IntComparator comp, final Swapper swapper) {
		final int len = to - from;
		// Insertion sort on smallest arrays
		if (len < QUICKSORT_NO_REC) {
			for (int i = from; i < to; i++)
				for (int j = i; j > from && (comp.compare(j - 1, j) > 0); j--) {
					swapper.swap(j, j - 1);
				}
			return;
		}

		// Choose a partition element, v
		int m = from + len / 2; // Small arrays, middle element
		int l = from;
		int n = to - 1;
		if (len > QUICKSORT_MEDIAN_OF_9) { // Big arrays, pseudomedian of 9
			int s = len / 8;
			l = med3(l, l + s, l + 2 * s, comp);
			m = med3(m - s, m, m + s, comp);
			n = med3(n - 2 * s, n - s, n, comp);
		}
		m = med3(l, m, n, comp); // Mid-size, med of 3
		// int v = x[m];

		int a = from;
		int b = a;
		int c = to - 1;
		// Establish Invariant: v* (<v)* (>v)* v*
		int d = c;
		while (true) {
			int comparison;
			while (b <= c && ((comparison = comp.compare(b, m)) <= 0)) {
				if (comparison == 0) {
					// Fix reference to pivot if necessary
					if (a == m) m = b;
					else if (b == m) m = a;
					swapper.swap(a++, b);
				}
				b++;
			}
			while (c >= b && ((comparison = comp.compare(c, m)) >= 0)) {
				if (comparison == 0) {
					// Fix reference to pivot if necessary
					if (c == m) m = d;
					else if (d == m) m = c;
					swapper.swap(c, d--);
				}
				c--;
			}
			if (b > c) break;
			// Fix reference to pivot if necessary
			if (b == m) m = d;
			else if (c == m) m = c;
			swapper.swap(b++, c--);
		}

		// Swap partition elements back to middle
		int s;
		s = Math.min(a - from, b - a);
		swap(swapper, from, b - s, s);
		s = Math.min(d - c, to - d - 1);
		swap(swapper, b, to - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) quickSort(from, from + s, comp, swapper);
		if ((s = d - c) > 1) quickSort(to - s, to, comp, swapper);
	}

	private static int med3(final int a, final int b, final int c, final IntComparator comp) {
		int ab = comp.compare(a, b);
		int ac = comp.compare(a, c);
		int bc = comp.compare(b, c);
		return (ab < 0 ?
				(bc < 0 ? b : ac < 0 ? c : a) :
				(bc > 0 ? b : ac > 0 ? c : a));
	}

	protected static void swap(final Swapper swapper, int a, int b, final int n) {
		for (int i = 0; i < n; i++, a++, b++) swapper.swap(a, b);
	}

}

