/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.type.TypeList;

/**
 * Type list specialized for Windows AMD64 platform.
 * 
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
public class TypeList64 extends TypeList {

	//
	// CONSTRUCTORS
	//

	TypeList64(Args args) {
		super(args);
	}

	//
	// ANCESTOR CLASS: TypeList
	//

	@Override
	protected final MarshallPlanBuilder makeBuilder(int variableIndirectCount,
			boolean alignToWordBoundary) {
		return new MarshallPlanBuilder64(variableIndirectCount,
				alignToWordBoundary);
	}
}
