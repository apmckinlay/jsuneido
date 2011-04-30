/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import static suneido.util.Util.commaSplitter;
import static suneido.util.Util.listToCommas;

import java.util.*;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Immutable
public class Columns implements Iterable<Column> {
	final ImmutableList<Column> columns;

	public Columns(ImmutableList<Column> columns) {
		this.columns = columns;
	}

	public ImmutableList<Integer> nums(String s) {
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (String name : commaSplitter(s))
			builder.add(ck_find(name).field);
		return builder.build();
	}

	public String names(int[] nums) {
		StringBuilder sb = new StringBuilder();
		for (int n : nums)
			sb.append(',').append(find(n).name);
		return sb.substring(1);
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

	public Column find(int num) {
		for (Column c : columns)
			if (c.field == num)
				return c;
		return null;
	}

	public boolean hasColumn(String name) {
		return find(name) != null;
	}

	public int size() {
		return columns.size();
	}

	public ImmutableList<String> names() {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		for (Column c : columns)
			list.add(c.name);
		return list.build();
	}

	public String schemaColumns() {
		if (columns.isEmpty())
			return "";
		List<String> cols = new ArrayList<String>();
		for (Column c : columns)
			if (c.field >= 0)
				cols.add(c.name);
		// reverse rule order to match cSuneido
		int i = cols.size();
		for (Column c : columns)
			if (c.field < 0)
				cols.add(i, c.name.substring(0,1).toUpperCase() + c.name.substring(1));
		return listToCommas(cols);
	}

	public Iterator<Column> iterator() {
		return columns.iterator();
	}

	public int maxNum() {
		int maxNum = -1;
		for (Column c : columns)
			if (c.field > maxNum)
				maxNum = c.field;
		return maxNum;
	}

	@Override
	public String toString() {
		return "Columns " + Iterables.toString(this);
	}

}
