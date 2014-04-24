/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static java.util.Collections.disjoint;
import static suneido.util.Util.difference;
import static suneido.util.Util.intersect;
import static suneido.util.Util.nil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import suneido.SuException;
import suneido.intfc.database.Record;

import com.google.common.collect.ImmutableSet;

public class Rename extends Query1 {
	List<String> from;
	List<String> to;

	Rename(Query source, List<String> from, List<String> to) {
		super(source);
		this.from = from;
		this.to = to;
		List<String> src = source.columns();
		if (!src.containsAll(from))
			throw new SuException("rename: nonexistent column(s): "
					+ difference(from, src));
		if (!disjoint(src, to))
			throw new SuException("rename: column(s) already exist: "
					+ intersect(src, to));

		renameDependencies(src);
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
			List<String> from2 = new ArrayList<>();
			List<String> to2 = new ArrayList<>();
			for (int i = 0; i < from.size(); ++i)
				if (!r.to.contains(from.get(i))) {
					from2.add(from.get(i));
					to2.add(to.get(i));
				}
			to = new ArrayList<>(rename_fields(r.to, from, to));
			to.addAll(to2);
			from = new ArrayList<>(r.from);
			from.addAll(from2);
			source = r.source;
			return transform();
		}
		source = source.transform();
		return this;
	}

	private static List<String> rename_fields(List<String> f,
			List<String> from, List<String> to) {
		List<String> new_fields = new ArrayList<>(f);
		for (int i = 0; i < f.size(); ++i) {
			int j = from.indexOf(f.get(i));
			if (j != -1)
				new_fields.set(i, to.get(j));
		}
		return new_fields;
	}

	private static Set<String> rename_fields(Set<String> f,
			List<String> from, List<String> to) {
		ImmutableSet.Builder<String> new_fields = ImmutableSet.builder();
		for (String s : f) {
			int j = from.indexOf(s);
			new_fields.add(j == -1 ? s : to.get(j));
		}
		return new_fields.build();
	}

	@Override
	double optimize2(List<String> index, Set<String> needs,
			Set<String> firstneeds, boolean is_cursor, boolean freeze) {
		// NOTE: optimize1 to bypass tempindex
		return source.optimize1(rename_fields(index, to, from), rename_fields(
				needs, to, from), rename_fields(firstneeds, to, from),
				is_cursor, freeze);
	}

	@Override
	List<String> columns() {
		return rename_fields(source.columns(), from, to);
	}

	@Override
	List<List<String>> indexes() {
		return rename_indexes(source.indexes(), from, to);
	}
	private static List<List<String>> rename_indexes(List<List<String>> i,
			List<String> from, List<String> to) {
		List<List<String>> new_idxs = new ArrayList<>(i.size());
		for (List<String> j : i)
			new_idxs.add(rename_fields(j, from, to));
		return new_idxs;
	}

	@Override
	public List<List<String>> keys() {
		return rename_indexes(source.keys(), from, to);
	}

	// iteration
	@Override
	public Header header() {
		return source.header().rename(from, to);
	}

	@Override
	void select(List<String> index, Record f, Record t) {
		source.select(rename_fields(index, to, from), f, t);
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
