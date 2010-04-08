package suneido.language.builtin;

import java.net.URL;

import suneido.Suneido;
import suneido.language.*;

public class ExePath extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		URL uri = Suneido.class.getProtectionDomain().getCodeSource().getLocation();
		return uri.getPath();
	}

	public static void main(String[] args) {
		System.out.println(new ExePath().call());
	}

}
