package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public class Callback extends ComplexType {

	//
	// CONSTRUCTORS
	//

	protected Callback(String suTypeName, TypeList parameters) {
		super(TypeId.CALLBACK, suTypeName, parameters);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		throw new UnsupportedOperationException("Not implemented");
	}
}
