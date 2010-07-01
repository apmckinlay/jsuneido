package suneido.language.builtin;

import suneido.language.*;

public class OperatingSystem extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return System.getProperty("os.name");
	}

	public static void main(String[] args) {
		System.out.println("Operating System " + System.getProperty("os.name"));
	}

}
