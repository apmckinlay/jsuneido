/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.commaSplitter;
import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Immutable
class Columns implements Iterable<Column> {
	final ImmutableList<Column> columns;

	Columns(ImmutableList<Column> columns) {
		this.columns = columns;
	}

	ImmutableList<Integer> nums(String s) {
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (String name : commaSplitter(s))
			builder.add(ck_find(name).field);
		return builder.build();
	}

	String names(int[] nums) {
		if (nums.length == 0)
			return "";
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

	Column find(String name) {
		for (Column c : columns)
			if (name.equals(c.name))
				return c;
		return null;
	}

	Column find(int num) {
		for (Column c : columns)
			if (c.field == num)
				return c;
		return null;
	}

	boolean hasColumn(String name) {
		return find(name) != null;
	}

	int size() {
		return columns.size();
	}

	ImmutableList<String> names() {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		for (Column c : columns)
			list.add(c.name);
		return list.build();
	}

	String schemaColumns() {
		if (columns.isEmpty())
			return "";
		List<String> cols = new ArrayList<String>();
		for (Column c : columns)
			if (c.field >= 0)
				cols.add(c.name);
		// NOT reversing rule order like cSuneido
		for (Column c : columns)
			if (c.field < 0)
				cols.add(c.name.substring(0,1).toUpperCase() + c.name.substring(1));
		return listToCommas(cols);
	}

	@Override
	public Iterator<Column> iterator() {
		return columns.iterator();
	}

	int maxNum() {
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
