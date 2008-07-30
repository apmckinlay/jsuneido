package suneido.database.query;

import static suneido.Suneido.verify;
import static suneido.Util.list;
import static suneido.Util.listToParens;
import static suneido.Util.prefix;
import static suneido.database.Database.theDB;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import suneido.SuException;
import suneido.database.Record;

public class TempIndex1 extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private boolean first = true;
	private boolean rewound = true;
	private Header hdr = null;
	private TreeMap<Record,Long> map = null;
	private Map.Entry<Record, Long> cur;
	private Record selFrom = Record.MINREC;
	private Record selTo = Record.MAXREC;

	public TempIndex1(Query source, List<String> order, boolean unique) {
		super(source);
		this.order = order;
		this.unique = unique;
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEX1" + listToParens(order)
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
		return new Row(null, theDB.input(cur.getValue()));
	}

	private boolean outsideSelect(Record key) {
		return key.compareTo(selFrom) < 0 || key.compareTo(selTo) > 0;
	}

	private void iterate_setup(Dir dir) {
		Header srchdr = source.header();
		map = new TreeMap<Record,Long>();
		Row row;
		for (int num = 0; null != (row = source.get(Dir.NEXT)); ++num)
			{
			Record key = unique
				? row_to_key(srchdr, row, order)
				: row_to_key(srchdr, row, order, num);
			// WARNING: assumes data is always second in row
			verify(null == map.put(key, row.data[1].off()));
			}
	}

	private Record row_to_key(Header hdr, Row row, List<String> flds, int num) {
		Record key = row_to_key(hdr, row, flds);
		key.add(num);
		return key;
	}

	private Record row_to_key(Header hdr, Row row, List<String> flds) {
		Record key = new Record();
		for (String f : flds)
			key.add(row.getrawval(hdr, f));
		if (key.bufSize() > 4000)
			throw new SuException("index entry size > 4000: " + flds);
		return key;
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
