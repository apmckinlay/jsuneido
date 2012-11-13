package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.BuiltinClass2;

public class RecordClass extends BuiltinClass2 {

	@Override
	public Object newInstance(Object... args) {
		return create(args);
	}

	/** used by direct calls in generated code */
	public static Object create(Object... args) {
		return Args.collectArgs(new SuRecord(), args);
	}

}
