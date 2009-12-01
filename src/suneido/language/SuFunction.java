package suneido.language;

import static suneido.SuException.methodNotFound;

/**
 * an SuFunction is implemented as a class with the definition in a "call"
 * method
 *
 * @author Andrew McKinlay
 */
abstract public class SuFunction extends SuCallable {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Type")
			return "function";
		// TODO other standard methods on functions e.g. Params
		else
			throw methodNotFound(self, method);
	}

	@Override
	public Object call(Object... args) {
		return call(this, args);
	}

	// defined by compiled functions
	public abstract Object call(Object self, Object... args);

	@Override
	public Object eval(Object self, Object... args) {
		return call(self, args);
	}

	@Override
	public String typeName() {
		return "Function";
	}

	@Override
	public String toString() {
		return super.typeName();
	}

}
