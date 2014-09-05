/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.ReturnTypeGroup;
import suneido.jsdi.type.Type;
import suneido.runtime.Args;

/**
 * Generic implementation of {@code dll}.
 *
 * @author Victor Schappert
 * @since 20140801
 */
@DllInterface
final class GenericDll extends Dll {

	//
	// DATA
	//

	private final ParamsTypeList  params;
	private final NativeCall64    nativeCall;       // null if params isn't closed
	private final ReturnTypeGroup returnTypeGroup;
	private final boolean         is32BitIEEEFloatReturn;
	private MarshallPlan64        marshallPlan;     // may change, may be null

	//
	// CONSTRUCTORS
	//

	protected GenericDll(long funcPtr, ParamsTypeList params, Type returnType,
			ReturnTypeGroup returnTypeGroup, boolean is32BitIEEEFloatReturn,
			NativeCall64 nc, String valueName, DllFactory dllFactory,
			String libraryName, String funcName, MarshallPlan64 plan) {
		super(funcPtr, params, returnType, valueName, dllFactory, libraryName, funcName);
		assert null != returnTypeGroup;
		this.params = params;
		this.nativeCall = nc;
		this.returnTypeGroup = returnTypeGroup;
		this.is32BitIEEEFloatReturn = is32BitIEEEFloatReturn;
		this.marshallPlan = plan;
	}

	//
	// INTERNALS
	//

	private MarshallPlan64 getMarshallPlan() {
		if (bind() || null == marshallPlan) {
			marshallPlan = (MarshallPlan64) params.makeParamsMarshallPlan(
				false, ReturnTypeGroup.VARIABLE_INDIRECT == returnTypeGroup);
		}
		return marshallPlan;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		args = Args.massage(super.params, args);
		final MarshallPlan plan = getMarshallPlan();
		final Marshaller m = plan.makeMarshaller();
		params.marshallInParams(m, args);
		returnType.marshallInReturnValue(m);
		final NativeCall64 nc = null == nativeCall ? NativeCall64.get(
				plan.getStorageCategory(), returnTypeGroup, params.size(),
				false, params.needsFpRegister(), is32BitIEEEFloatReturn)
				: nativeCall;
		final long returnValueRaw = nc.invoke(funcPtr, plan.getSizeDirect(),
				params.getRegisterUsage(), m);
		m.rewind();
		params.marshallOutParams(m, args);
		return returnType.marshallOutReturnValue(returnValueRaw, m);
	}
}
