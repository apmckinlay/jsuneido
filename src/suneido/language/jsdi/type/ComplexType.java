package suneido.language.jsdi.type;

import suneido.language.jsdi.StorageType;

public abstract class ComplexType extends Type {

	//
	// DATA
	//

	protected final TypeList typeList;

	//
	// CONSTRUCTORS
	//

	protected ComplexType(TypeId typeId, TypeList typeList) {
		super(typeId, StorageType.VALUE);
		this.typeList = typeList;
	}
}
