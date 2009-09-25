package suneido.language;

public class BuiltinFunction extends SuFunction {

	@Override
	public String typeName() {
		return "Builtin";
	}

	// not used by builtin functions
	@Override
	public Object call(Object self, Object... args) {
		return null;
	}
}
