package suneido.language;

import static suneido.SuException.methodNotFound;

/**
 * a Suneido function compiles to a class that extends SuFunction
 * with the definition in a "call" method
 */
abstract public class SuFunction extends SuCallable {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "Params")
			return Params(self, args);
		throw methodNotFound(self, method);
	}

	private Object Params(Object self, Object[] args) {
		return params.params();
	}

	@Override
	public Object call(Object... args) {
		return call(this, args);
	}

	/** defined by compiled functions */
	public abstract Object call(Object self, Object... args);

	/** overridden by blocks */
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
		return super.typeName().replace(AstCompile.METHOD_SEPARATOR, '.');
	}

}
