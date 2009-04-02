package suneido;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Suneido's single container type.
 * Combines an extendable array plus a hash map.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class SuContainer extends SuValue {
	final private ArrayList<SuValue> vec = new ArrayList<SuValue>();
	final private HashMap<SuValue, SuValue> map = new HashMap<SuValue, SuValue>();
	private final SuValue defval = null; // TODO defval

	public SuContainer() {
	}

	public SuValue vecGet(int i) {
		return vec.get(i);
	}
	public SuValue mapGet(SuValue key) {
		return map.get(key);
	}

	public void append(SuValue value) {
		vec.add(value);
		// check for migration from map to vec
		while (!map.isEmpty()) {
			SuValue num = SuInteger.valueOf(vec.size());
			if (! map.containsKey(num))
				break ;
			vec.add(map.get(num));
			map.remove(num);
		}
	}

	public void merge(SuContainer c) {
		vec.addAll(c.vec);
		map.putAll(c.map);
	}

	@Override
	public void put(SuValue key, SuValue value) {
		int i = key.index();
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			append(value);
		else
			map.put(key, value);
	}

	@Override
	public SuValue get(SuValue key) {
		int i = key.index();
		if (0 <= i && i < vec.size())
			return vec.get(i);
		SuValue x = map.get(key);
		return x == null ? defval : x;
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
		for (SuValue x : vec)
			s += x + ", ";
		// use a TreeSet to get the members sorted
		TreeSet<SuValue> keys = new TreeSet<SuValue>(map.keySet());
		for (SuValue k : keys)
			s += k.string() + ": " + map.get(k) + ", ";
		if (s.length() >= 2)
			s = s.substring(0, s.length() - 2);
		return before + s + after;
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
		for (SuValue x : vec)
			result = 31 * result + x.hashCode(nest);
		for (Map.Entry<SuValue,SuValue> e : map.entrySet()) {
			result = 31 * result + e.getKey().hashCode(nest);
			result = 31 * result + e.getValue().hashCode(nest);
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

	@Override
	public int compareTo(SuValue value) {
		if (value == this)
			return 0;
		int ord = order() - value.order();
		if (ord != 0)
			return ord < 0 ? -1 : +1;
		SuContainer other = (SuContainer) value;
		for (int i = 0; i < vec.size() && i < other.vec.size(); ++i)
			if (0 != (ord = vec.get(i).compareTo(other.vec.get(i))))
				return ord;
		return vec.size() - other.vec.size();
		//TODO handle stack overflow from self-reference
	}
	@Override
	public int order() {
		return Order.CONTAINER.ordinal();
	}

	public boolean erase(SuValue key) {
		int i = key.index();
		if (0 <= i && i < vec.size()) {
			vec.remove(i);
			return true;
		} else
			return null != map.remove(key);
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
		for (SuValue x : vec)
			ps += 4 /* value size */+ x.packSize(nest);

		ps += 4; // map size
		for (Map.Entry<SuValue, SuValue> e : map.entrySet())
			ps += 4 /* member size */ + e.getKey().packSize(nest)
					+ 4 /* value size */ + e.getValue().packSize(nest);

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
		buf.put(Pack.OBJECT);
		if (size() == 0)
			return;
		buf.putInt(vec.size() ^ 0x80000000);
		for (SuValue x : vec)
			packvalue(buf, x);

		buf.putInt(map.size() ^ 0x80000000);
		for (Map.Entry<SuValue, SuValue> e : map.entrySet()) {
			packvalue(buf, e.getKey()); // member
			packvalue(buf, e.getValue()); // value
		}
	}

	private void packvalue(ByteBuffer buf, SuValue x) {
		buf.putInt(x.packSize() ^ 0x80000000);
		x.pack(buf);
	}

	public static SuValue unpack1(ByteBuffer buf) {
		SuContainer c = new SuContainer();
		if (buf.remaining() == 0)
			return c;
		int n = buf.getInt() ^ 0x80000000; // vec size
		for (int i = 0; i < n; ++i)
			c.vec.add(unpackvalue(buf));
		n = buf.getInt() ^ 0x80000000; // map size
		for (int i = 0; i < n; ++i) {
			SuValue key = unpackvalue(buf);
			SuValue val = unpackvalue(buf);
			c.map.put(key, val);
		}
		verify(buf.remaining() == 0);
		return c;
	}

	private static SuValue unpackvalue(ByteBuffer buf) {
		int n = buf.getInt() ^ 0x80000000;
		ByteBuffer buf2 = buf.slice();
		buf2.limit(n);
		buf.position(buf.position() + n);
		return SuValue.unpack(buf2);
	}

	public void setReadonly() {
		// TODO setReadonly
	}

	public SuValue slice(int i) {
		SuContainer c = new SuContainer();
		c.vec.addAll(vec.subList(i, vec.size()));
		c.map.putAll(map);
		return c;
	}

	@Override
	public SuContainer container() {
		return this;
	}

}
