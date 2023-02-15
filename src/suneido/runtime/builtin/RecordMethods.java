/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.Map;

import suneido.SuException;
import suneido.SuObject;
import suneido.SuRecord;
import suneido.runtime.*;

/** Used by {@link SuRecord} */
public class RecordMethods {
	public static final BuiltinMethods methods =
			new BuiltinMethods("record", RecordMethods.class, "Records");

	private RecordMethods() {
	}

	public static Object Clear(Object self) {
		((SuRecord) self).clear();
		return null;
	}

	public static Object Copy(Object self) {
		return new SuRecord((SuRecord) self);
	}

	// handle dbRecord.Delete()
	public static Object Delete(Object self, Object... args) {
		if (args.length != 0)
			return ObjectMethods.delete(self, args);
		((SuRecord) self).delete();
		return null;
	}

	public static Object Drop(Object self, Object... args) {
		((SuRecord) self).delete();
		return null;
	}

	@Params("member, block")
	public static Object GetDefault(Object self, Object a, Object b) {
		Object x = ((SuRecord) self).getDef(a, null);
		if (x != null)
			return x;
		return SuCallable.isBlock(b) ? Ops.call(b) : b;
	}

	@Params("field")
	public static Object GetDeps(Object self, Object a) {
		return ((SuRecord) self).getdeps(Ops.toStr(a));
	}

	public static Object Invalidate(Object self, Object... args) {
		ArgsIterator iter = new ArgsIterator(args);
		SuRecord r = (SuRecord) self;
		while (iter.hasNext()) {
			Object arg = iter.next();
			if (arg instanceof Map.Entry)
				throw new SuException("usage: record.Invalidate(member, ...)");
			r.invalidate(arg);
		}
		return null;
	}

	public static Object NewQ(Object self) {
		return ((SuRecord) self).isNew();
	}

	@Params("observer")
	public static Object Observer(Object self, Object a) {
		((SuRecord) self).addObserver(a);
		return null;
	}

	@Params("field, value")
	public static Object PreSet(Object self, Object a, Object b) {
		((SuRecord) self).preset(a, b);
		return null;
	}

	@Params("observer")
	public static Object RemoveObserver(Object self, Object a) {
		((SuRecord) self).removeObserver(a);
		return null;
	}

	@Params("field, string")
	public static Object SetDeps(Object self, Object a, Object b) {
		((SuRecord) self).setdeps(Ops.toStr(a), Ops.toStr(b));
		return null;
	}

	public static Object Transaction(Object self) {
		suneido.runtime.builtin.SuTransaction t = ((SuRecord) self).getTransaction();
		return t == null || t.isEnded() ? Boolean.FALSE : t;
	}

	@Params("object = false")
	public static Object Update(Object self, Object a) {
		SuRecord rec = (SuRecord) self;
		SuObject newrec = (a == Boolean.FALSE) ? rec : Ops.toObject(a);
		rec.update(newrec);
		return null;
	}

	@Params("field, rule")
	public static Object AttachRule(Object self, Object field, Object rule) {
		((SuRecord) self).attachRule(Ops.toStr(field), rule);
		return null;
	}

}
