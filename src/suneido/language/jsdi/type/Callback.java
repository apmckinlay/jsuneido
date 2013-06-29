package suneido.language.jsdi.type;

public class Callback extends ComplexType {

	//
	// CONSTRUCTORS
	//

	protected Callback(TypeList parameters) {
		super(TypeId.CALLBACK, parameters);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		throw new UnsupportedOperationException("Not implemented");
	}
}
