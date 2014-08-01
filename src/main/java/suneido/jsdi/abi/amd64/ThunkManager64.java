/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.ReturnTypeGroup;
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
		final Callback64 c = (Callback64)callback;
		final MarshallPlan p = c.getMarshallPlan(); // Triggers a bind on params
		final NativeCall64 nc = NativeCall64.get(p.getStorageCategory(),
				ReturnTypeGroup.INTEGER, c.params.size(),
				c.params.needsFpRegister(), false);
		final int registerUsage = c.params.getRegisterUsage();
		newThunk64(callback, boundValue, p.getSizeDirect(), p.getSizeTotal(),
				p.getPtrArray(), p.getVariableIndirectCount(), registerUsage,
				c.params.size(), jsdi.isFastMode() && nc.isFastCallable(),
				addrs);
	}

	@Override
	protected void deleteThunk(long thunkObjectAddr) {
		deleteThunk64(thunkObjectAddr);
	}

	//
	// NATIVE CALLS
	//

	private static native void newThunk64(Callback callback,
			SuValue boundValue, int sizeDirect, int sizeTotal, int[] ptrArray,
			int variableIndirectCount, int registers, int numParams,
			boolean makeFastCall, long[] outThunkAddrs);

	private static native void deleteThunk64(long thunkObjectAddr);
}
