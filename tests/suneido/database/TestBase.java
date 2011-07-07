/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import org.junit.After;

import suneido.database.query.*;
import suneido.database.server.ServerData;

public class TestBase extends TestBaseBase {
	protected static final ServerData serverData = new ServerData();

	@After
	public void close() {
		db.close();
	}

	protected void reopen() {
		db.close();
		db = new Database(dest, Mode.OPEN);
	}

	protected static Record key(int i) {
		return new Record().add(i);
	}

	protected Record record(int... values) {
		Record r = new Record();
		for (int i : values)
			r.add(i);
		return r;
	}

	protected int req(String s) {
		Transaction tran = db.readwriteTran();
		try {
			Query q = CompileQuery.parse(tran, serverData, s);
			int n = ((QueryAction) q).execute();
			tran.ck_complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

}