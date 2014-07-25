/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlan;
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
	public MarshallPlan makeParamsMarshallPlan(boolean isCallbackPlan,
			boolean hasViReturnValue) {
// FIXME: Do we have to resolve the type list, or is it already resolved??????
//        (At least, should it assert it is resolved??)
		int variableIndirectCount = getVariableIndirectCount();
		if (hasViReturnValue) {
			++variableIndirectCount;
		}
		final MarshallPlanBuilderX86 builder = new MarshallPlanBuilderX86(
			variableIndirectCount,
			true
		);
		addToPlan(builder, isCallbackPlan);
		if (hasViReturnValue) {
			// Need to add another variable indirect slot for the variable
			// indirect return value pseudo-parameter.
			builder.ptrVariableIndirectPseudoParam();
		}
		return builder.makeMarshallPlan();
	}

	@Override
	public MarshallPlan makeMembersMarshallPlan() {
// FIXME: Do we have to resolve the type list, or is it already resolved??????
//      (At least, should it assert it is resolved??)
		final MarshallPlanBuilderX86 builder = new MarshallPlanBuilderX86(
			getVariableIndirectCount(),
			false
		);
		addToPlan(builder, false);
		return builder.makeMarshallPlan();
	}
}
