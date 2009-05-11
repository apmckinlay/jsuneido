package suneido;

import static suneido.Suneido.verify;
import static suneido.language.Ops.canonical;
import static suneido.language.Ops.cmp;

import java.nio.ByteBuffer;
import java.util.*;

import suneido.language.Ops;
import suneido.language.Pack;
import suneido.language.builtin.ContainerMethods;

/**
 * Suneido's single container type.
 * Combines an extendable array plus a hash map.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuContainer extends SuValue
		implements Comparable<SuContainer>, Iterable<Object> {
	private final ArrayList<Object> vec = new ArrayList<Object>();
	private final CanonicalMap map = new CanonicalMap();
	private final Object defval = null; // TODO defval

	@SuppressWarnings("serial")
	static class CanonicalMap extends HashMap<Object, Object> {
		@Override
		public Object get(Object key) {
			return super.get(canonical(key));
		}
		@Override
		public Object put(Object key, Object value) {
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

	public SuContainer() {
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

	public void append(Object value) {
		vec.add(value);
		migrate();
	}

	private void migrate() {
		Object x;
		while (null != (x = map.remove(vec.size())))
			vec.add(x);
	}

	public void insert(int at, Object value) {
		vec.add(at, value);
		migrate();
	}

	public void merge(SuContainer c) {
		vec.addAll(c.vec);
		map.putAll(c.map);
	}

	@Override
	public void put(Object key, Object value) {
		int i = index(key);
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			append(value);
		else
			map.put(key, value);
	}

	@Override
	public Object get(Object key) {
		int i = index(key);
		if (0 <= i && i < vec.size())
			return vec.get(i);
		Object x = map.get(key);
		return x == null ? defval : x;
	}

	public boolean containsKey(Object key) {
		int i = index(key);
		return 0 <= i && i < vec.size() ? true : map.containsKey(key);
	}

	public int size() {
		return vec.size() + map.size();
	}

	@Override
	public String toString() {
		return toString("#(", ")");
	}

	protected String toString(String before, String after) {
		String s = "";
		for (Object x : vec)
			s += Ops.display(x) + ", ";
		for (Map.Entry<Object, Object> e : map.entrySet())
			s += keyToString(e.getKey()) + ": " + Ops.display(e.getValue()) + ", ";
		if (s.length() >= 2)
			s = s.substring(0, s.length() - 2);
		return before + s + after;
	}
	static String keyToString(Object x) {
		return x instanceof String ? keyToString((String) x) : Ops.toStr(x);
	}
	static String keyToString(String s) {
		return s.matches("^[_a-zA-Z][_a-zA-Z0-9]*[?!]?$") ? s : ("'" + s + "'");
	}

	@Override
	public int hashCode() {
		return hashCode(0);
	}
	/** as recommended by Effective Java
	 *  can't use vec and map hashCode methods
	 *  because we need to check nesting */
	@Override
	public int hashCode(int nest) {
		checkNest(++nest);
		int result = 17;
		for (Object x : vec)
			result = 31 * result + Ops.hashCode(x, nest);
		for (Map.Entry<Object, Object> e : map.entrySet()) {
			result = 31 * result + Ops.hashCode(e.getKey(), nest);
			result = 31 * result + Ops.hashCode(e.getValue(), nest);
		}
		return result;
	}

	@Override
	public boolean equals(Object value) {
		if (value == this)
			return true;
		if (! (value instanceof SuContainer))
			return false;
		SuContainer c = (SuContainer) value;
		return vec.equals(c.vec) && map.equals(c.map);
		//TODO handle stack overflow from self-reference
	}

	public int compareTo(SuContainer other) {
		int ord;
		for (int i = 0; i < vec.size() && i < other.vec.size(); ++i)
			if (0 != (ord = cmp(vec.get(i), other.vec.get(i))))
				return ord;
		return vec.size() - other.vec.size();
		//TODO handle stack overflow from self-reference
	}

	public boolean erase(Object key) {
		int i = index(key);
		if (0 <= i && i < vec.size()) {
			vec.remove(i);
			return true;
		} else
			return null != map.remove(key);
	}

	public static int index(Object x) {
		x = canonical(x);
		return x instanceof Integer ? (Integer) x : -1;
	}

	public int vecSize() {
		return vec.size();
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

	final static int NESTING_LIMIT = 20;

	private void checkNest(int nest) {
		if (nest > NESTING_LIMIT)
			throw new SuException("pack: object nesting limit ("
					+ NESTING_LIMIT + ") exceeded");
	}

	@Override
	public void pack(ByteBuffer buf) {
		buf.put(Pack.Tag.OBJECT);
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

	private void packvalue(ByteBuffer buf, Object x) {
		buf.putInt(Pack.packSize(x) ^ 0x80000000);
		Pack.pack(x, buf);
	}

	public static Object unpack1(ByteBuffer buf) {
		SuContainer c = new SuContainer();
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
		// TODO setReadonly
	}

	public Object slice(int i) {
		SuContainer c = new SuContainer();
		c.vec.addAll(vec.subList(i, vec.size()));
		c.map.putAll(map);
		return c;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		assert this == self;
		return ContainerMethods.invoke(this, method, args);
	}

	public Iterator<Object> iterator() {
		return new Iter(vec.iterator(), map.entrySet().iterator());
	}

	static class Iter implements Iterator<Object> {
		private final Iterator<Object> veciter;
		private final Iterator<Map.Entry<Object, Object>> mapiter;
		public Iter(Iterator<Object> veciter,
				Iterator<Map.Entry<Object, Object>> mapiter) {
			this.veciter = veciter;
			this.mapiter = mapiter;
		}
		public boolean hasNext() {
			return veciter.hasNext() || mapiter.hasNext();
		}
		public Object next() {
			if (veciter.hasNext())
				return veciter.next();
			else if (mapiter.hasNext())
				return mapiter.next().getValue();
			else
				throw new NoSuchElementException();
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public void sort() {
		Collections.sort(vec, new Comparator<Object>() {
			public int compare(Object x, Object y) { return Ops.cmp(x, y); } });
	}

}
