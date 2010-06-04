package suneido.language.builtin;

import java.io.File;
import java.math.BigDecimal;

import suneido.language.*;

public class GetCurrentDiskFreeSpace extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return BigDecimal.valueOf(new File(".").getUsableSpace());
	}

}
