package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public final class Callback extends ComplexType {

	//
	// CONSTRUCTORS
	//

	Callback(String suTypeName, TypeList parameters) {
		super(TypeId.CALLBACK, suTypeName, parameters);
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return "callback" + typeList.toParamsTypeString();
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
