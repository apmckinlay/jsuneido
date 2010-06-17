package suneido.database.query;

import static suneido.SuException.unreachable;
import static suneido.SuException.verify;
import static suneido.util.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Join extends Query2 {
	List<String> joincols;
	protected Type type;
	Header hdr1;
	Row row1;
	Row row2;
	short[] cols1;
	short[] cols2;
	Row empty2;
	protected double nrecs = -1;

	enum Type {
		NONE(""), ONE_ONE("1:1"), ONE_N("1:n"), N_ONE("n:1"), N_N("n:n");
		public String name;
		Type(String name) {
			this.name = name;
		}
	}

	Join(Query source1, Query source2, List<String> by) {
		super(source1, source2);
		joincols = intersect(source.columns(), source2.columns());
		if (joincols.isEmpty())
			throw new SuException("join: common columns required");
		if (by != null && !setEquals(by, joincols))
			throw new SuException("join: by does not match common columns: "
					+ joincols);

		// find out if joincols include keys
		boolean k1 = containsKey(source.keys());
		boolean k2 = containsKey(source2.keys());
		if (k1 && k2)
			type = Type.ONE_ONE;
		else if (k1)
			type = Type.ONE_N;
		else if (k2)
			type = Type.N_ONE;
		else
			type = Type.N_N;
	}

	private boolean containsKey(List<List<String>> keys) {
		for (List<String> k : keys)
			if (joincols.containsAll(k))
				return true;
		return false;
	}

	@Override
	public String toString() {
		return "(" + source + " " + name() + " " + type.name + " on "
				+ listToParens(joincols)
				+ " " + source2 + ")";
	}

	protected String name() {
		return "JOIN";
	}

	@Override
	double optimize2(List<String> index, List<String> needs,
			List<String> firstneeds, boolean is_cursor, boolean freeze) {
		List<String> needs1 = intersect(source.columns(), needs);
		List<String> needs2 = intersect(source2.columns(), needs);
		verify(union(needs1, needs2).size() == needs.size());

		double cost1 = opt(source, source2, type, index, needs1, needs2,
				is_cursor, false);
		double cost2 = can_swap()
			? opt(source2, source, reverse(type), index,
				needs2, needs1, is_cursor, false)
				+ OUT_OF_ORDER
			: IMPOSSIBLE;
		double cost = Math.min(cost1, cost2);
		if (cost >= IMPOSSIBLE)
			return IMPOSSIBLE;
		if (freeze) {
			if (cost2 < cost1) {
				Query t1 = source;
				source = source2;
				source2 = t1;
				List<String> t2 = needs1;
				needs1 = needs2;
				needs2 = t2;
				type = reverse(type);
			}
			opt(source, source2, type, index, needs1, needs2, is_cursor, true);
		}
		return cost;
	}

	private double opt(Query src1, Query src2, Type typ, List<String> index,
			List<String> needs1, List<String> needs2, boolean is_cursor,
			boolean freeze) {
		// guestimated from: (3 treelevels * 4096) / 10 ~ 1000
		final double SELECT_COST = 1000;

		// always have to read all of source 1
		double cost1 = src1.optimize(index, needs1, joincols, is_cursor, freeze);
		if (cost1 >= IMPOSSIBLE)
			return IMPOSSIBLE;
		double nrecs1 = src1.nrecords();

		// for each of source 1, select on source2
		cost1 += nrecs1 * SELECT_COST;

		// cost of reading all of source 2
		double cost2 =
				src2.optimize(joincols, needs2, noFields, is_cursor, false);
		if (cost2 >= IMPOSSIBLE)
			return IMPOSSIBLE;
		double nrecs2 = src2.nrecords();

		boolean is_cursor2 = is_cursor;
		if (type == Type.N_ONE && nrecs1 >= 0 && nrecs2 > 0) {
			double p = nrecs1 / nrecs2;
			if (!is_cursor && p < .2) {
				// "1" side can be no bigger than "n" side
				// if "1" side is a lot bigger, then pass is_cursor = true to avoid temp or filter indexes
				double cost2b =
						src2.optimize(joincols, needs2, noFields, true, false);
				if (cost2b < IMPOSSIBLE) {
					is_cursor2 = true;
					cost2 = cost2b;
				}
			}
		}
		if (freeze)
			src2.optimize(joincols, needs2, noFields, is_cursor2, true);

		switch (typ) {
		case ONE_ONE:
			nrecs = Math.min(nrecs1, nrecs2);
			break;
		case N_ONE:
			nrecs = nrecs2 <= 0 ? 0 : nrecs1;
			break;
		case ONE_N:
			nrecs = nrecs1 <= 0 ? 0 : nrecs2;
			break;
		case N_N:
			nrecs = nrecs1 * nrecs2;
			break;
		default:
			throw unreachable();
		}
		nrecs /= 2; // convert from max to guess of expected

		if (nrecs <= 0)
			cost2 = 0;
		else if (is_cursor2 || !src2.tempindexed())
			cost2 = nrecs * (cost2 / nrecs2);

		return cost1 + cost2;
	}

	private static Type reverse(Type type) {
		return type == Type.ONE_N ? Type.N_ONE
				: type == Type.N_ONE ? Type.ONE_N : type;
	}

	protected boolean can_swap() {
		return true;
	}

	@Override
	List<String> columns() {
		return union(source.columns(), source2.columns());
	}

	@Override
	List<List<String>> indexes() {
		switch (type) {
		case ONE_ONE:
			return union(source.indexes(), source2.indexes());
		case ONE_N:
			return source2.indexes();
		case N_ONE:
			return source.indexes();
		case N_N:
			// union of indexes that don't include joincols
			List<List<String>> idxs = new ArrayList<List<String>>();
			for (List<String> i : source.indexes())
				if (nil(intersect(i, joincols)))
					idxs.add(i);
			for (List<String> i : source2.indexes())
				if (nil(intersect(i, joincols)))
					addUnique(idxs, i);
			return idxs;
		default:
			throw unreachable();
		}
	}

	@Override
	public List<List<String>> keys() {
		switch (type) {
		case ONE_ONE :
			return union(source.keys(), source2.keys());
		case ONE_N :
			return source2.keys();
		case N_ONE :
			return source.keys();
		case N_N :
			return keypairs();
		default :
			throw unreachable();
		}
	}

	protected List<List<String>> keypairs() {
		List<List<String>> keys = new ArrayList<List<String>>();
		for (List<String> k1 : source.keys())
			for (List<String> k2 : source2.keys())
				addUnique(keys, union(k1, k2));
		verify(!nil(keys));
		return keys;
	}

	@Override
	double nrecords() {
		verify(nrecs >= 0);
		return nrecs;
	}

	@Override
	int recordsize() {
		return source.recordsize() + source2.recordsize();
	}

	@Override
	List<Fixed> fixed() {
		return union(source.fixed(), source2.fixed());
	}

	@Override
	public Row get(Dir dir) {
		if (hdr1 == null) {
			hdr1 = source.header();
			empty2 = new Row(source2.header().size());
		}
		while (true) {
			if (row2 == null && !next_row1(dir))
				return null;
			row2 = source2.get(dir);
			if (should_output(row2)) {
				if (row2 != null)
					assert (row1.project(hdr1, joincols).equals(
							row2.project(source2.header(), joincols)));
				return new Row(row1, row2 == null ? empty2 : row2);
			}
		}
	}

	protected boolean next_row1(Dir dir) {
		if (null == (row1 = source.get(dir)))
			return false;
		Record key = row1.project(hdr1, joincols);
		source2.select(joincols, key);
		return true;
	}

	protected boolean should_output(Row row) {
		return row != null;
	}

	@Override
	public void rewind() {
		source.rewind();
		row2 = null;
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		source.select(index, from, to);
		row2 = null;
	}

}
