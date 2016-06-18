/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.commaSplitter;
import static suneido.util.Util.listToCommas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import suneido.SuException;

/**
 * A wrapper for a list of {@link Column}'s.
 * Used by {@link Table}.
 */
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

	int[] numsArray(String names) {
		if (names.isEmpty())
			return new int[0];
		Iterable<String> cs = commaSplitter.split(names);
		int[] nums = new int[Iterables.size(cs)];
		int c = 0;
		for (String name : cs)
			nums[c++] = ck_find(name).field;
		return nums;
	}

	String names(int[] nums) {
		if (nums.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (int n : nums)
			sb.append(',').append(find(n).name);
		return sb.substring(1);
	}

	List<String> namesList(int[] nums) {
		if (nums.length == 0)
			return Collections.emptyList();
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (int n : nums)
			builder.add(find(n).name);
		return builder.build();
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
		List<String> cols = new ArrayList<>();
		for (Column c : columns)
			if (c.field >= 0)
				cols.add(c.name);
		// NOT reversing rule order like cSuneido
		for (Column c : columns)
			if (c.field == -1)
				cols.add(c.name.substring(0,1).toUpperCase() + c.name.substring(1));
			else if (c.field < -1)
				cols.add(c.name); // special e.g. _lower!
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
