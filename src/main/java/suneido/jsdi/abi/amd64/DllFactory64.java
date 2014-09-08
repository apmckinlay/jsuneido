/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuInternalError;
import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.StorageType;
import suneido.jsdi.marshall.MarshallPlan.StorageCategory;
import suneido.jsdi.marshall.ReturnTypeGroup;
import suneido.jsdi.type.BasicType;
import suneido.jsdi.type.BasicValue;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeId;
import suneido.jsdi.type.TypeList;

/**
 * Dll factory specialized to create amd64 {@code dll} values.
 *
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class DllFactory64 extends DllFactory {

	//
	// CONSTRUCTORS
	//

	DllFactory64(JSDI jsdi) {
		super(jsdi);
	}

	//
	// ANCESTOR CLASS DllFactory
	//

	@Override
	protected Dll makeRealDll(String libraryName, String userFuncName,
			TypeList params, Type returnType) {
		ParamsTypeList ptl = (ParamsTypeList)params;
		ReturnTypeGroup rtg = ReturnTypeGroup.fromType(returnType);
		long funcPtr = getFuncPtr(libraryName, userFuncName);
		boolean isFloatReturn = is32BitIEEEFloat(returnType);
		MarshallPlan64 paramsPlan = null;
		NativeCall64 nc = null;
		if (params.isClosed()) {
			paramsPlan = (MarshallPlan64) params.makeParamsMarshallPlan(false,
					0 < returnType.getVariableIndirectCount());
			StorageCategory sc = paramsPlan.getStorageCategory();
			nc = NativeCall64.get(sc, rtg, ptl.size(),
					ptl.isMarshallableToLong(), ptl.needsFpRegister(),
					isFloatReturn);
			if (jsdi.isFastMode() && nc.isFastCallable()) {
				return makeFastDll(nc, funcPtr, ptl, returnType, libraryName,
						userFuncName);
			}
		}
		return new GenericDll(funcPtr, ptl, returnType, rtg, isFloatReturn, nc,
				this, libraryName, userFuncName, paramsPlan);
	}

	//
	// INTERNALS
	//

	private static boolean is32BitIEEEFloat(Type type) {
		return
			StorageType.VALUE == type.getStorageType() &&
			TypeId.BASIC == type.getTypeId() &&
			BasicType.FLOAT == ((BasicValue)type).getBasicType();
	}

	private Dll makeFastDll(NativeCall64 nc, long funcPtr,
			ParamsTypeList params, Type returnType, String libraryName,
			String userFuncName) {
		switch (nc) {
		case J0_RETURN_INT64:
			return new FastDll0(funcPtr, params, returnType, this, libraryName,
					userFuncName);
		case J1_RETURN_INT64:
			return new FastDll1(funcPtr, params, returnType, this, libraryName,
					userFuncName);
		case J2_RETURN_INT64:
			return new FastDll2(funcPtr, params, returnType, this, libraryName,
					userFuncName);
		case J3_RETURN_INT64:
			return new FastDll3(funcPtr, params, returnType, this, libraryName,
					userFuncName);
		case J4_RETURN_INT64:
			return new FastDll4(funcPtr, params, returnType, this, libraryName,
					userFuncName);
		default:
			throw SuInternalError.unhandledEnum(nc);
		}
	}
}
