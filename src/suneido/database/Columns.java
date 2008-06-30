package suneido.database;

import java.util.ArrayList;
import java.util.Collections;

import suneido.SuException;

public class Columns {
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

	private Column find(String name) {
		for (Column c : columns)
			if (name.equals(c.name))
				return c;
		return null;
	}

	public boolean hasColumn(String name) {
		return find(name) != null;
	}
}
