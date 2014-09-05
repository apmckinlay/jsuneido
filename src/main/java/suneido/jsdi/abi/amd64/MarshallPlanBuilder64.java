/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.MarshallPlanBuilder;

/**
 * Specialized builder for making amd64 marshall plans.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class MarshallPlanBuilder64 extends MarshallPlanBuilder {

	//
	// CONSTRUCTORS
	//

	MarshallPlanBuilder64(int variableIndirectCount, boolean alignToWordBoundary) {
		super(variableIndirectCount, alignToWordBoundary);
	}

	//
	// ANCESTOR CLASS: MarshallPlanBuilder
	//

	@Override
	protected MarshallPlan makeMarshallPlan(int sizeDirect, int alignDirect,
			int sizeIndirect, int sizeTotal, int variableIndirectCount,
			int[] ptrArray, int[] posArray) {
		return new MarshallPlan64(sizeDirect, alignDirect, sizeIndirect,
				sizeTotal, ptrArray, posArray, variableIndirectCount);
	}
}
