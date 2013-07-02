package suneido.language.jsdi.type;

import suneido.language.jsdi.StorageType;

public final class BasicPointer extends Type {

	//
	// DATA
	//

	private final BasicValue underlying;

	//
	// CONSTRUCTORS
	//

	BasicPointer(BasicValue underlying, long jsdiHandle) {
		super(TypeId.BASIC, StorageType.POINTER, jsdiHandle);
		assert 0 != jsdiHandle : "BasicPointer may not have a null handle";
		assert null != underlying : "Underlying BasicValue may not be null";
		this.underlying = underlying;
	}

	//
	// ACCESSORS
	//

	public BasicValue getUnderlying() {
		return underlying;
	}

	public BasicType getBasicType() {
		return underlying.getBasicType();
	}

	//
	// ANCESTOR CLASS: Type
	//

	@Override
	public String getDisplayName() {
		return getBasicType().toIdentifier() + '*';
	}
}
