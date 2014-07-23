/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlan;
import suneido.jsdi.PrimitiveSize;
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
		int sizeIndirect = getSizeIndirect();
		int variableIndirectCount = getVariableIndirectCount();
		if (hasViReturnValue) {
			sizeIndirect += PrimitiveSize.POINTER;
			++variableIndirectCount;
		}
		final MarshallPlanBuilderX86 builder = new MarshallPlanBuilderX86(
			getSizeDirectWholeWords(),
			sizeIndirect,
			variableIndirectCount,
			true
		);
		addToPlan(builder, isCallbackPlan);
		if (hasViReturnValue) {
			builder.variableIndirectPseudoArg();
		}
		return builder.makeMarshallPlan();
	}

	@Override
	public MarshallPlan makeMembersMarshallPlan() {
		final MarshallPlanBuilderX86 builder = new MarshallPlanBuilderX86(
			getSizeDirectIntrinsic(),
			getSizeIndirect(),
			getVariableIndirectCount(),
			false
		);
		addToPlan(builder, false);
		return builder.makeMarshallPlan();
	}
}
