package suneido.language.builtin;

import static suneido.util.Util.array;

import java.io.File;
import java.math.BigDecimal;

import suneido.language.*;

public class GetDiskFreeSpace extends BuiltinFunction1 {
	{ params = new FunctionSpec(array("dir"), "."); }

	@Override
	public Object call1(Object a) {
		String dir = Ops.toStr(a);
		return BigDecimal.valueOf(new File(dir).getUsableSpace());
	}

}
