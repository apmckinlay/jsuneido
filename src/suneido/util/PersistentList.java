package suneido.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.*;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Joiner;

/**
 * A persistent immutable single linked list class.
 *
 * @author Andrew McKinlay
 */
@Immutable
public class PersistentList<T> extends AbstractSequentialList<T> {

	private final T value;
	// logically final, but Builder uses mutable private copies
	private PersistentList<T> next;

	private PersistentList(T value, PersistentList<T> next) {
		this.value = value;
		this.next = next;
	}

	@SuppressWarnings("rawtypes")
	private final static PersistentList Nil = makeNil();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static PersistentList makeNil() {
		PersistentList nil = new PersistentList(null, null);
		nil.next = nil;
		return nil;
	}

	/** @return The empty list */
	@SuppressWarnings("unchecked")
	public static <T> PersistentList<T> nil() {
		return Nil;
	}

	/** @return A list containing the specified value */
	@SuppressWarnings("unchecked")
	public static <T> PersistentList<T> of(T value) {
		return new PersistentList<T>(value, Nil);
	}

	/** @return A list containing the specified values */
	@SuppressWarnings("unchecked")
	public static <T> PersistentList<T> of(T v1, T v2) {
		return Nil.with(v2).with(v1);
	}

	/** @return A list containing the specified values */
	@SuppressWarnings("unchecked")
	public static <T> PersistentList<T> of(T v1, T v2, T v3) {
		return Nil.with(v3).with(v2).with(v1);
	}

	/** @return A list containing the specified values */
	@SuppressWarnings("unchecked")
	public static <T> PersistentList<T> of(T... values) {
		PersistentList<T> list = Nil;
		for (int i = values.length - 1; i >= 0; --i)
			list = list.with(values[i]);
		return list;
	}

	/** @return A new list with the values from the Iterable */
	public static <T> PersistentList<T> copyOf(Iterable<T> values) {
		return new Builder<T>().addAll(values).build();
	}

	/** @return The value at the start of the list */
	public T head() {
		return value;
	}

	/** @return The remainder of the list without the first element */
	public PersistentList<T> tail() {
		return next;
	}

	/** @return A new list with value as the head and the old list as the tail */
	public PersistentList<T> with(T value) {
		checkNotNull(value);
		return new PersistentList<T>(value, this);
	}

	@Override
	public T get(int i) {
		if (i >= 0)
			for (PersistentList<T> list = this; list != Nil; list = list.tail())
				if (i-- == 0)
					return list.head();
		throw new IndexOutOfBoundsException();
	}

	/**
	 * Note: O(N)
	 * @return A new list omitting the specified value
	 */
	public PersistentList<T> without(T x) {
		if (!contains(x))
			return this;
		Builder<T> save = new Builder<T>();
		PersistentList<T> list = this;
		do {
			for (; list != Nil && !x.equals(list.head()); list = list.tail())
				save.add(list.head());
			while (list != Nil && x.equals(list.head()))
				list = list.tail();
		} while (list.contains(x));
		// list is now the longest tail that doesn't contain x
		return save.buildOnto(list);
	}

	/** Note: O(N) */
	@Override
	public boolean contains(Object value) {
		for (PersistentList<T> list = this; list != Nil; list = list.next)
			if (value.equals(list.head()))
				return true;
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof PersistentList))
			return false;
		PersistentList<? extends Object> x = this;
		PersistentList<Object> y = (PersistentList<Object>) other;
		for (; x != Nil && y != Nil; x = x.tail(), y = y.tail())
			if (!x.head().equals(y.head()))
				return false;
		return x == Nil && y == Nil;
	}

	public static final Joiner commaJoiner = Joiner.on(",");

	@Override
	public String toString() {
		return "(" + commaJoiner.join(this) + ")";
	}

	/**
	 * Note: O(N)
	 * @return The number of elements in the list
	 */
	@Override
	public int size() {
		int size = 0;
		for (PersistentList<T> list = this; list != Nil; list = list.next)
			++size;
		return size;
	}

	@Override
	public boolean isEmpty() {
		return this == Nil;
	}

	/** @return A new list with the elements in the reverse order */
	public PersistentList<T> reversed() {
		PersistentList<T> list = nil();
		for (PersistentList<T> p = this; p != Nil; p = p.next)
			list = list.with(p.value);
		return list;
	}

	public static class Builder<T> {

		private PersistentList<T> list = nil();

		public Builder<T> add(T value) {
			list = list.with(value);
			return this;
		}

		public Builder<T> addAll(Iterable<? extends T> values) {
			for (T value : values)
				add(value);
			return this;
		}

		/**
		 * The Builder cannot be used after calling build()
		 * @return The list
		 */
		@SuppressWarnings("unchecked")
		public PersistentList<T> build() {
			return buildOnto((PersistentList<T>) nil());
		}

		public PersistentList<T> buildOnto(PersistentList<T> tail) {
			// reverse in place by changing pointers (no allocation)
			for (PersistentList<T> p = list; p != nil();) {
				PersistentList<T> next = p.next;
				p.next = tail;
				tail = p;
				p = next;
			}
			list = null;
			return tail;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iter<T>(this);
	}

	private static class Iter<T> implements Iterator<T> {

		private PersistentList<T> list;

		private Iter(PersistentList<T> list) {
			this.list = new PersistentList<T>(null, list);
		}

		public boolean hasNext() {
			return list.tail() != Nil;
		}

		public T next() {
			list = list.tail();
			return list.head();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

}
