package suneido;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SuContainer extends SuValue {
	private ArrayList<SuValue> vec = new ArrayList<SuValue>();
	private HashMap<SuValue, SuValue> map = new HashMap<SuValue,SuValue>();
	
	public void append(SuValue value) {
		vec.add(value);
		// check for migration from map to vec
		while (true) {
			SuValue num = new SuInteger(vec.size());
			if (! map.containsKey(num))
				break ;
			vec.add(map.get(num));
			map.remove(num);
		}
	}
	
	@Override
	public void putdata(SuValue key, SuValue value) {
		int i = key.is_numeric() ? key.integer() : -1;
		if (0 <= i && i < vec.size())
			vec.set(i, value);
		else if (i == vec.size())
			append(value);
		else
			map.put(key, value);
	}
	
	@Override
	public SuValue getdata(SuValue key) {
		if (key.is_numeric()) {
			int i = key.integer();
			if (0 <= i && i < vec.size())
				return vec.get(i);
		}
		return map.get(key);
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
			s += e.getKey() + ": " + e.getValue() + ", ";
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
//	@Override
//	public int compareTo(SuValue value) {
//		int ord = order() - value.order();
//		return ord < 0 ? -1 : ord > 0 ? +1 :
//			 vec.compareTo(((SuContainer) value).vec);
//	}
	@Override
	public int order() {
		return Order.CONTAINER.ordinal();
	}
	
	public boolean erase(SuValue key) {
		int i = key.is_numeric() ? key.integer() : -1;
		if (0 <= i && i < vec.size()) {
			vec.remove(i);
			return true;
		} else
			return null != map.remove(key);
	}
}
