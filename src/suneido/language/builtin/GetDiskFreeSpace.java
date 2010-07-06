package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.File;
import java.math.BigDecimal;

import suneido.language.*;

public class GetDiskFreeSpace extends BuiltinFunction {

	private static FunctionSpec fs = new FunctionSpec(array("dir"), ".");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		String dir = Ops.toStr(args[0]); 
		return BigDecimal.valueOf(new File(dir).getUsableSpace());
	}

}
