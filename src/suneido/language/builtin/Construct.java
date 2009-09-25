package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.SuContainer;
import suneido.SuException;
import suneido.language.*;

public class Construct extends BuiltinFunction {

	private static final FunctionSpec constructFS =
			new FunctionSpec(array("what", "suffix"), "");
	private static Object[] noArgs = new Object[0];
	@Override
	public Object call(Object... args) {
		args = Args.massage(constructFS, args);
		String suffix = Ops.toStr(args[1]);
		Object what = args[0];
		Object[] newargs = noArgs;
		if (what instanceof SuContainer) {
			SuContainer c = Ops.toContainer(what);
			what = c.get(0);
			if (what == null)
				throw new SuException("Construct: object requires member 0");
			newargs = array(Args.Special.EACH1, c);
		}
		if (what instanceof String) {
			String className = (String) what;
			if (!className.endsWith(suffix))
				className += suffix;
			what = Globals.get(className);
		}
		return Ops.invoke(what, "<new>", newargs);
	}

}
