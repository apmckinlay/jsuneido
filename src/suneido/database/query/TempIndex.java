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
	private Header hdr = null;
	private TreeMap<Record, Object[]> map = null;
	private Map.Entry<Record, Object[]> cur;
	private Record selFrom = Record.MINREC;
	private Record selTo = Record.MAXREC;

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

	@Override
	List<List<String>> indexes() {
		return list(order);
	}

	@Override
	Row get(Dir dir) {
		if (first) {
			first = false;
			hdr = header();
			iterate_setup(dir);
		}
		if (rewound) {
			rewound = false;
			if (dir == Dir.NEXT)
				cur = map.ceilingEntry(selFrom);
			else
				cur = map.floorEntry(selTo);
		}
		else if (dir == Dir.NEXT)
			cur = map.higherEntry(cur.getKey());
		else // dir == PREV
			cur = map.lowerEntry(cur.getKey());
		if (cur == null || outsideSelect(cur.getKey()))
			{
			rewound = true;
			return null;
			}

		// TODO: put iter->key into row
		return Row.fromRefs(cur.getValue());
	}

	private boolean outsideSelect(Record key) {
		return key.compareTo(selFrom) < 0 || key.compareTo(selTo) > 0;
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
	void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(prefix(order, index));
		selFrom = from;
		selTo = to;
		rewound = true;
	}

}
