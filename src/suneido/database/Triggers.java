/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import javax.annotation.concurrent.ThreadSafe;

import suneido.*;
import suneido.database.server.DbmsTranLocal;
import suneido.language.Globals;
import suneido.language.Ops;
import suneido.language.builtin.SuTransaction;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

@ThreadSafe
public class Triggers {
	private static final Multiset<String> disabledTriggers =
			ConcurrentHashMultiset.create();

	static void call(Transaction tran, Table table,
			Record oldrec, Record newrec) {
		if (disabledTriggers.contains(table.name))
			return;
		String trigger = "Trigger_" + table.name;
		Object fn = Globals.tryget(trigger);
		if (fn == null)
			return;
		if (!SuValue.isCallable(fn))
			throw new SuException(trigger + " not callable (" + Ops.typeName(fn) + ")");
		SuTransaction t = new SuTransaction(new DbmsTranLocal(tran));
		try {
			Ops.call(fn, t,
					oldrec == null ? false : new SuRecord(oldrec, table.fields, t),
					newrec == null ? false : new SuRecord(newrec, table.fields, t));
		} catch (SuException e) {
			//e.printStackTrace();
			throw new SuException(e + " (" + trigger + ")", e);
		}
	}

	public static void disableTrigger(String table) {
		disabledTriggers.add(table);
	}

	public static void enableTrigger(String table) {
		disabledTriggers.remove(table);
	}

}
