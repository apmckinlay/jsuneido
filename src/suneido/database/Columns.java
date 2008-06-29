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

	public short[] commaToNums(String s) {
		String[] names = s.split(",");
		short[] nums = new short[names.length];
		int n = 0;
		for (String name : names)
			nums[n++] = find(name).num;
		return nums;
	}

	private Column find(String name) {
		for (Column col : columns)
			if (name.equals(col.name))
				return col;
		throw new SuException("column not found: " + name);
	}
}
