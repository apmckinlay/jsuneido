/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuException;
import suneido.TheDbms;
import suneido.compiler.Compiler;
import suneido.database.query.Query.Dir;
import suneido.database.server.Dbms.HeaderAndRow;

/**
 *
 *
 * NOTE: not currently used, work in progress
 */
public class LoaderLibrary implements Loader {

	@Override
	public Object load(String module, String name) {
		String query = module + " where group = -1 and name = '" + name + "'";
		HeaderAndRow hr = TheDbms.dbms().get(Dir.NEXT, query, true);
		if (hr == null)
			return null;
		String text = (String) hr.row.getval(hr.header, "text");
		try {
			return Compiler.compile(name, text);
		} catch (Exception e) {
			throw new SuException("error loading " + name, e);
		}
	}

}
