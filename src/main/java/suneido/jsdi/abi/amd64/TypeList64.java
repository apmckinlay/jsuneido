/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.type.LateBinding;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeId;
import suneido.jsdi.type.TypeList;

/**
 * Type list specialized for Windows AMD64 platform.
 * 
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
public final class TypeList64 extends TypeList {

	//
	// CONSTRUCTORS
	//

	TypeList64(Args args) {
		super(args);
		// If this is a parameters plan, shim the LateBinding pass-by-value
		// types where necessary.
		if (isParams()) {
			int k = 0;
			for (final Entry entry : this) {
				final Type type = entry.getType();
				if (TypeId.LATE_BINDING == type.getTypeId() &&
						StorageType.VALUE == type.getStorageType()) {
					modifyEntryType(k, new ByValShim((LateBinding)type));
				}
				++k;
			}
		}
	}

	//
	// ANCESTOR CLASS: TypeList
	//

	@Override
	protected MarshallPlanBuilder makeBuilder(int variableIndirectCount,
			boolean alignToWordBoundary) {
		throw new SuInternalError("not implemented"); // TODO: implement me
		//return new MarshallPlanBuilderX86(variableIndirectCount, alignToWordBoundary);
	}
}
