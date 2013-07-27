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

	@Override
	public int getSizeDirectIntrinsic() {
		return PrimitiveSize.POINTER;
	}

	@Override
	public int getSizeDirectWholeWords() {
		return PrimitiveSize.pointerWholeWordBytes();
	}

	// TODO: implement marshall in?
	// TODO: implement marshall out?

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getDisplayName(); // Can be result of Suneido 'Display' built-in.
	}
}
