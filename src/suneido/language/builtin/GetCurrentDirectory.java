package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.language.*;

public class GetCurrentDirectory extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		try {
			return new File(".").getCanonicalPath();
		} catch (IOException e) {
			throw new SuException("GetCurrentDirectory: " + e, e);
		}
	}

}
