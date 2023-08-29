/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static suneido.util.Util.nil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import suneido.SuException;
import suneido.database.immudb.Record;

public class Rename extends Query1 {
	List<String> from;
	List<String> to;

	Rename(Query source, List<String> from, List<String> to) {
		super(source);
		this.from = from;
		this.to = to;
		List<String> srcCols = source.columns();
		checkRename(srcCols, from, to);
		renameDependencies(srcCols);
	}

	private static void checkRename(List<String> srcCols,
			List<String> from, List<String> to) {
		var cols = new ArrayList<>(srcCols);
		for (int i = 0; i < from.size(); ++i) {
			var f = from.get(i);
			var j = cols.indexOf(f);
			if (j == -1)
				throw new SuException("rename: nonexistent column: " + f);
			var t = to.get(i);
			if (cols.contains(t))
				throw new SuException("rename: column already exists: " + t);
			cols.set(j,  t);
		}
	}

	private void renameDependencies(List<String> src) {
		boolean copy = false;
		for (int i = 0; i < from.size(); ++i) {
			String deps = from.get(i) + "_deps";
			if (src.contains(deps)) {
				if (!copy) {
					from = new ArrayList<>(from);
					to = new ArrayList<>(to);
					copy = true;
				}
				from.add(deps);
				to.add(to.get(i) + "_deps");
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(source.toString());
		sb.append(" RENAME ");
		for (int i = 0; i < from.size(); ++i)
			sb.append(from.get(i)).append(" to ").append(to.get(i)).append(", ");
		return sb.substring(0, sb.length() - 2);
	}

	@Override
	Query transform() {
		// remove empty Renames
		if (nil(from))
			return source.transform();
		// combine Renames
		if (source instanceof Rename) {
			Rename r = (Rename) source;
			var from2 = new ArrayList<>(r.from);
			from2.addAll(from);
			from = from2;
			var to2 = new ArrayList<>(r.to);
			to2.addAll(to);
			to = to2;
			source = r.source;
			return transform();
		}
		source = source.transform();
		return this;
	}

	private List<String> renameFwd(List<String> f) {
		List<String> new_fields = new ArrayList<>(f);
		for (int i = 0; i < from.size(); ++i) {
			int j = new_fields.indexOf(from.get(i));
			if (j != -1)
				new_fields.set(j, to.get(i));
		}
		return new_fields;
	}

	private List<String> renameRev(List<String> f) {
		List<String> new_fields = new ArrayList<>(f);
		for (int i = to.size() - 1; i >= 0; --i) {
			int j = new_fields.indexOf(to.get(i));
			if (j != -1)
				new_fields.set(j, from.get(i));
		}
		return new_fields;
	}

	private Set<String> renameSetRev(Set<String> f) {
		var fields = new ArrayList<>(f);
		return ImmutableSet.copyOf(renameRev(fields));
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		// NOTE: optimize1 to bypass tempindex
		return source.optimize1(renameRev(index), renameSetRev(needs),
				renameSetRev(firstneeds), is_cursor, freeze);
	}

	@Override
	List<String> columns() {
		return renameFwd(source.columns());
	}

	@Override
	List<List<String>> indexes() {
		return renameIndexes(source.indexes());
	}
	private List<List<String>> renameIndexes(List<List<String>> i) {
		List<List<String>> new_idxs = new ArrayList<>(i.size());
		for (List<String> j : i)
			new_idxs.add(renameFwd(j));
		return new_idxs;
	}

	@Override
	public List<List<String>> keys() {
		return renameIndexes(source.keys());
	}

	@Override
	public List<Fixed> fixed() {
		var result = new ArrayList<>(source.fixed());
		for (int i = 0; i < from.size(); ++i) {
			var fld = from.get(i);
			for (int j = 0; j < result.size(); ++j) {
				var f = result.get(j);
				if (f.field.equals(fld)) {
					result.set(j, new Fixed(to.get(i), f.values));
					break;
				}
			}
		}
		return result;
	}

	// iteration
	@Override
	public Header header() {
		var hdr = source.header();
		var flds = renameIndexes(hdr.flds);
		var cols = renameFwd(hdr.cols);
		return new Header(flds, cols);
		}

	@Override
	void select(List<String> index, Record f, Record t) {
		source.select(renameRev(index), f, t);
	}

	@Override
	public void rewind() {
		source.rewind();
	}

	@Override
	public Row get(Dir dir) {
		return source.get(dir);
	}

	@Override
	public void output(Record r) {
		source.output(r);
	}

}
