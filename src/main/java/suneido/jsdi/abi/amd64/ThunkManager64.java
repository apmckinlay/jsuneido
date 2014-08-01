/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.type.Callback;

/**
 * Thunk manager specialized for the amd64 platform.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class ThunkManager64 extends ThunkManager {

	//
	// CONSTRUCTORS
	//

	ThunkManager64(JSDI jsdi) {
		super(jsdi);
	}

	//
	// ANCESTOR CLASS: ThunkManager
	//

	@Override
	protected void newThunk(SuValue boundValue, Callback callback, long[] addrs) {
		MarshallPlan plan = callback.getMarshallPlan();
//		newThunk64(callback, boundValue, plan.getSizeDirect(),
//				plan.getSizeTotal(), plan.getPtrArray(),
//				plan.getVariableIndirectCount(), addrs);
throw new RuntimeException("not implemented"); // FIXME: core funtionality not implemented
	}

	@Override
	protected void deleteThunk(long thunkObjectAddr) {
		deleteThunk64(thunkObjectAddr);
	}

	//
	// NATIVE CALLS
	//

	private static native void newThunk64(Callback callback, SuValue boundValue,
			int sizeDirect, int registers, int sizeTotal, int[] ptrArray,
			int variableIndirectCount, long[] outThunkAddrs);

	private static native void deleteThunk64(long thunkObjectAddr);

}
