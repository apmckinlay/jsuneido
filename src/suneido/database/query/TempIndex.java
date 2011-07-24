package suneido.database.query;

import static java.util.Arrays.asList;
import static suneido.SuException.verify;
import static suneido.Suneido.dbpkg;
import static suneido.util.Util.listToParens;
import static suneido.util.Util.startsWith;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import suneido.SuException;
import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class TempIndex extends Query1 {
	private final List<String> order;
	private final boolean unique;
	private Transaction tran;
	private boolean first = true;
	private boolean rewound = true;
	private TreeMap<Record, Object[]> map = null;
	private Map.Entry<Record, Object[]> cur;
	private final Keyrange sel = new Keyrange();

	public TempIndex(Query source, Transaction tran, List<String> order, boolean unique) {
		super(source);
		this.tran = tran;
		this.order = order;
		this.unique = unique;
	}

	@Override
	public void setTransaction(Transaction tran) {
		this.tran = tran;
		super.setTransaction(tran);
	}

	@Override
	public String toString() {
		return source.toString() + " TEMPINDEX" + listToParens(order)
				+ (unique ? " unique" : "");
	}

	@SuppressWarnings("unchecked")
	@Override
	List<List<String>> indexes() {
		return asList(order);
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

		// TODO put iter->key into row
		return Row.fromRefs(tran, cur.getValue());
	}

	private void iterate_setup(Dir dir) {
		Header srchdr = source.header();
		map = new TreeMap<Record, Object[]>();
		Row row;
		for (int num = 0; null != (row = source.get(Dir.NEXT)); ++num)
			{
			Record key = row.project(srchdr, order);
			if (key.bufSize() > 4000)
				throw new SuException("index entry size > 4000: " + order);
			if (! unique)
				key = dbpkg.recordBuilder().addAll(key).add(num).build();
			verify(null == map.put(key, row.getRefs()));
			}
	}

	@Override
	public void rewind() {
		rewound = true;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		verify(startsWith(order, index));
		sel.set(from, to);
		rewound = true;
	}

}
