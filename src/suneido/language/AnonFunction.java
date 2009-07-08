package suneido.language;

public class AnonFunction extends SuMethod {

	public AnonFunction(String method) {
		super(null, method);
	}

	@Override
	public String typeName() {
		return "Function";
	}

	@Override
	public String toString() {
		return "aFunction";
	}

}
