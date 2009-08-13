package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.FunctionSpec;

public class RecordMethods {

	public static Object invoke(SuRecord r, String method, Object... args) {
		if (method == "Delete")
			return Delete(r, args);
		if (method == "New?")
			return NewQ(r, args);
		if (method == "Transaction")
			return Transaction(r, args);
		if (method == "Update")
			return Update(r, args);
		// TODO Records user defined methods
		return ContainerMethods.invoke(r, method, args);
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

	private static Boolean NewQ(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return r.isNew();
	}

	private static Object Transaction(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return r.getTransaction();
	}

	private static Object Update(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		r.update();
		return Boolean.TRUE;
	}

}
