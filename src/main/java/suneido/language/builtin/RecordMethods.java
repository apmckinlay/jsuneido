/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.FunctionSpec.NA;

import java.util.Map;

import suneido.SuException;
import suneido.SuRecord;
import suneido.language.ArgsIterator;
import suneido.language.BuiltinMethods;
import suneido.language.Ops;
import suneido.language.Params;

/** {@link SuRecord} delegates invoke to here */
public class RecordMethods {
	public static final BuiltinMethods methods =
			new BuiltinMethods(RecordMethods.class, "Records");

	private RecordMethods() {
	}

	public static Object Clear(Object self) {
		((SuRecord) self).clear();
		return null;
	}

	public static Object Copy(Object self) {
		return new SuRecord((SuRecord) self);
	}

	@Params("key=NA, all=NA")
	public static Object Delete(Object self, Object key, Object all) {
		if (key == NA && all == NA) {
			((SuRecord) self).delete();
			return Boolean.TRUE;
		} else
			return ContainerMethods.delete(self, key, all);
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
		suneido.language.builtin.SuTransaction t = ((SuRecord) self).getTransaction();
		return t == null || t.isEnded() ? Boolean.FALSE : t;
	}

	public static Object Update(Object self) {
		((SuRecord) self).update();
		return Boolean.TRUE;
	}

	@Params("field, rule")
	public static Object AttachRule(Object self, Object field, Object rule) {
		((SuRecord) self).attachRule(Ops.toStr(field), rule);
		return null;
	}

}
