package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;
import suneido.language.*;

import com.google.common.io.Files;

public class DeleteDir extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.string, args);
		String path = Ops.toStr(args[0]);
		File dir = new File(path);
		if (!dir.isDirectory())
			return false;
		try {
			Files.deleteRecursively(dir);
		} catch (IOException e) {
			throw new SuException("DeleteDir failed", e);
		}
		return true;
	}

}
