/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import java.util.Arrays;

import suneido.SuValue;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.Marshaller;
import suneido.language.jsdi.ThunkManager;
import suneido.language.jsdi.VariableIndirectInstruction;
import suneido.language.jsdi.type.Callback;
import suneido.language.jsdi.type.ProxyResolveException;
import suneido.language.jsdi.type.TypeList;

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
	public int invoke(SuValue boundValue, byte[] argsIn) {
		final MarshallerX86 marshaller = marshallPlan.makeUnMarshaller(argsIn);
		final Object[] argsOut = typeList.marshallOutParams(marshaller);
		final Object result = boundValue.call(argsOut);
		return toInt(result);
	}

	@Override
	public int invokeVariableIndirect(SuValue boundValue, byte[] argsIn,
			Object[] viArray) {
		int[] viInstArray = new int[viArray.length];
		Arrays.fill(viInstArray,
				VariableIndirectInstruction.RETURN_JAVA_STRING.ordinal());
		Marshaller marshaller = marshallPlan.makeUnMarshaller(argsIn, viArray,
				viInstArray);
		Object[] argsOut = typeList.marshallOutParams(marshaller);
		Object result = boundValue.call(argsOut);
		// FIXME: Return value will have to be 64-bit on x64 so it probably
		//        makes most sense just to return 64-bit on all platforms for
		//        simplicity...
		return toInt(result);
	}
}
