package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.zip.Adler32;
import java.util.zip.Checksum;

import suneido.SuValue;
import suneido.language.*;

public class Adler32Class extends BuiltinClass {

	@Override
	public Instance newInstance(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new Instance();
	}

	private static Object nil = new Object();
	private static final FunctionSpec fs =
			new FunctionSpec(array("string"), nil);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		Instance f = new Instance();
		if (args[0] == nil)
			return f;
		else
			return f.Update(args).Value();
	}

	private static class Instance extends SuValue {
		Checksum cksum = new Adler32();

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Update")
				return Update(args);
			if (method == "Value")
				return Value(args);
			return super.invoke(self, method, args);
		}

		private Instance Update(Object... args) {
			args = Args.massage(FunctionSpec.string, args);
			String s = Ops.toStr(args[0]);
			byte[] b = s.getBytes();
			cksum.update(b, 0, b.length);
	                return this;
                }

		private Object Value(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
	                return (int) cksum.getValue();
                }

	}

}
