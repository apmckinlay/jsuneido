package suneido.database;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.concurrent.ThreadSafe;

import suneido.*;
import suneido.database.server.DbmsTranLocal;
import suneido.language.Globals;
import suneido.language.Ops;
import suneido.language.builtin.TransactionInstance;

@ThreadSafe
public class Triggers {

	private static final Set<String> disabledTriggers =
			new ConcurrentSkipListSet<String>();

	public static void call(Transaction tran, Table table,
			Record oldrec, Record newrec) {
		if (disabledTriggers.contains(table.name))
			return;
		String trigger = "Trigger_" + table.name;
		Object fn = Globals.tryget(trigger);
		if (fn == null)
			return;
		if (!SuValue.isCallable(fn))
			throw new SuException(trigger + " not callable (" + Ops.typeName(fn) + ")");
		TransactionInstance t = new TransactionInstance(new DbmsTranLocal(tran));
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
