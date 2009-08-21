package suneido.language.builtin;

import static suneido.language.UserDefined.userDefinedClass;
import static suneido.util.Util.array;

import java.util.Map;

import suneido.SuException;
import suneido.SuRecord;
import suneido.language.*;
import suneido.util.Util;

public class RecordMethods {

	public static Object invoke(SuRecord r, String method, Object... args) {
		if (method == "Copy")
			return Copy(r, args);
		if (method == "Delete")
			return Delete(r, args);
		if (method == "GetDeps")
			return GetDeps(r, args);
		if (method == "Invalidate")
			return Invalidate(r, args);
		if (method == "New?")
			return NewQ(r, args);
		if (method == "Observer")
			return Observer(r, args);
		if (method == "RemoveObserver")
			return RemoveObserver(r, args);
		if (method == "SetDeps")
			return SetDeps(r, args);
		if (method == "Transaction")
			return Transaction(r, args);
		if (method == "Update")
			return Update(r, args);

		SuClass c = userDefinedClass("Records", method);
		if (c != null)
			return c.invoke(r, method, args);
		return ContainerMethods.invoke(r, method, args);
	}

	private static Object Copy(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuRecord(r);
	}

	private static final Object nil = new Object();
	private static final FunctionSpec deleteFS =
		new FunctionSpec(array("key"), nil);

	private static Object Delete(SuRecord r, Object[] args) {
		args = Args.massage(deleteFS, args);
		if (args[0] != nil)
			return ContainerMethods.Delete(r, args);
		r.delete();
		return Boolean.TRUE;
	}

	private static Object GetDeps(SuRecord r, Object[] args) {
		args = Args.massage(FunctionSpec.value, args);
		return Util.listToCommas(r.getdeps(Ops.toStr(args[0])));
	}

	private static Object Invalidate(SuRecord r, Object[] args) {
		ArgsIterator iter = new ArgsIterator(args);
		while (iter.hasNext()) {
			Object arg = iter.next();
			if (arg instanceof Map.Entry)
				throw new SuException("usage: record.Invalidate(member, ...)");
			r.invalidate(arg);
			r.callObservers(arg);
		}
		return null;
	}

	private static Boolean NewQ(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return r.isNew();
	}

	private static Object Observer(SuRecord r, Object[] args) {
		args = Args.massage(FunctionSpec.value, args);
		r.addObserver(args[0]);
		return null;
	}

	private static Object RemoveObserver(SuRecord r, Object[] args) {
		args = Args.massage(FunctionSpec.value, args);
		r.removeObserver(args[0]);
		return null;
	}

	private static final FunctionSpec setdepsFS =
			new FunctionSpec("field", "string");

	private static Object SetDeps(SuRecord r, Object[] args) {
		args = Args.massage(setdepsFS, args);
		r.setdeps(Ops.toStr(args[0]), Ops.toStr(args[1]));
		return null;
	}

	private static Object Transaction(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		Object t = r.getTransaction();
		return t == null ? Boolean.FALSE : t;
	}

	private static Object Update(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		r.update();
		return Boolean.TRUE;
	}

}
