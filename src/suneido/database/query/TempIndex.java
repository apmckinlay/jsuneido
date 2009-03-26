package suneido.database.query;

import static suneido.Suneido.verify;
import static suneido.Util.*;

import java.util.*;

import suneido.database.Record;

public class TempIndex extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private boolean first = true;
	private boolean rewound = true;
	private TreeMap<Record, Object[]> map = null;
	private Map.Entry<Record, Object[]> cur;
	private final Keyrange sel = new Keyrange();

	public TempIndex(Query source, List<String> order, boolean unique) {
		super(source);
		this.order = order;
		this.unique = unique;
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEX" + listToParens(order)
				+ (unique ? " unique" : "");
	}

	@SuppressWarnings("unchecked")
	@Override
	List<List<String>> indexes() {
		return list(order);
	}

	@Override
	public Row get(Dir dir) {
		if (first) {
			first = false;
			iterate_setup(dir);
		}
		if (rewound) {
			rewound = false;
			if (dir == Dir.NEXT)
				cur = map.ceilingEntry(sel.org);
			else
				cur = map.floorEntry(sel.end);
		}
		else if (dir == Dir.NEXT)
			cur = map.higherEntry(cur.getKey());
		else // dir == PREV
			cur = map.lowerEntry(cur.getKey());
		if (cur == null || !sel.contains(cur.getKey()))
			{
			rewound = true;
			return null;
			}

		// TODO: put iter->key into row
		return Row.fromRefs(cur.getValue());
	}

	private void iterate_setup(Dir dir) {
		Header srchdr = source.header();
		map = new TreeMap<Record, Object[]>();
		Row row;
		for (int num = 0; null != (row = source.get(Dir.NEXT)); ++num)
			{
			Record key = row.project(srchdr, order);
			if (!unique)
				key.add(num);
			verify(null == map.put(key, row.getRefs()));
			}
	}

	@Override
	public void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(prefix(order, index));
		sel.set(from, to);
		rewound = true;
	}

}
