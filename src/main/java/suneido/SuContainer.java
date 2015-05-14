/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.Suneido.dbpkg;
import static suneido.runtime.Numbers.toBigDecimal;
import static suneido.util.Verify.verify;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.query.Header;
import suneido.intfc.database.Record;
import suneido.intfc.database.RecordBuilder;
import suneido.runtime.Ops;
import suneido.runtime.Pack;
import suneido.runtime.Range;
import suneido.runtime.SuInstance;
import suneido.runtime.builtin.ContainerMethods;
import suneido.util.NullIterator;
import suneido.util.PairStack;
import suneido.util.Util;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterables;

//TODO detect the same modification-during-iteration as cSuneido (see ObjectsTest)
//TODO resolve concurrency issues flagged below

/**
 * Suneido's single container type.
 * Combines an extendible array plus a hash map.
 * vec and map are synchronized for partial thread safety
 */
@NotThreadSafe // i.e. objects/records should be thread contained
public class SuContainer extends SuValue
		implements Comparable<SuContainer>, Iterable<Object> {
	public final List<Object> vec;
	private final Map<Object,Object> map;
	protected Object defval = null;
	private boolean readonly = false;
	public final static SuContainer EMPTY = new SuContainer(Empty.EMPTY);

	@SuppressWarnings("serial")
	private static class CanonicalMap extends HashMap<Object, Object> {
		@Override
		public Object get(Object key) {
			return super.get(canonical(key));
		}
		@Override
		public Object put(Object key, Object value) {
			assert key != null;
			assert value != null;
			return super.put(canonical(key), value);
		}
		@Override
		public Object remove(Object key) {
			return super.remove(canonical(key));
		}
		@Override
		public boolean containsKey(Object key) {
			return super.containsKey(canonical(key));
		}
	}

	public SuContainer(int vecCapacity) {
		vec = Collections.synchronizedList(new ArrayList<>(vecCapacity));
		map = Collections.synchronizedMap(new CanonicalMap());
	}

	public SuContainer() {
		this(10);
	}

	/** create a new container and add the specified values */
	public SuContainer(Iterable<?> c) {
		this(10);
		addAll(c);
	}

	public SuContainer(SuContainer other) {
		this(other.vecSize());
		vec.addAll(other.vec);
		map.putAll(other.map);
		defval = other.defval;
	}
	
	enum Empty { EMPTY };
	private SuContainer(Empty empty) {
		vec = Collections.emptyList();
		map = Collections.emptyMap();
		readonly = true;
	}

	public static SuContainer of(Object x, Object y) {
		SuContainer c = new SuContainer();
		c.add(x);
		c.add(y);
		return c;
	}

	/**
	 * Constructs an SuContainer from an array of key-value pairs.
	 * @param pairs Even-length array of objects where
	 *              <code>pairs<sub>i</sub></code> represents the key to insert
	 *              and <code>pairs<sub>i+1</sub></code> represents the value.
	 * @return New container
	 * @author Victor Schappert
	 * @since 20130712
	 */
	public static SuContainer fromKVPairs(Object... pairs) {
		final int N = pairs.length;
		assert 0 == N % 2 : "pairs must contain an even number of elements";
		SuContainer c = new SuContainer();
		for (int k = 0; k < N;) {
			c.preset(pairs[k++], pairs[k++]);
		}
		return c;
	}

	public Object vecGet(int i) {
		return vec.get(i);
	}
	public Object mapGet(Object key) {
		return map.get(key);
	}
	public Set<Map.Entry<Object, Object>> mapEntrySet() {
		return map.entrySet();
	}
	public Set<Object> mapKeySet() {
		return map.keySet();
	}

	public void add(Object value) {
		checkReadonly();
		vec.add(value);
		migrate();
	}

	public void addAll(Iterable<?> iterable) {
		Iterables.addAll(vec, iterable);
	}

	private void checkReadonly() {
		if (readonly)
			throw new SuException("can't modify readonly objects");
	}

	private void migrate() {
		Object x;
		// FIXME: concurrency issue -- this should all be wrapped in synchronized...
		while (null != (x = map.remove(vec.size())))
			vec.add(x);
	}

	public void insert(int at, Object value) {
		checkReadonly();
		// FIXME: concurrency issue -- modification between size check and get??
		if (0 <= at && at <= vec.size()) {
			vec.add(at, value);
			migrate();
		} else
			put(at, value);
	}

	public void merge(SuContainer c) {
		vec.addAll(c.vec);
		map.putAll(c.map);
		migrate();
	}

	@Override
	public void put(Object key, Object value) {
		preset(key, value);
	}

	public void preset(Object key, Object value) {
		checkReadonly();
		int i = index(key);
		// FIXME: concurrency issue -- modification between size check and get??
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			add(value);
		else
			map.put(key, value);
	}

	/** used by CallRule, bypasses readonly */
	protected void putMap(Object key, Object value) {
		map.put(key, value);
	}

	@Override
	public Object get(Object key) {
		if (key instanceof Range)
			return ((Range) key).sublist(this);
		else
			return getDefault(key, defval);
	}

	/**
	 * Get method which attempts to avoid boxing of the key if the key is an
	 * {@code int} value.
	 * @param at Integer key
	 * @return Result of the get operation
	 * @author Victor Schappert
	 * @since 20130717
	 * @see #getDefault(int, Object)
	 * @see #getIfPresent(int)
	 */
	public Object get(int at) {
		return getDefault(at, defval);
	}

	public Object getDefault(Object key, Object defval) {
		Object x = getIfPresent(key);
		if (x != null)
			return x;
		if (defval instanceof SuContainer) {
			x = new SuContainer((SuContainer) defval);
			if (! readonly)
				put(key, x);
			return x;
		}
		return defval;
	}

	/**
	 * Get-with-default method which attempts to avoid boxing of the key if
	 * the key is an {@code int} value.
	 * @param at Integer key
	 * @param defval Default value to insert if {@code at} not present
	 * @return Result of the get operation
	 * @author Victor Schappert
	 * @since 20130717
	 * @see #get(int)
	 * @see #getIfPresent(int)
	 */
	public Object getDefault(int at, Object defval) {
		Object x = getIfPresent(at);
		if (x != null)
			return x;
		if (defval instanceof SuContainer) {
			x = new SuContainer((SuContainer) defval);
			if (! readonly)
				insert(at, x);
			return x;
		}
		return defval;
	}

	public Object getIfPresent(Object key) {
		// FIXME: concurrency issue -- modification between size check and get??
		int i = index(key);
		return (0 <= i && i < vec.size()) ? vec.get(i) : map.get(key);
	}

	/**
	 * Get-if-present method which attempts to avoid boxing of the key if the
	 * key is an {@code int} value.
	 * @param at Integer key
	 * @return Result of the get operation
	 * @author Victor Schappert
	 * @since 20130717
	 * @see #get(int)
	 * @see #getDefault(int, Object)
	 */
	public Object getIfPresent(int at) {
		synchronized (vec) {
			if (0 <= at && at < vec.size())
				return vec.get(at);
		}
		return map.isEmpty() ? null : map.get(at);
	}

	public boolean containsKey(Object key) {
		// FIXME: concurrency issue -- modification between size check and get??
		int i = index(key);
		return (0 <= i && i < vec.size()) || map.containsKey(key);
	}

	public int size() {
		return vec.size() + map.size();
	}

	@Override
	public String toString() {
		return toString("#(", ")");
	}

	protected String toString(String before, String after) {
		// FIXME: Concurrency issue when iterating over synchronized list ...
		//        should wrap in synchronized block...
		StringBuilder sb = new StringBuilder(before);
		for (Object x : vec)
			sb.append(Ops.display(x)).append(", ");
		for (Map.Entry<Object, Object> e : map.entrySet())
			sb.append(keyToString(e.getKey()) + ": " + Ops.display(e.getValue()))
					.append(", ");
		if (size() > 0)
			sb.delete(sb.length() - 2, sb.length());
		return sb.append(after).toString();
	}
	static String keyToString(Object x) {
		return Ops.isString(x) ? keyToString(x.toString()) : Ops.display(x);
	}
	private static final Pattern idpat;
	static { idpat = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*[?!]?$"); }
	static String keyToString(String s) {
		return idpat.matcher(s).matches() ? s : Ops.display(s);
	}

	@Override
	public synchronized int hashCode() {
		int h = hashCodeContrib();
		if (! vec.isEmpty()) {
			// The nice thing about vectors: they have a canonical ordering, so
			// we know we can satisfy the hashCode() contract by just looking at
			// an arbitrary number of elements.
			h = 31 * h + Ops.hashCodeContrib(vec.get(0));
		} else if (map.size() <= 5) {
			// The nasty thing about maps: no canonical ordering. If we want to
			// look at any of the members, we have to look at all of them.
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				h = 31 * h + Ops.hashCodeContrib(entry.getKey())
						^ Ops.hashCodeContrib(entry.getValue());
			}
		}
		return h;
	}

	@Override
	public int hashCodeContrib() {
		return 31 * 31 * vec.size() + 31 * map.size()
				+ SuContainer.class.hashCode();
	}

	/**
	 * convert to standardized types so lookup works consistently
	 * Number is converted to Integer if within range, else BigDecimal
	 * CharSequence (String, Concat, SuException) is converted to String
	 */
	private static Object canonical(Object x) {
		if (x instanceof Number) {
			if (x instanceof Integer)
				return x;
			if (x instanceof Byte || x instanceof Short)
				return ((Number) x).intValue();
			if (x instanceof Long) {
				long i = (Long) x;
				if (Integer.MIN_VALUE <= i && i <= Integer.MIN_VALUE)
					return (int) i;
			}
			return canonicalBD(toBigDecimal(x));
		}
		if (x instanceof CharSequence)
			return x.toString();
		return x;
	}
	private static Number canonicalBD(BigDecimal n) {
		try {
			return n.intValueExact();
		} catch (ArithmeticException e) {
			return n.stripTrailingZeros();
		}
	}

	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		SuContainer c = Ops.toContainer(value);
		if (c == null)
			return false;
		return equals2(this, c, new PairStack());
	}

	// avoid infinite recursion from self-reference
	private static boolean equals2(SuContainer x, SuContainer y, PairStack stack) {
		if (x.vec.size() != y.vec.size() || x.map.size() != y.map.size())
			return false;
		if (stack.contains(x, y))
			return true; // comparison is already in progress
		stack.push(x, y);
		try {
			for (int i = 0; i < x.vec.size(); ++i)
				if (! equals3(x.vec.get(i), y.vec.get(i), stack))
					return false;
			for (Map.Entry<Object, Object> e : x.map.entrySet())
				if (! equals3(e.getValue(), y.map.get(e.getKey()), stack))
					return false;
			return true;
		} finally {
			stack.pop();
		}
	}

	// public since also called by SuInstance.equals2
	public static boolean equals3(Object x, Object y, PairStack stack) {
		if (x == y)
			return true;
		if (x instanceof SuInstance && y instanceof SuInstance)
			return SuInstance.equals2((SuInstance) x, (SuInstance) y, stack);
		SuContainer cx = Ops.toContainer(x);
		if (cx == null)
			return Ops.is_(x, y);
		SuContainer cy = Ops.toContainer(y);
		return (cy == null) ? false : equals2(cx, cy, stack);
	}

	@Override
	public int compareTo(SuContainer that) {
		if (this == that)
			return 0;
		return compare2(that, new PairStack());
	}

	private int compare2(SuContainer that, PairStack stack) {
		if (stack.contains(this, that))
			return 0; // comparison is already in progress
		stack.push(this, that);
		int ord;
		for (int i = 0; i < vec.size() && i < that.vec.size(); ++i)
			if (0 != (ord = compare3(vec.get(i), that.vec.get(i), stack)))
				return ord;
		return vec.size() - that.vec.size();
	}

	private static int compare3(Object x, Object y, PairStack stack) {
		if (x == y)
			return 0;
		SuContainer cx = Ops.toContainer(x);
		if (cx == null)
			return Ops.cmp(x, y);
		SuContainer cy = Ops.toContainer(y);
		return (cy == null) ? Ops.cmp(x, y) : cx.compare2(cy, stack);
	}

	public boolean delete(Object key) {
		checkReadonly();
		if (null != map.remove(key))
			return true;
		int i = index(key);
		if (0 <= i && i < vec.size()) {
			vec.remove(i);
			return true;
		} else
			return false;
	}

	public boolean erase(Object key) {
		checkReadonly();
		if (null != map.remove(key))
			return true;
		int i = index(key);
		if (i < 0 || vec.size() <= i)
			return false;
		// migrate from vec to map
		for (int j = vec.size() - 1; j > i; --j) {
			map.put(j, vec.get(j));
			vec.remove(j);
		}
		vec.remove(i);
		return true;
	}

	public void clear() {
		checkReadonly();
		vec.clear();
		map.clear();
	}

	private static int index(Object x) {
		x = canonical(x);
		return x instanceof Integer ? (Integer) x : -1;
	}

	public int vecSize() {
		return vec.size();
	}
	public int mapSize() {
		return map.size();
	}

	@Override
	public int packSize(int nest) {
		checkNest(++nest);
		int ps = 1;
		if (size() == 0)
			return ps;

		ps += 4; // vec size
		for (Object x : vec)
			ps += 4 /* value size */+ Pack.packSize(x, nest);

		ps += 4; // map size
		for (Map.Entry<Object, Object> e : map.entrySet())
			ps += 4 /* member size */ + Pack.packSize(e.getKey(), nest)
					+ 4 /* value size */ + Pack.packSize(e.getValue(), nest);

		return ps;
	}

	static final int NESTING_LIMIT = 20;

	private static void checkNest(int nest) {
		if (nest > NESTING_LIMIT)
			throw new SuException("pack: object nesting limit ("
					+ NESTING_LIMIT + ") exceeded");
	}

	@Override
	public void pack(ByteBuffer buf) {
		pack(buf, Pack.Tag.OBJECT);
	}

	protected void pack(ByteBuffer buf, byte tag) {
		buf.put(tag);
		if (size() == 0)
			return;
		buf.putInt(vec.size() ^ 0x80000000);
		for (Object x : vec)
			packvalue(buf, x);

		buf.putInt(map.size() ^ 0x80000000);
		for (Map.Entry<Object, Object> e : map.entrySet()) {
			packvalue(buf, e.getKey()); // member
			packvalue(buf, e.getValue()); // value
		}
	}

	private static void packvalue(ByteBuffer buf, Object x) {
		buf.putInt(Pack.packSize(x) ^ 0x80000000);
		Pack.pack(x, buf);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuContainer());
	}

	public static Object unpack(ByteBuffer buf, SuContainer c) {
		if (buf.remaining() == 0)
			return c;
		int n = buf.getInt() ^ 0x80000000; // vec size
		for (int i = 0; i < n; ++i)
			c.vec.add(unpackvalue(buf));
		n = buf.getInt() ^ 0x80000000; // map size
		for (int i = 0; i < n; ++i) {
			Object key = unpackvalue(buf);
			Object val = unpackvalue(buf);
			c.map.put(key, val);
		}
		verify(buf.remaining() == 0);
		return c;
	}

	private static Object unpackvalue(ByteBuffer buf) {
		int n = buf.getInt() ^ 0x80000000;
		ByteBuffer buf2 = buf.slice();
		buf2.limit(n);
		buf.position(buf.position() + n);
		return Pack.unpack(buf2);
	}

	public void setReadonly() {
		if (readonly)
			return;
		readonly = true;
		// recurse
		for (Object x : vec)
			if (x instanceof SuContainer)
				((SuContainer) x).setReadonly();
		for (Object x : map.values())
			if (x instanceof SuContainer)
				((SuContainer) x).setReadonly();
	}

	public boolean getReadonly() {
		return readonly;
	}

	public Object slice(int i) {
		SuContainer c = new SuContainer();
		c.vec.addAll(vec.subList(i, vec.size()));
		c.map.putAll(map);
		return c;
	}

	public static enum IterWhich { LIST, NAMED, ALL };

	@Override
	public Iterator<Object> iterator() {
		return iterator(IterWhich.ALL, IterResult.VALUE);
	}

	@SuppressWarnings("unchecked")
	public Iterator<Object> iterator(IterWhich iterWhich, IterResult iterResult) {
		return new Iter(
				iterWhich == IterWhich.NAMED ? nullIter : vec.iterator(),
				iterWhich == IterWhich.LIST ? nullIter : map.entrySet().iterator(),
				iterResult);
	}

	public Iterable<Object> iterable(IterWhich iterWhich, IterResult iterResult) {
		if (iterWhich == IterWhich.ALL && iterResult == IterResult.VALUE)
			return this;
		else
			return new IterableAdapter(iterWhich, iterResult);
	}

	private class IterableAdapter implements Iterable<Object> {
		private final IterWhich iterWhich;
		private final IterResult iterResult;

		public IterableAdapter(IterWhich iterWhich, IterResult iterResult) {
			this.iterWhich = iterWhich;
			this.iterResult = iterResult;
		}

		@Override
		public Iterator<Object> iterator() {
			return SuContainer.this.iterator(iterWhich, iterResult);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private static final NullIterator nullIter = new NullIterator();

	public static enum IterResult {
		KEY, VALUE, ASSOC, ENTRY
	};

	static class Iter implements Iterator<Object> {
		private final Iterator<Object> veciter;
		private int vec_i = 0;
		private final Iterator<Map.Entry<Object, Object>> mapiter;
		private final IterResult iterResult;

		public Iter(Iterator<Object> veciter,
				Iterator<Map.Entry<Object, Object>> mapiter, IterResult iterResult) {
			this.veciter = veciter;
			this.mapiter = mapiter;
			this.iterResult = iterResult;
		}
		@Override
		public boolean hasNext() {
			return veciter.hasNext() || mapiter.hasNext();
		}
		@Override
		public Object next() {
			if (veciter.hasNext())
				return result(vec_i++, veciter.next());
			else if (mapiter.hasNext()) {
				Map.Entry<Object, Object> e = mapiter.next();
				if (iterResult == IterResult.ENTRY)
					return e;
				return result(e.getKey(), e.getValue());
			} else
				throw new NoSuchElementException();
		}
		private Object result(Object key, Object value) {
			switch (iterResult) {
			case KEY:
				return key;
			case VALUE:
			case ENTRY:
				return value;
			case ASSOC:
				return SuContainer.of(key, value);
			default:
				throw SuInternalError.unreachable();
			}
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Object find(Object value) {
		// FIXME: Concurrency issue -- modification between size() and get()
		for (int i = 0; i < vec.size(); ++i)
			if (Ops.is_(value, vec.get(i)))
				return i;
		// FIXME: Concurrency issue -- iteration generally
		for (Map.Entry<Object, Object> e : map.entrySet())
			if (Ops.is_(value, e.getValue()))
				return e.getKey();
		return null;
	}

	public void reverse() {
		checkReadonly();
		// TODO: Possible concurrency issue -- is Collections.reverse() thread-safe?
		Collections.reverse(vec);
	}

	public void sort(final Object fn) {
		checkReadonly();
		// TODO: Possible concurrency issue -- is Collections.sort() thread-safe?
		if (fn == Boolean.FALSE)
			Collections.sort(vec, Ops.comp);
		else
			Collections.sort(vec, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1
							: Ops.call(fn, y, x) == Boolean.TRUE ? 1 : 0);
	}

	public int lowerBound(Object value, final Object fn) {
		if (fn == Boolean.FALSE)
			return Util.lowerBound(vec, value, Ops.comp);
		else
			return Util.lowerBound(vec, value, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1 : 1);
	}

	public int upperBound(Object value, final Object fn) {
		if (fn == Boolean.FALSE)
			return Util.upperBound(vec, value, Ops.comp);
		else
			return Util.upperBound(vec, value, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1 : 1);
	}

	public Util.Range equalRange(Object value, final Object fn) {
		if (fn == Boolean.FALSE)
			return Util.equalRange(vec, value, Ops.comp);
		else
			return Util.equalRange(vec, value, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1 : 1);
	}

	public Record toDbRecord(Header hdr) {
		RecordBuilder rec = dbpkg.recordBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		for (String f : hdr.output_fldsyms())
			if (f == "-")
				rec.addMin();
			else if (f.equals(ts))
				rec.add(TheDbms.dbms().timestamp());
			else if (null != (x = get(f)))
				rec.add(x);
			else
				rec.addMin();
		return rec.build();
	}

	public void setDefault(Object value) {
		defval = value;
	}

	@Override
	public SuContainer toContainer() {
		return this;
	}

	@Override
	public String typeName() {
		return "Object";
	}

	public boolean isEmpty() {
		return vec.isEmpty() && map.isEmpty();
	}

	@Override
	public SuValue lookup(String method) {
		return ContainerMethods.lookup(method);
	}

	public SuContainer subList(int from, int to) {
		return new SuContainer(vec.subList(from, to));
	}

}
