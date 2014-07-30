/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.type;

import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.ElementSkipper;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130625
 */
@DllInterface
public abstract class ComplexType extends Type {

	//
	// DATA
	//

	private   final String         valueName;
	protected final TypeList       typeList;
	protected       ElementSkipper skipper; // only valid to last addToPlan()

	//
	// CONSTRUCTORS
	//

	protected ComplexType(TypeId typeId, String valueName, TypeList typeList) {
		super(typeId, StorageType.VALUE);
		if (null == valueName)
			throw new IllegalArgumentException("suTypeName cannot be null");
		this.valueName = valueName;
		this.typeList = typeList;
	}

	//
	// ACCESSORS
	//

	// TODO: Docs since 20130725
	public final ElementSkipper getElementSkipper() {
		return skipper;
	}

	//
	// MUTATORS
	//

	// TODO: docs
	boolean resolve(int level) {
		return false;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public final String valueName() {
		return valueName;
	}
}
