package suneido.database;

import static suneido.util.Util.commaSplitter;

import java.util.*;

import net.jcip.annotations.Immutable;
import suneido.SuException;

import com.google.common.collect.ImmutableList;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@Immutable
public class Columns implements Iterable<Column> {

	private final ImmutableList<Column> columns;

	public Columns(ImmutableList<Column> columns) {
		this.columns = columns;
	}

	public ImmutableList<Integer> nums(String s) {
		if (s.isEmpty())
			return ImmutableList.of();
		Iterable<String> names = commaSplitter.split(s);
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (String name : names)
			builder.add(ck_find(name).num);
		return builder.build();
	}

	private Column ck_find(String name) {
		Column c = find(name);
		if (c == null)
			throw new SuException("column not found: " + name);
		return c;
	}

	public Column find(String name) {
		for (Column c : columns)
			if (name.equals(c.name))
				return c;
		return null;
	}

	public boolean hasColumn(String name) {
		return find(name) != null;
	}

	public int size() {
		return columns.size();
	}

	public List<String> names() {
		ArrayList<String> list = new ArrayList<String>();
		for (Column c : columns)
			list.add(c.name);
		return list;
	}

	public Iterator<Column> iterator() {
		return columns.iterator();
	}
}
