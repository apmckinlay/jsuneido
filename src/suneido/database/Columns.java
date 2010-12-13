package suneido.database;

import static suneido.util.Util.commaSplitter;
import static suneido.util.Util.listToCommas;

import java.util.*;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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

	public String schemaColumns() {
		if (columns.isEmpty())
			return "";
		List<String> cols = new ArrayList<String>();
		for (Column c : columns)
			if (c.num >= 0)
				cols.add(c.name);
		// reverse rule order to match cSuneido
		int i = cols.size();
		for (Column c : columns)
			if (c.num < 0)
				cols.add(i, c.name.substring(0,1).toUpperCase() + c.name.substring(1));
		return listToCommas(cols);
	}

	public Iterator<Column> iterator() {
		return columns.iterator();
	}

	public int maxNum() {
		int maxNum = -1;
		for (Column c : columns)
			if (c.num > maxNum)
				maxNum = c.num;
		return maxNum;
	}

	@Override
	public String toString() {
		return "Columns " + Iterables.toString(this);
	}

}
