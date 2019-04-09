/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

import suneido.SuException;
import suneido.SuRecord;
import suneido.SuValue;
import suneido.Suneido;
import suneido.database.immudb.Record;
import suneido.database.server.DbmsTranLocal;
import suneido.database.immudb.Table;
import suneido.database.immudb.Transaction;
import suneido.runtime.builtin.SuTransaction;
import suneido.util.ThreadSafe;

@ThreadSafe
public class Triggers {
	private final Multiset<String> disabledTriggers =
			ConcurrentHashMultiset.create();

	public void call(Transaction tran, Table table, Record oldrec, Record newrec) {
		if (disabledTriggers.contains(table.name()))
			return;
		String trigger = "Trigger_" + table.name();
		Object fn = Suneido.context.tryget(trigger);
		if (fn == null)
			return;
		if (!SuValue.isCallable(fn))
			throw new SuException(trigger + " not callable (" + Ops.typeName(fn) + ")");
		SuTransaction t = new SuTransaction(new DbmsTranLocal(tran));
		try {
			Ops.call(fn, t,
					oldrec == null ? false : new SuRecord(oldrec, table.getFields(), t),
					newrec == null ? false : new SuRecord(newrec, table.getFields(), t));
		} catch (Exception e) {
			throw new SuException(e + " (" + trigger + ")", e);
		}
	}

	public void disableTrigger(String table) {
		disabledTriggers.add(table);
	}

	public void enableTrigger(String table) {
		disabledTriggers.remove(table);
	}

}
