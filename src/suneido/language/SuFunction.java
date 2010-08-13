package suneido.language;

import static suneido.SuException.methodNotFound;

/**
 * a Suneido function compiles to a class that extends SuFunction
 * with the definition in a "call" method
 * Note: anonymous functions compile to methods in their containing class
 * and are represented by {@link AnonFunction} which extends SuMethod
 *
 * @author Andrew McKinlay
 */
abstract public class SuFunction extends SuCallable {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Params")
			return Params(self, args);
		throw methodNotFound(self, method);
	}

	private Object Params(Object self, Object[] args) {
		return params[0].params(); // will it always be zero?
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
