package suneido;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import suneido.database.Record;
import suneido.database.query.Header;

/**
 * Suneido's single container type.
 * Combines an extendable array plus a hash map.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class SuContainer extends SuValue {
	ArrayList<SuValue> vec = new ArrayList<SuValue>();
	HashMap<SuValue, SuValue> map = new HashMap<SuValue,SuValue>();

	public SuContainer() {
	}
	public SuContainer(SuContainer c) {
		merge(c);
	}

	public void append(SuValue value) {
		vec.add(value);
		// check for migration from map to vec
		while (true) {
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

	public void putdata(String key, SuValue value) {
		putdata(new SuString(key), value);
	}

	@Override
	public void putdata(SuValue key, SuValue value) {
		int i = key.index();
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			append(value);
		else
			map.put(key, value);
	}

	@Override
	public SuValue getdata(SuValue key) {
		int i = key.index();
		return 0 <= i && i < vec.size()
			? vec.get(i)
			: map.get(key);
	}

	public int size() {
		return vec.size() + map.size();
	}

	@Override
	public String toString() {
		String s = "";
		for (SuValue x : vec)
			s += x + ", ";
		// use a TreeSet to get the members sorted
		TreeSet<SuValue> keys = new TreeSet<SuValue>(map.keySet());
		for (SuValue k : keys)
			s += k.string() + ": " + map.get(k) + ", ";
		if (s.length() >= 2)
			s = s.substring(0, s.length() - 2);
		return "[" + s + "]";
	}

	@Override
	public int hashCode() {
		int hash = size();
		for (SuValue x : vec)
			hash += x.hashCode();
		for (Map.Entry<SuValue,SuValue> e : map.entrySet())
			hash += e.getKey().hashCode() + e.getValue().hashCode();
		return hash;
		//TODO handle stack overflow from self-reference
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
	public int vecsize() {
		return vec.size();
	}

	// TODO prevent infinite recursion

	@Override
	public int packSize() {
		int ps = 1;
		if (size() == 0)
			return ps;

		ps += 4; // vec size
		for (SuValue x : vec)
			ps += 4 /* value size */+ x.packSize();

		ps += 4; // map size
		for (Map.Entry<SuValue, SuValue> e : map.entrySet())
			ps += 4 /* member size */+ e.getKey().packSize() + 4 /* value size */
					+ e.getValue().packSize();

		return ps;
	}

	@Override
	public void pack(ByteBuffer buf) {
		buf.put(Pack.OBJECT);
		if (size() == 0)
			return;
		buf.putInt(vec.size());
		for (SuValue x : vec)
			packvalue(buf, x);

		buf.putInt(map.size());
		for (Map.Entry<SuValue, SuValue> e : map.entrySet()) {
			packvalue(buf, e.getKey()); // member
			packvalue(buf, e.getValue()); // value
		}
	}

	private void packvalue(ByteBuffer buf, SuValue x) {
		buf.putInt(x.packSize());
		x.pack(buf);
	}

	public static SuValue unpack1(ByteBuffer buf) {
		SuContainer c = new SuContainer();
		if (buf.remaining() == 0)
			return c;
		int n = buf.getInt(); // vec size
		for (int i = 0; i < n; ++i)
			c.vec.add(unpackvalue(buf));
		n = buf.getInt(); // map size
		for (int i = 0; i < n; ++i) {
			SuValue key = unpackvalue(buf);
			SuValue val = unpackvalue(buf);
			c.map.put(key, val);
		}
		return c;
	}

	private static SuValue unpackvalue(ByteBuffer buf) {
		int n = buf.getInt();
		ByteBuffer buf2 = buf.slice();
		buf2.limit(n);
		buf.position(buf.position() + n);
		return SuValue.unpack(buf2);
	}

	public Record toDbRecord(Header hdr) {
		int[] fldsyms = hdr.output_fldsyms();
		// dependencies
		// - access all the fields to ensure dependencies are created
//		Lisp<int> f;
//		for (f = fldsyms; ! nil(f); ++f)
//			if (*f != -1)
//				getdata(symbol(*f));
		// - invert stored dependencies
//		typedef HashMap<ushort, Lisp<ushort> > Deps;
//		Deps deps;
//		for (HashMap<ushort,Lisp<ushort> >::iterator it = dependents.begin();
//			it != dependents.end(); ++it)
//			{
//			for (Lisp<ushort> m = it->val; ! nil(m); ++m)
//				{
//				ushort d = depsname(*m);
//				if (fldsyms.member(d))
//					deps[d].push(it->key);
//				}
//			}

		Record rec = new Record();
//		OstreamStr oss;
		SuValue x;
		// int ts = hdr.timestamp_field();
		for (int f : fldsyms)
			if (f == -1)
				rec.addMin();
//			else if (f == ts)
//				rec.addval(dbms()->timestamp());
//			else if (Lisp<ushort>* pd = deps.find(*f))
//				{
//				// output dependencies
//				oss.clear();
//				for (Lisp<ushort> d = *pd; ! nil(d); )
//					{
//					oss << symstr(*d);
//					if (! nil(++d))
//						oss << ",";
//					}
//				rec.addval(oss.str());
//				}
			else if (null != (x = getdata(Symbols.symbol(f))))
				rec.add(x);
			else
				rec.addMin();
		return rec;
	}
}
