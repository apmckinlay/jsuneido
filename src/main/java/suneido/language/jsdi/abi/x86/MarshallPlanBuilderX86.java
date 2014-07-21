/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import com.google.common.primitives.Ints;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.MarshallPlanBuilder;

/**
 * Specialized builder for making x86 marshall plans.
 *
 * @author Victor Schappert
 * @since 20140718
 */
@DllInterface
final class MarshallPlanBuilderX86 extends MarshallPlanBuilder {

	//
	// CONSTRUCTORS
	//

	MarshallPlanBuilderX86(int sizeDirect, int sizeIndirect,
			int variableIndirectCount, boolean alignToWordBoundary) {
		super(sizeDirect, sizeIndirect, variableIndirectCount, alignToWordBoundary);
	}

	//
	// ANCESTOR CLASS: MarshallPlanBuilder
	//

	@Override
	public MarshallPlan makeMarshallPlanInternal() {
		return new MarshallPlanX86(
			sizeDirect,
			sizeIndirect,
			Ints.toArray(ptrList),
			Ints.toArray(posList),
			variableIndirectCount
		);
	}
}
