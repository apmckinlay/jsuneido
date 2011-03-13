package suneido.language.builtin;

import suneido.language.*;

public class GetTempPath extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return System.getProperty("java.io.tmpdir");
	}

	public static void main(String[] args) {
		System.out.println(new GetTempPath().call());
	}

}
