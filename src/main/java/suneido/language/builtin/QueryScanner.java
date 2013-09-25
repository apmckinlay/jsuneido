package suneido.language.builtin;

import suneido.language.Args;
import suneido.language.BuiltinClass;
import suneido.language.FunctionSpec;
import suneido.language.Ops;

public class QueryScanner extends Scanner {
	
	public QueryScanner(String s) {
		super(s);
		lexer.ignoreCase();
	}

	public static final BuiltinClass clazz = new BuiltinClass() {
		@Override
		public QueryScanner newInstance(Object... args) {
			args = Args.massage(FunctionSpec.string, args);
			return new QueryScanner(Ops.toStr(args[0]));
		}
	};

}
