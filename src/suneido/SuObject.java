/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.runtime.Numbers.intOrMin;
import static suneido.util.ByteBuffers.getUVarint;
import static suneido.util.ByteBuffers.putUVarint;
import static suneido.util.ByteBuffers.varintSize;
import static suneido.util.Verify.verify;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.query.Header;
import suneido.runtime.*;
import suneido.runtime.builtin.ObjectMethods;
import suneido.util.Dnum;
import suneido.util.NullIterator;
import suneido.util.PairStack;
import suneido.util.Util;

//TODO detect the same modification-during-iteration as cSuneido (see ObjectsTest)

/**
 * Suneido's primary container type.
 * Combines an extendible array plus a hash map.
 */
public class SuObject extends SuValue
		implements Comparable<SuObject>, Iterable<Object>, Showable {
	public final List<Object> vec;
	private final Map<Object,Object> map;
	protected Object defval = null;
	private boolean readonly = false;
	public final static SuObject EMPTY = empty();

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

	public SuObject(int vecCapacity) {
		vec = new ArrayList<>(vecCapacity);
		map = new CanonicalMap();
	}

	public SuObject() {
		this(10);
	}

	/** create a new container and add the specified values */
	public SuObject(Iterable<?> c) {
		this(10);
		addAll(c);
	}

	public SuObject(SuObject other) {
		this(other.vecSize());
		vec.addAll(other.vec);
		map.putAll(other.map);
		defval = other.defval;
	}

	private SuObject(List<Object> vec) {
		this(vec, new CanonicalMap());
	}

	private SuObject(List<Object> vec, Map<Object,Object> map) {
		this.vec = vec;
		this.map = map;
	}

	/** only used to initialize EMPTY */
	private static SuObject empty() {
		SuObject c =
				new SuObject(Collections.emptyList(), Collections.emptyMap());
		c.readonly = true;
		return c;
	}

	public static SuObject of(Object ...values) {
		return new SuObject(Lists.newArrayList(values));
	}

	public synchronized Object vecGet(int i) {
		return vec.get(i);
	}
	/** WARNING: does not handle migration */
	public synchronized Object vecSet(int i, Object value) {
		return vec.set(i, value);
	}
	public synchronized Object mapGet(Object key) {
		return map.get(key);
	}
	public synchronized Set<Map.Entry<Object, Object>> mapEntrySet() {
		return map.entrySet();
	}
	public synchronized Set<Object> mapKeySet() {
		return map.keySet();
	}

	public synchronized void add(Object value) {
		checkReadonly();
		vec.add(value);
		migrate();
	}

	public synchronized void addAll(Iterable<?> iterable) {
		Iterables.addAll(vec, iterable);
	}

	private void checkReadonly() {
		if (readonly)
			throw new SuException("can't modify readonly objects");
	}

	private void migrate() {
		Object x;
		while (null != (x = map.remove(vec.size())))
			vec.add(x);
	}

	public synchronized void insert(int at, Object value) {
		checkReadonly();
		if (0 <= at && at <= vec.size()) {
			vec.add(at, value);
			migrate();
		} else
			put(at, value);
	}

	public synchronized void merge(SuObject c) {
		vec.addAll(c.vec);
		map.putAll(c.map);
		migrate();
	}

	@Override
	public synchronized void put(Object key, Object value) {
		preset(key, value);
	}

	public synchronized void preset(Object key, Object value) {
		checkReadonly();
		int i = intOrMin(key);
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			add(value);
		else
			map.put(key, value);
	}

	/** used by CallRule, bypasses readonly */
	public void putMap(Object key, Object value) {
		map.put(key, value);
	}

	@Override
	public synchronized Object get(Object key) {
		return getDefault(key, defval);
	}

	/**
	 * Get method to avoid boxing int keys
	 * @see #getDefault(int, Object)
	 * @see #getIfPresent(int)
	 */
	public synchronized Object get(int at) {
		return getDefault(at, defval);
	}

	private synchronized Object getDefault(Object key, Object defval) {
		Object x = getIfPresent(key);
		if (x != null)
			return x;
		return defaultValue(key, defval);
	}

	protected Object defaultValue(Object key, Object defval) {
		if (defval instanceof SuObject) {
			Object x = ((SuObject) defval).dup();
			if (! readonly)
				put(key, x);
			return x;
		}
		return defval;
	}

	protected SuObject dup() {
		return new SuObject(this);
	}

	/**
	 * Get-with-default method to avoid boxing int keys
	 * @see #get(int)
	 * @see #getIfPresent(int)
	 */
	public synchronized Object getDefault(int at, Object defval) {
		Object x = getIfPresent(at);
		if (x != null)
			return x;
		if (defval instanceof SuObject) {
			x = new SuObject((SuObject) defval);
			if (! readonly)
				insert(at, x);
			return x;
		}
		return defval;
	}

	public synchronized Object getIfPresent(Object key) {
		int i = intOrMin(key);
		return (0 <= i && i < vec.size()) ? vec.get(i) : map.get(key);
	}

	/**
	 * Get-if-present method to avoid boxing int keys
	 * @see #get(int)
	 * @see #getDefault(int, Object)
	 */
	public synchronized Object getIfPresent(int at) {
		synchronized (vec) {
			if (0 <= at && at < vec.size())
				return vec.get(at);
		}
		return map.isEmpty() ? null : map.get(at);
	}

	@Override
	public synchronized Object rangeTo(int i, int j) {
		int size = vec.size();
		int f = Range.prepFrom(i, size);
		int t = Range.prepTo(f, j, size);
		return subList(f, t);
	}

	@Override
	public synchronized Object rangeLen(int i, int n) {
		int size = vec.size();
		int f = Range.prepFrom(i, size);
		int t = f + Range.prepLen(n, size - f);
		return subList(f, t);
	}

	public synchronized boolean containsKey(Object key) {
		int i = intOrMin(key);
		return (0 <= i && i < vec.size()) || map.containsKey(key);
	}

	public synchronized int size() {
		return vec.size() + map.size();
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		rString(sb, new InProgress());
		return sb.toString();
	}

	public void rString(StringBuilder sb, InProgress inProgress) {
		toString(sb, "#(", ")", inProgress);
	}

	protected void toString(StringBuilder sb, String before, String after,
			InProgress inProgress) {
		if (!inProgress.push(this)) {
			sb.append("...");
			return;
		}
		inProgress.push(this);
		sb.append(before);
		for (Object x : vec) {
			display(sb, x, inProgress);
			sb.append(", ");
		}
		for (Map.Entry<Object, Object> e : map.entrySet()) {
			sb.append(keyToString(e.getKey())).append(":");
			if (e.getValue() != Boolean.TRUE) {
				sb.append(" ");
				display(sb, e.getValue(), inProgress);
			}
			sb.append(", ");
		}
		if (size() > 0)
			sb.delete(sb.length() - 2, sb.length());
		sb.append(after);
		inProgress.pop();
	}
	static String keyToString(Object x) {
		return Ops.isString(x) ? keyToString(x.toString()) : Ops.display(x);
	}
	private static final Pattern idpat;
	static { idpat = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*[?!]?$"); }
	static String keyToString(String s) {
		return idpat.matcher(s).matches() && !s.equals("true") && !s.equals("false")
				? s : Ops.display(s);
	}

	private static void display(StringBuilder sb, Object x, InProgress inProgress) {
		if (x instanceof SuObject)
			((SuObject) x).rString(sb, inProgress);
		else
			sb.append(Ops.display(x));
	}

	protected static class InProgress {
		private List<SuObject> list =  new ArrayList<>();

		boolean push(SuObject x) {
			for (var y : list) {
				if (y == x)
					return false;
			}
			list.add(x);
			return true;
		}

		void pop() {
			list.remove(list.size() - 1);
		}
	}

	@Override
	public String show() {
		return show("#(", ")");
	}

	protected String show(String before, String after) {
		StringBuilder sb = new StringBuilder(before);
		var sep = "";
		for (Object x : vec) {
			sb.append(sep).append(Ops.display(x));
			sep = ", ";
		}
		var keys = new ArrayList<>(map.keySet());
		Collections.sort(keys, Ops::cmp);
		for (var key : keys) {
			sb.append(sep);
			sep = ", ";
			sb.append(keyToString(key)).append(":");
			var val = map.get(key);
			if (val != Boolean.TRUE)
				sb.append(" ").append(Showable.show(val));
		}
		return sb.append(after).toString();
	}

	@Override
	public synchronized int hashCode() {
		int h = hashCodeContrib();
		// The nice thing about vectors: they have a canonical ordering, so
		// we know we can satisfy the hashCode() contract by just looking at
		// an arbitrary number of elements.
		if (vec.size() > 0) {
			h = 31 * h + Ops.hashCodeContrib(vec.get(0));
			if (vec.size() > 1)
				h = 31 * h + Ops.hashCodeContrib(vec.get(1));
		}
		if (map.size() <= 5) {
			// The nasty thing about hash maps: no canonical ordering.
			// If we look at any members, we have to look at all of them.
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				h = 31 * h + Ops.hashCodeContrib(entry.getKey())
						^ Ops.hashCodeContrib(entry.getValue());
			}
		}
		return h;
	}

	@Override
	public synchronized int hashCodeContrib() {
		return 31 * 31 * vec.size() + 31 * map.size()
				+ SuObject.class.hashCode();
	}

	/**
	 * Convert to standardized types so lookup works consistently
	 * Dnum is narrowed to Integer if in range
	 * CharSequence (String, Concat, SuException) is converted to String
	 */
	static Object canonical(Object x) {
		if (x instanceof CharSequence)
			return x.toString();
		if (x instanceof Integer)
			return x;
		if (x instanceof Dnum) {
			Object y = ((Dnum) x).intObject();
			if (y != null)
				return y;
		}
		return x;
	}

	@Override
	public synchronized boolean equals(Object value) {
		if (value == this)
			return true;
		return equals2(this, value, null);
	}

	// avoid infinite recursion from self-reference
	private static boolean equals2(SuObject x, Object value, PairStack stack) {
		SuObject y = Ops.toObject(value);
		if (y == null)
			return false;
		if (x.vec.size() != y.vec.size() || x.map.size() != y.map.size())
			return false;
		if (stack == null)
			stack = new PairStack();
		else if (stack.contains(x, y))
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
	public synchronized static boolean equals3(Object x, Object y, PairStack stack) {
		if (x == y)
			return true;
		if (x instanceof SuInstance && y instanceof SuInstance)
			return SuInstance.equals2((SuInstance) x, (SuInstance) y, stack);
		SuObject cx = Ops.toObject(x);
		return (cx == null) ? Ops.is_(x, y) : equals2(cx, y, stack);
	}

	@Override
	public synchronized int compareTo(SuObject that) {
		if (this == that)
			return 0;
		return compare2(that, new PairStack());
	}

	private int compare2(SuObject that, PairStack stack) {
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
		SuObject cx = Ops.toObject(x);
		if (cx == null)
			return Ops.cmp(x, y);
		SuObject cy = Ops.toObject(y);
		return (cy == null) ? Ops.cmp(x, y) : cx.compare2(cy, stack);
	}

	public synchronized boolean delete(Object key) {
		checkReadonly();
		if (null != map.remove(key))
			return true;
		int i = intOrMin(key);
		if (0 <= i && i < vec.size()) {
			vec.remove(i);
			return true;
		} else
			return false;
	}

	public synchronized boolean erase(Object key) {
		checkReadonly();
		if (null != map.remove(key))
			return true;
		int i = intOrMin(key);
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

	public synchronized void deleteAll() {
		checkReadonly();
		vec.clear();
		map.clear();
	}

	public synchronized Object popFirst() {
		checkReadonly();
		return vec.isEmpty() ? null : vec.remove(0);
	}

	public synchronized Object popLast() {
		checkReadonly();
		return vec.isEmpty() ? null : vec.remove(vec.size() - 1);
	}

	public synchronized int vecSize() {
		return vec.size();
	}
	public synchronized int mapSize() {
		return map.size();
	}

	@Override
	public synchronized int packSize(int nest) {
		checkNest(++nest);
		int ps = 1;
		if (size() == 0)
			return ps;

		ps += varintSize(vec.size()); // vec size
		for (Object x : vec)
			ps += packSizeValue(x, nest);

		ps += varintSize(map.size());
		for (Map.Entry<Object, Object> e : map.entrySet())
			ps += packSizeValue(e.getKey(), nest) +
					packSizeValue(e.getValue(), nest);

		return ps;
	}

	private static int packSizeValue(Object x, int nest) {
		var n = Pack.packSize(x, nest);
		return varintSize(n) + n;
	}

	static final int NESTING_LIMIT = 20;

	private static void checkNest(int nest) {
		if (nest > NESTING_LIMIT)
			throw new SuException("pack: object nesting limit ("
					+ NESTING_LIMIT + ") exceeded");
	}

	@Override
	public synchronized void pack(ByteBuffer buf) {
		pack(buf, Pack.Tag.OBJECT);
	}

	protected void pack(ByteBuffer buf, byte tag) {
		buf.put(tag);
		if (size() == 0)
			return;
		putUVarint(buf, vec.size());
		for (Object x : vec)
			packvalue(buf, x);

		putUVarint(buf, map.size());
		for (Map.Entry<Object, Object> e : map.entrySet()) {
			packvalue(buf, e.getKey()); // member
			packvalue(buf, e.getValue()); // value
		}
	}

	private static void packvalue(ByteBuffer buf, Object x) {
		putUVarint(buf, Pack.packSize(x));
		Pack.pack(x, buf);
	}

	public static Object unpack(ByteBuffer buf) {
		return unpack(buf, new SuObject());
	}

	public static Object unpack(ByteBuffer buf, SuObject c) {
		if (buf.remaining() == 0)
			return c;
		int n = (int) getUVarint(buf); // vec size
		for (int i = 0; i < n; ++i)
			c.vec.add(unpackvalue(buf));
		n = (int) getUVarint(buf); // map size
		for (int i = 0; i < n; ++i) {
			Object key = unpackvalue(buf);
			Object val = unpackvalue(buf);
			c.map.put(key, val);
		}
		verify(buf.remaining() == 0);
		return c;
	}

	private static Object unpackvalue(ByteBuffer buf) {
		int n = (int) getUVarint(buf);
		ByteBuffer buf2 = buf.slice();
		buf2.limit(n);
		buf.position(buf.position() + n);
		return Pack.unpack(buf2);
	}

	public synchronized SuObject setReadonly() {
		if (readonly)
			return this;
		readonly = true;
		// recurse
		for (Object x : vec)
			if (x instanceof SuObject)
				((SuObject) x).setReadonly();
		for (Object x : map.values())
			if (x instanceof SuObject)
				((SuObject) x).setReadonly();
		return this;
	}

	public synchronized boolean getReadonly() {
		return readonly;
	}

	public synchronized Object slice(int i) {
		SuObject c = new SuObject();
		c.vec.addAll(vec.subList(i, vec.size()));
		c.map.putAll(map);
		return c;
	}

	public enum IterWhich { LIST, NAMED, ALL }

	@Override
	public synchronized Iterator<Object> iterator() {
		return iterator(IterWhich.ALL, IterResult.VALUE);
	}

	@SuppressWarnings("unchecked")
	public synchronized Iterator<Object> iterator(IterWhich iterWhich, IterResult iterResult) {
		return new Iter(
				iterWhich == IterWhich.NAMED ? nullIter : vec.iterator(),
				iterWhich == IterWhich.LIST ? nullIter : map.entrySet().iterator(),
				iterResult);
	}

	public synchronized Iterable<Object> iterable(IterWhich iterWhich, IterResult iterResult) {
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
			return SuObject.this.iterator(iterWhich, iterResult);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private static final NullIterator nullIter = new NullIterator();

	public enum IterResult {
		KEY, VALUE, ASSOC, ENTRY
	}

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
				return SuObject.of(key, value);
			default:
				throw SuInternalError.unreachable();
			}
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public synchronized Object find(Object value) {
		for (int i = 0; i < vec.size(); ++i)
			if (Ops.is_(value, vec.get(i)))
				return i;
		for (Map.Entry<Object, Object> e : map.entrySet())
			if (Ops.is_(value, e.getValue()))
				return e.getKey();
		return null;
	}

	public synchronized void reverse() {
		checkReadonly();
		Collections.reverse(vec);
	}

	public synchronized void sort(final Object fn) {
		checkReadonly();
		if (fn == Boolean.FALSE)
			Collections.sort(vec, Ops.comp);
		else
			Collections.sort(vec, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1
							: Ops.call(fn, y, x) == Boolean.TRUE ? 1 : 0);
	}

	public synchronized void unique() {
		int dst = 1;
		for (int src = 1; src < vec.size(); ++src) {
			if (Ops.is_(vec.get(src), vec.get(src - 1)))
				continue;
			if (dst < src)
				vec.set(dst, vec.get(src));
			++dst;
		}
		while (vec.size() > dst)
			vec.remove(vec.size() - 1);
	}

	public synchronized int binarySearch(Object value, final Object fn) {
		if (fn == Boolean.FALSE)
			return Util.lowerBound(vec, value, Ops.comp);
		else
			return Util.lowerBound(vec, value, (Object x, Object y) ->
					Ops.call(fn, x, y) == Boolean.TRUE ? -1 : 1);
	}

	public synchronized Record toDbRecord(Header hdr) {
		RecordBuilder rec = new RecordBuilder();
		Object x;
		String ts = hdr.timestamp_field();
		Object tsval = null;
		for (String f : hdr.output_fldsyms())
			if (f == "-")
				rec.addMin();
			else if (f.equals(ts))
				rec.add(tsval = TheDbms.dbms().timestamp());
			else if (null != (x = get(f)))
				rec.add(x);
			else
				rec.addMin();
		if (tsval != null && ! getReadonly())
			put(ts, tsval);
		return rec.build();
	}

	public synchronized void setDefault(Object value) {
		checkReadonly();
		defval = value;
	}

	@Override
	public SuObject toObject() {
		return this;
	}

	@Override
	public String typeName() {
		return "Object";
	}

	public synchronized boolean isEmpty() {
		return vec.isEmpty() && map.isEmpty();
	}

	@Override
	public SuValue lookup(String method) {
		return ObjectMethods.lookup(method);
	}

	public synchronized SuObject subList(int from, int to) {
		return new SuObject(new ArrayList<>(vec.subList(from, to)));
	}

}
