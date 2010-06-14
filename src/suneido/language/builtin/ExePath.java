package suneido.language.builtin;

import java.net.URL;

import suneido.Suneido;
import suneido.language.*;

public class ExePath extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		URL uri = Suneido.class.getProtectionDomain().getCodeSource().getLocation();
		String path = uri.getPath();
		if (path.matches("/[a-zA-Z]:.*"))
			path = path.substring(1);
		return path;
	}

	public static void main(String[] args) {
		System.out.println(new ExePath().call());
	}

}
