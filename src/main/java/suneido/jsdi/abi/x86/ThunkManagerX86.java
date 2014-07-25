/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.type.Callback;

/**
 * Thunk manager specialized for the x86 platform.
 *
 * @author Victor Schappert
 * @since 20140714
 */
@DllInterface
final class ThunkManagerX86 extends ThunkManager {

	//
	// CONSTRUCTORS
	//

	ThunkManagerX86(JSDI jsdi) {
		super(jsdi);
	}

	//
	// ANCESTOR CLASS: ThunkManager
	//

	@Override
	protected void newThunk(SuValue boundValue, Callback callback, long[] addrs) {
		MarshallPlanX86 plan = ((CallbackX86)callback).getMarshallPlan();
		newThunkX86(callback, boundValue, plan.getSizeDirect(),
				plan.getSizeTotal(), plan.getPtrArray(),
				plan.getVariableIndirectCount(), addrs);
	}

	@Override
	protected void deleteThunk(long thunkObjectAddr) {
		deleteThunkX86(thunkObjectAddr);
	}

	//
	// NATIVE CALLS
	//

	private static native void newThunkX86(Callback callback, SuValue boundValue,
			int sizeDirect, int sizeTotal, int[] ptrArray,
			int variableIndirectCount, long[] outThunkAddrs);

	private static native void deleteThunkX86(long thunkObjectAddr);
}
