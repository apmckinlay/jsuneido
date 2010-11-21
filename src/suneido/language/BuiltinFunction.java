package suneido.language;

public abstract class BuiltinFunction extends SuFunction {

	@Override
	public String typeName() {
		return "Builtin";
	}

	// must be defined by subclasses
	@Override
	public abstract Object call(Object...args);

	// not used by builtin functions
	@Override
	public Object call(Object self, Object... args) {
		return null;
	}

	// TODO support Params

}
