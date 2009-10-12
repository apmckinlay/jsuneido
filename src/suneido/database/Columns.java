package suneido.database;

import static suneido.util.Util.commaSplitter;

import java.util.*;

import suneido.SuException;

import com.google.common.collect.Iterables;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Columns implements Iterable<Column> {
	private final ArrayList<Column> columns = new ArrayList<Column>();

	public void add(Column column) {
		columns.add(column);
	}

	public void sort() {
		columns.trimToSize();
		Collections.sort(columns);
	}

	public short[] nums(String s) {
		if (s.isEmpty())
			return new short[0];
		Iterable<String> names = commaSplitter.split(s);
		short[] nums = new short[Iterables.size(names)];
		int i = 0;
		for (String name : names)
			nums[i++] = ck_find(name).num;
		return nums;
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
