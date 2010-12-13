package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.language.*;

import com.google.common.io.Files;

public class CopyFile extends BuiltinFunction {

	private static final FunctionSpec fs =
			new FunctionSpec("from", "to", "failIfExists");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		File from = new File(Ops.toStr(args[0]));
		File to = new File(Ops.toStr(args[1]));
		boolean failIfExists = Ops.toBoolean(args[2]);
		if (to.exists() && (failIfExists || ! to.delete()))
			return false;
		try {
			Files.copy(from, to);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
