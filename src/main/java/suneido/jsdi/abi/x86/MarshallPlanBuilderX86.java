/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallPlanBuilder;

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

	MarshallPlanBuilderX86(int variableIndirectCount,
			boolean alignToWordBoundary) {
		super(variableIndirectCount, alignToWordBoundary);
	}

	//
	// ANCESTOR CLASS: MarshallPlanBuilder
	//

	@Override
	protected MarshallPlan makeMarshallPlan(int sizeDirect, int alignDirect,
			int sizeIndirect, int sizeTotal, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		return new MarshallPlanX86(sizeDirect, alignDirect, sizeIndirect,
				sizeTotal, ptrArray, posArray, variableIndirectCount);
	}
}
