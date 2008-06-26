package suneido;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
		for (Map.Entry<SuValue,SuValue> e : map.entrySet())
			s += e.getKey().string() + ": " + e.getValue() + ", ";
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
	@Override
	public void pack(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int packSize() {
		// TODO Auto-generated method stub
		return 0;
	}
}
