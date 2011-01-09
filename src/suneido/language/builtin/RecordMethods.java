/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.FunctionSpec.NA;
import static suneido.util.Util.array;

import java.util.Map;

import suneido.SuException;
import suneido.SuRecord;
import suneido.language.*;
import suneido.util.Util;

/** {@link SuRecord} delegates invoke to here */
public class RecordMethods {
	public static final BuiltinMethods methods =
		new BuiltinMethods(RecordMethods.class, "Records");

	private RecordMethods() {
	}

	public static class Clear extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			((SuRecord) self).clear();
			return null;
		}
	}

	public static class Copy extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return new SuRecord((SuRecord) self);
		}
	}

	public static class Delete extends SuMethod2 {
		{ params = new FunctionSpec(array("key", "all"), NA, NA); }
		@Override
		public Object eval2(Object self, Object key, Object all) {
			if (key == NA && all == NA) {
				((SuRecord) self).delete();
				return Boolean.TRUE;
			} else
				return ContainerMethods.delete(self, key, all);
		}
	}

	public static class GetDeps extends SuMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			return Util.listToCommas(((SuRecord) self).getdeps(Ops.toStr(a)));
		}
	}

	public static class Invalidate extends SuMethod {
		@Override
		public Object eval(Object self, Object... args) {
			ArgsIterator iter = new ArgsIterator(args);
			SuRecord r = (SuRecord) self;
			while (iter.hasNext()) {
				Object arg = iter.next();
				if (arg instanceof Map.Entry)
					throw new SuException("usage: record.Invalidate(member, ...)");
				r.invalidate(arg);
				r.callObservers(arg);
			}
			return null;
		}
	}

	public static class NewQ extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return ((SuRecord) self).isNew();
		}
	}

	public static class Observer extends SuMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			((SuRecord) self).addObserver(a);
			return null;
		}
	}

	public static class PreSet extends SuMethod2 {
		{ params = new FunctionSpec("field", "value"); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			((SuRecord) self).preset(a, b);
			return null;
		}
	}

	public static class RemoveObserver extends SuMethod1 {
		@Override
		public Object eval1(Object self, Object a) {
			((SuRecord) self).removeObserver(a);
			return null;
		}
	}

	public static class SetDeps extends SuMethod2 {
		{ params = new FunctionSpec("field", "string"); }
		@Override
		public Object eval2(Object self, Object a, Object b) {
			((SuRecord) self).setdeps(Ops.toStr(a), Ops.toStr(b));
			return null;
		}
	}

	public static class Transaction extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			suneido.language.builtin.SuTransaction t = ((SuRecord) self).getTransaction();
			return t == null || t.isEnded() ? Boolean.FALSE : t;
		}
	}

	public static class Update extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			((SuRecord) self).update();
			return Boolean.TRUE;
		}
	}

}
