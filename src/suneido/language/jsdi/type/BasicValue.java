package suneido.language.jsdi.type;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.StorageType;

// TODO doc
@DllInterface
public final class BasicValue extends Type {

	//
	// DATA
	//

	private final BasicPointer pointerType;
	private final BasicType basicType;

	//
	// CONSTRUCTORS
	//

	BasicValue(BasicType basicType) {
		super(TypeId.BASIC, StorageType.VALUE, basicType.getMarshallPlan());
		this.basicType = basicType;
		this.pointerType = new BasicPointer(this);
	}

	//
	// ACCESSORS
	//

	public BasicPointer getPointerType() {
		return pointerType;
	}

	public BasicType getBasicType() {
		return basicType;
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return basicType.toIdentifier();
	}
}
