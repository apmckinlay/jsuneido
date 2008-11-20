package suneido.database;

import java.util.*;

import suneido.SuException;

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
		String[] names = s.split(",");
		short[] nums = new short[names.length];
		int n = 0;
		for (String name : names)
			nums[n++] = ck_find(name).num;
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
