/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import suneido.language.Args;
import suneido.language.jsdi.CallGroup;
import suneido.language.jsdi.Dll;
import suneido.language.jsdi.DllFactory;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.ReturnTypeGroup;
import suneido.language.jsdi.type.InOutString;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;

/**
 * Concrete implementation of {@link Dll} for the Windows x86 platform.
 * 
 * @author Victor Schappert
 * @since 20140718
 */
@DllInterface
final class DllX86 extends Dll {

	//
	// DATA
	//

	private final NativeCallX86   nativeCall;      // null if params isn't closed
	private final ReturnTypeGroup returnTypeGroup;
	private MarshallPlanX86       marshallPlan;    // may change, may be null

	//
	// CONSTRUCTORS
	//

	protected DllX86(long funcPtr, TypeList params, Type returnType,
			ReturnTypeGroup returnTypeGroup, NativeCallX86 nc, String valueName,
			DllFactory dllFactory, String libraryName, String funcName) {
		super(funcPtr, params, returnType, valueName, dllFactory, libraryName,
				funcName);
		assert null != returnTypeGroup;
		this.returnTypeGroup = returnTypeGroup;
		this.nativeCall = nc;
		this.marshallPlan = null;
	}

	//
	// INTERNALS
	//


	private MarshallPlanX86 getMarshallPlan() {
		// FIXME: resolve: thread safety
		//        I think the main issue is thread (A) could store plan (A)
		//        and thread (B) could "concurrently" store plan (B) in such a
		//        way that thread (A) reads back plan (B). Is this a problem?
		if (resolve() || null == marshallPlan) {
			marshallPlan = (MarshallPlanX86) dllParams.makeParamsMarshallPlan(
					false, InOutString.INSTANCE == returnType);
		}
		return marshallPlan;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		args = Args.massage(super.params, args);
		final MarshallPlanX86 plan = getMarshallPlan();
		final MarshallerX86 m = plan.makeMarshallerX86();
		dllParams.marshallInParams(m, args);
		returnType.marshallInReturnValue(m);
		NativeCallX86 nc = null == nativeCall ? NativeCallX86.get(
				CallGroup.fromTypeList(dllParams, true), returnTypeGroup)
				: nativeCall;
		long returnValueRaw = nc.invoke(funcPtr, plan.getSizeDirect(), m);
		m.rewind();
		dllParams.marshallOutParams(m, args);
		return returnType.marshallOutReturnValue(returnValueRaw, m);
	}
}
