/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.intfc.database;

import java.util.List;

import com.google.common.collect.ImmutableList;

public interface Table {

	int num();

	boolean singleton();

	List<String> getColumns();

	List<List<String>> indexesColumns();

	List<List<String>> keysColumns();

	/** @return The physical fields. 1:1 match with records */
	ImmutableList<String> getFields();

	String schema();

}