package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.zip.Adler32;
import java.util.zip.Checksum;

import suneido.SuValue;
import suneido.language.*;

public class Adler32Class extends BuiltinClass {

	@Override
	public Adler32Instance newInstance(Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		return new Adler32Instance();
	}

	private static Object nil = new Object();
	private static final FunctionSpec fs =
			new FunctionSpec(array("string"), nil);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		Adler32Instance f = new Adler32Instance();
		if (args[0] == nil)
			return f;
		else
			return f.Update(args).Value();
	}

	private static class Adler32Instance extends SuValue {
		Checksum cksum = new Adler32();

		@Override
		public Object invoke(Object self, String method, Object... args) {
			if (method == "Update")
				return Update(args);
			if (method == "Value")
				return Value(args);
			return super.invoke(self, method, args);
		}

		private Adler32Instance Update(Object... args) {
			args = Args.massage(FunctionSpec.string, args);
			String s = Ops.toStr(args[0]);
			for (int i = 0; i < s.length(); ++i)
				cksum.update(s.charAt(i));
			return this;
		}

		private Object Value(Object... args) {
			Args.massage(FunctionSpec.noParams, args);
			return (int) (cksum.getValue());
		}

	}

}
