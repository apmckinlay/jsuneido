package suneido.database.query;

import static suneido.SuException.unreachable;
import static suneido.Suneido.verify;
import static suneido.Util.*;

import java.util.ArrayList;
import java.util.List;

import suneido.SuException;
import suneido.database.Record;

public class Join extends Query2 {
	List<String> joincols;
	protected Type type;

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
		if (by != null && !set_eq(by, joincols))
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
		double SELECT_COST = 1000;

		// always have to read all of source 1
		double cost1 = src1
				.optimize(index, needs1, joincols, is_cursor, freeze);

		double nrecs1 = src1.nrecords();
		double nrecs2 = src2.nrecords();

		// for each of source 1, select on source2
		cost1 += nrecs1 * SELECT_COST;

		// cost of reading all of source 2
		double cost2 = src2.optimize(joincols, needs2, noFields, is_cursor,
				freeze);

		if (src2.willneed_tempindex)
			return cost1 + cost2;

		double nrecs;
		switch (typ) {
		case ONE_ONE:
			nrecs = Math.min(nrecs1, nrecs2);
			break;
		case N_ONE:
			nrecs = nrecs1;
			break;
		case ONE_N:
			nrecs = nrecs2;
			break;
		case N_N:
			nrecs = nrecs1 * nrecs2;
			break;
		default:
			throw unreachable();
		}
		// guestimate 0...nrecs => nrecs / 2
		cost2 *= nrecs <= 0 || nrecs2 <= 0 ? 0 : (nrecs / 2) / nrecs2;

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
	List<List<String>> keys() {
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
		switch (type) {
		case ONE_ONE:
			return Math.min(source.nrecords(), source2.nrecords()) / 2;
		case ONE_N:
			return source.nrecords() / 2;
		case N_ONE:
			return source2.nrecords() / 2;
		case N_N:
			return (source.nrecords() * source2.nrecords()) / 2;
		default:
			throw unreachable();
		}
	}

	@Override
	int recordsize() {
		// TODO shouldn't be total, maybe count common fields
		return source.recordsize() + source2.recordsize();
	}

	@Override
	List<Fixed> fixed() {
		// TODO I think this is wrong - should be intersect?
		return union(source.fixed(), source2.fixed());
	}

	@Override
	Header header() {
		return Header.add(source.header(), source2.header());
	}

	@Override
	Row get(Dir dir) {
		// TODO get
		return null;
	}

	@Override
	void rewind() {
		// TODO rewind
	}

	@Override
	void select(List<String> index, Record from, Record to) {
		// TODO select
	}

}
