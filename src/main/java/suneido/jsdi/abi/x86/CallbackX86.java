/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import java.util.Arrays;

import suneido.SuValue;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;
import suneido.jsdi.Marshaller;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.VariableIndirectInstruction;
import suneido.jsdi.type.Callback;
import suneido.jsdi.type.ProxyResolveException;
import suneido.jsdi.type.TypeList;

/**
 * Callback specialized for x86 {@code __stdcall} ABI.
 *
 * @author Victor Schappert
 * @since 20140719
 */
@DllInterface
final class CallbackX86 extends Callback {

	//
	// DATA
	//

	private MarshallPlanX86 marshallPlan;

	//
	// CONSTRUCTORS
	//

	CallbackX86(String valueName, TypeList parameters, ThunkManager thunkManager) {
		super(valueName, parameters, thunkManager);
		this.marshallPlan = null;
	}

	//
	// ACCESSORS
	//

	MarshallPlanX86 getMarshallPlan() { // Called by ThunkManagerX86
		// TODO: resolve thread safety and update issues --
		// this will cause problems if marshall plan on an already bound
		// thunk can change
		try {
			if (typeList.resolve(0) || null == marshallPlan) {
				marshallPlan = (MarshallPlanX86) typeList
						.makeParamsMarshallPlan(true, false);
			}
		} catch (ProxyResolveException e) {
			e.setMemberType("parameter");
			e.setParentName(valueName());
			throw new JSDIException(e);
		}
		return marshallPlan;
	}

	//
	// ANCESTOR CLASS: Callback
	//

	@Override
	public long invoke(SuValue boundValue, Object argsIn) {
		final MarshallerX86 marshaller = marshallPlan.makeUnMarshaller((int[])argsIn);
		final Object[] argsOut = typeList.marshallOutParams(marshaller);
		final Object result = boundValue.call(argsOut);
		return toLong(result);
	}

	@Override
	public long invokeVariableIndirect(SuValue boundValue, Object argsIn,
			Object[] viArray) {
		int[] viInstArray = new int[viArray.length];
		Arrays.fill(viInstArray,
				VariableIndirectInstruction.RETURN_JAVA_STRING.ordinal());
		Marshaller marshaller = marshallPlan.makeUnMarshaller((int[]) argsIn,
				viArray, viInstArray);
		Object[] argsOut = typeList.marshallOutParams(marshaller);
		Object result = boundValue.call(argsOut);
		return toLong(result);
	}
}
