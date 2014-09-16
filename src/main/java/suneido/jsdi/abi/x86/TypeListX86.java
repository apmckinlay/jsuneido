/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlanBuilder;
import suneido.jsdi.type.TypeList;

/**
 * Type list specialized for x86 platform.
 *
 * @author Victor Schappert
 * @since 20140719
 */
@DllInterface
final class TypeListX86 extends TypeList {

	//
	// CONSTRUCTORS
	//

	TypeListX86(Args args) { // Deliberately package-internals
		super(args);
	}

	//
	// ANCESTOR CLASS: TypeList
	//

	@Override
	protected MarshallPlanBuilder makeBuilder(int variableIndirectCount,
			boolean alignToWordBoundary) {
		return new MarshallPlanBuilderX86(variableIndirectCount,
				alignToWordBoundary);
	}
}
