/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.DIRECT;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.INDIRECT;
import static suneido.jsdi.marshall.ReturnTypeGroup.DOUBLE;
import static suneido.jsdi.marshall.ReturnTypeGroup.INTEGER;

import java.util.Arrays;

import suneido.SuInternalError;
import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan.StorageCategory;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.ReturnTypeGroup;

/**
 * Contains logic for describing and making {@code dll} calls on amd64.
 *
 * @author Victor Schappert
 * @since 20140730
 * @see suneido.jsdi.abi.x86.NativeCallX86
 * @see ParamRegisterType
 */
@DllInterface
enum NativeCall64 {

	J0_RETURN_INT64(0), J1_RETURN_INT64(1), J2_RETURN_INT64(2), J3_RETURN_INT64(
			3), J4_RETURN_INT64(4),

	DIRECT_NOFP_RETURN_INT64(DIRECT, INTEGER, false, false), DIRECT_NOFP_RETURN_FLOAT(
			DIRECT, DOUBLE, false, true), DIRECT_NOFP_RETURN_DOUBLE(DIRECT,
			DOUBLE, false, false), DIRECT_NOFP_RETURN_VARIABLE_INDIRECT(DIRECT,
			ReturnTypeGroup.VARIABLE_INDIRECT, false, false),

	DIRECT_RETURN_INT64(DIRECT, INTEGER, true, false), DIRECT_RETURN_FLOAT(
			DIRECT, DOUBLE, true, true), DIRECT_RETURN_DOUBLE(DIRECT, DOUBLE,
			true, false), DIRECT_RETURN_VARIABLE_INDIRECT(DIRECT,
			ReturnTypeGroup.VARIABLE_INDIRECT, true, false),

	INDIRECT_NOFP_RETURN_INT64(INDIRECT, INTEGER, false, false), INDIRECT_NOFP_RETURN_FLOAT(
			INDIRECT, DOUBLE, false, true), INDIRECT_NOFP_RETURN_DOUBLE(
			INDIRECT, DOUBLE, false, false), INDIRECT_NOFP_RETURN_VARIABLE_INDIRECT(
			INDIRECT, ReturnTypeGroup.VARIABLE_INDIRECT, false, false),

	INDIRECT_RETURN_INT64(INDIRECT, INTEGER, true, false), INDIRECT_RETURN_FLOAT(
			INDIRECT, DOUBLE, true, true), INDIRECT_RETURN_DOUBLE(INDIRECT,
			DOUBLE, true, false), INDIRECT_RETURN_VARIABLE_INDIRECT(INDIRECT,
			ReturnTypeGroup.VARIABLE_INDIRECT, true, false),

	VARIABLE_INDIRECT_NOFP_RETURN_INT64(StorageCategory.VARIABLE_INDIRECT,
			INTEGER, false, false), VARIABLE_INDIRECT_NOFP_RETURN_FLOAT(
			StorageCategory.VARIABLE_INDIRECT, DOUBLE, false, true), VARIABLE_INDIRECT_NOFP_RETURN_DOUBLE(
			StorageCategory.VARIABLE_INDIRECT, DOUBLE, false, false), VARIABLE_INDIRECT_NOFP_RETURN_VARIABLE_INDIRECT(
			StorageCategory.VARIABLE_INDIRECT,
			ReturnTypeGroup.VARIABLE_INDIRECT, false, false),

	VARIABLE_INDIRECT_RETURN_INT64(StorageCategory.VARIABLE_INDIRECT, INTEGER,
			true, false), VARIABLE_INDIRECT_RETURN_FLOAT(
			StorageCategory.VARIABLE_INDIRECT, DOUBLE, true, true), VARIABLE_INDIRECT_RETURN_DOUBLE(
			StorageCategory.VARIABLE_INDIRECT, DOUBLE, true, false), VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT(
			StorageCategory.VARIABLE_INDIRECT,
			ReturnTypeGroup.VARIABLE_INDIRECT, true, false);

	//
	// DATA
	//

	private final StorageCategory storageCategory;
	private final ReturnTypeGroup returnTypeGroup;
	private final int             numParams;
	private final boolean         hasFpParams;
	private final boolean         is32BitIEEEFloatReturn;
	private final boolean         isFastCallable;

	//
	// CONSTRUCTORS
	//

	private NativeCall64(StorageCategory storageCategory,
			ReturnTypeGroup returnTypeGroup, int numParams,
			boolean hasFpParams, boolean is32BitIEEEFloatReturn, boolean isFastCallable) {
		assert null != storageCategory;
		assert null != returnTypeGroup;
		assert 0 <= numParams && numParams <= MAX_NUMPARAMS_VALUE;
		this.storageCategory        = storageCategory;
		this.returnTypeGroup        = returnTypeGroup;
		this.numParams              = numParams;
		this.hasFpParams            = hasFpParams;
		this.is32BitIEEEFloatReturn = is32BitIEEEFloatReturn;
		this.isFastCallable         = isFastCallable;
	}

	private NativeCall64(int numParams) {
		this(StorageCategory.DIRECT, ReturnTypeGroup.INTEGER, numParams, false,
				false, true);
		assert numParams <= MAX_LONGMARSHALL_PARAMS;
	}

	private NativeCall64(StorageCategory storageCategory,
			ReturnTypeGroup returnTypeGroup, boolean hasFpParams,
			boolean is32BitIEEEFloatReturn) {
		this(storageCategory, returnTypeGroup, MAX_NUMPARAMS_VALUE,
				hasFpParams, is32BitIEEEFloatReturn, false);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns true iff this enumerator meets the criteria for fast calling.
	 *
	 * <p>
	 * The fast-calling criteria are:
	 * <ul>
	 * <li>four parameters or fewer;</li>
	 * <li>parameters and return value use direct storage only;</li>
	 * <li>all parameters can be marshalled into a 64-bit Java {@code long} value</li>
	 * <li>return value is can be marshalled into a 64-bit Java {@code long} value</li>
	 * </ul>
	 * </p>
	 * 
	 *
	 * @return Whether this native call can be fast-called
	 * @since 20140801
	 */
	public boolean isFastCallable() {
		return isFastCallable;
	}

	/**
	 * <p>
	 * Invokes a native function using an invocation method appropriate for this
	 * enumerator.
	 * </p>
	 *
	 * @param funcPtr
	 *            Address of the native function to call
	 * @param sizeDirect
	 *            Size, in bytes, of the marshalled data to pass as arguments
	 * @param registers
	 *            A value returned by
	 *            {@link ParamRegisterType#packFromParamTypes(suneido.jsdi.type.Type, suneido.jsdi.type.Type, suneido.jsdi.type.Type, suneido.jsdi.type.Type)}
	 * @param marshaller
	 *            Marshaller with all required data marshalled in
	 * @return Value returned from the invoked native function as a 64-bit
	 *         integer
	 * @since 20140730
	 */
	public long invoke(long funcPtr, int sizeDirect, int registers,
			Marshaller marshaller) {
		final long[] args = marshaller.getData();
		switch (this) {
		case J0_RETURN_INT64:
		case J1_RETURN_INT64:
		case J2_RETURN_INT64:
		case J3_RETURN_INT64:
		case J4_RETURN_INT64:
		case DIRECT_NOFP_RETURN_INT64:
			return callDirectNoFpReturnInt64(funcPtr, sizeDirect, args);
		case DIRECT_RETURN_INT64:
			return callDirectReturnInt64(funcPtr, sizeDirect, args, registers);
		case DIRECT_NOFP_RETURN_FLOAT:
		case DIRECT_RETURN_FLOAT:
			return callDirectReturnFloat(funcPtr, sizeDirect, args, registers);
		case DIRECT_NOFP_RETURN_DOUBLE:
		case DIRECT_RETURN_DOUBLE:
			return callDirectReturnDouble(funcPtr, sizeDirect, args, registers);
		case INDIRECT_NOFP_RETURN_INT64:
			return callIndirectNoFpReturnInt64(funcPtr, sizeDirect, args,
					marshaller.getPtrArray());
		case INDIRECT_RETURN_INT64:
			return callIndirectReturnInt64(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray());
		case INDIRECT_NOFP_RETURN_FLOAT:
		case INDIRECT_RETURN_FLOAT:
			return callIndirectReturnFloat(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray());
		case INDIRECT_NOFP_RETURN_DOUBLE:
		case INDIRECT_RETURN_DOUBLE:
			return callIndirectReturnDouble(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray());
		case VARIABLE_INDIRECT_NOFP_RETURN_INT64:
		case VARIABLE_INDIRECT_RETURN_INT64:
			return callVariableIndirectReturnInt64(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case VARIABLE_INDIRECT_NOFP_RETURN_FLOAT:
		case VARIABLE_INDIRECT_RETURN_FLOAT:
			return callVariableIndirectReturnFloat(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case VARIABLE_INDIRECT_NOFP_RETURN_DOUBLE:
		case VARIABLE_INDIRECT_RETURN_DOUBLE:
			return callVariableIndirectReturnDouble(funcPtr, sizeDirect, args,
					registers, marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case DIRECT_NOFP_RETURN_VARIABLE_INDIRECT:
		case DIRECT_RETURN_VARIABLE_INDIRECT:
		case INDIRECT_NOFP_RETURN_VARIABLE_INDIRECT:
		case INDIRECT_RETURN_VARIABLE_INDIRECT:
		case VARIABLE_INDIRECT_NOFP_RETURN_VARIABLE_INDIRECT:
		case VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT:
			callVariableIndirectReturnVariableIndirect(funcPtr, sizeDirect,
					args, registers, marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
			return 0L;
		default:
			throw SuInternalError.unhandledEnum(this);
		}
	}

	//
	// STATICS
	//

	
	public static final int MAX_LONGMARSHALL_PARAMS = 4;

	private static final int MAX_NUMPARAMS_VALUE = MAX_LONGMARSHALL_PARAMS + 1;

	private static final int rtIndex(ReturnTypeGroup rtg,
			boolean is32BitIEEEFloatReturn) {
		switch (rtg) {
		case INTEGER:
			assert !is32BitIEEEFloatReturn;
			return 0;
		case DOUBLE:
			return is32BitIEEEFloatReturn ? 2 : 1;
		case VARIABLE_INDIRECT:
			assert !is32BitIEEEFloatReturn;
			return 3;
		default:
			throw SuInternalError.unhandledEnum(rtg);
		}
	}

	private static final int scIndex(StorageCategory sc, boolean hasFpParams) {
		int index = sc.ordinal();
		return hasFpParams ? index + 3 : index; 
	}

	// fastCallMap[numParams:0..4]
	private static final NativeCall64[] fastCallMap;
	// ordinaryCallMap[rtIndex 0..3][scIndex 0..5]
	private static final NativeCall64[][] ordinaryCallMap;

	private static void initSCMap(StorageCategory[] SC, ReturnTypeGroup rtg,
			boolean is32BitIEEEFloatReturn, NativeCall64[] map) {
		final NativeCall64[] NC = values();
		for (final StorageCategory sc : SC) {
			inner: for (final boolean hasFpParams : new boolean[] { false, true }) {
				for (final NativeCall64 nc : NC) {
					if (nc.storageCategory == sc
							&& nc.hasFpParams == hasFpParams
							&& nc.is32BitIEEEFloatReturn == is32BitIEEEFloatReturn
							&& nc.returnTypeGroup == rtg
							&& nc.isFastCallable == false) {
						map[scIndex(sc, hasFpParams)] = nc;
						break inner;
					}
				}
			}
		}
	}
	
	static {
		// Fastcall map
		fastCallMap = new NativeCall64[] { J0_RETURN_INT64, J1_RETURN_INT64,
				J2_RETURN_INT64, J3_RETURN_INT64, J4_RETURN_INT64 };
		// Ordinary call map
		final ReturnTypeGroup[] RTG = ReturnTypeGroup.values();
		final StorageCategory[] SC = StorageCategory.values();
		ordinaryCallMap = new NativeCall64[RTG.length + 1][SC.length * 2];
		for (final NativeCall64 nc : values()) {
			if (nc.isFastCallable) continue;
			final int rtIndex = rtIndex(nc.returnTypeGroup, nc.is32BitIEEEFloatReturn);
			final int scIndex = scIndex(nc.storageCategory, nc.hasFpParams);
			ordinaryCallMap[rtIndex][scIndex] = nc;
		}
	}

	/**
	 * Returns the enumerator applicable for a native function having a given
	 * storage category, return type, and parameter profile.
	 *
	 * @param storageCategory
	 *            Storage category of the native function
	 * @param returnTypeGroup
	 *            Return type group of the native function
	 * @param numParams
	 *            Non-negative parameter count of the native function
	 * @param isLongMarshallable
	 *            Whether <em>all</em> parameters can be marshalled into and
	 *            out of a Java {@code long} value
	 * @param hasFpParams
	 *            Whether any of the native function's first for parameters are
	 *            {@code float} or {@code double}
	 * @param has32BitIEEEFloatReturn
	 *            If, and only if, the returnTypeGroup is
	 *            {@link ReturnTypeGroup#DOUBLE} but the true return type of the
	 *            native function is {@code float}, this parameter must be set
	 *            to {@code true}
	 * @return Applicable enumerator
	 * @since 20140730
	 */
	public static NativeCall64 get(StorageCategory storageCategory,
			ReturnTypeGroup returnTypeGroup, int numParams,
			boolean isLongMarshallable, boolean hasFpParams,
			boolean is32BitIEEEFloatReturn) {
		assert 0 <= numParams;
		if (numParams <= MAX_LONGMARSHALL_PARAMS && isLongMarshallable &&
			INTEGER == returnTypeGroup) {
			assert StorageCategory.DIRECT == storageCategory;
			assert ! hasFpParams;
			assert ! is32BitIEEEFloatReturn;
			return fastCallMap[numParams];
		} else {
			final int rtIndex = rtIndex(returnTypeGroup, is32BitIEEEFloatReturn);
			final int scIndex = scIndex(storageCategory, hasFpParams);
			return ordinaryCallMap[rtIndex][scIndex];
		}
	}

	//
	// TESTING
	//

	public static void main(String[] args) {
		for (int rtIndex = 0; rtIndex < 4; ++rtIndex) {
			System.out.println("rtindex " + rtIndex + "...");
			System.out.println("\t" + Arrays.toString(ordinaryCallMap[rtIndex]));
		}
	}

	//
	// NATIVE METHODS
	//

	static native long callJ0(long funcPtr);

	static native long callJ1(long funcPtr, long arg0);

	static native long callJ2(long funcPtr, long arg0, long arg1);

	static native long callJ3(long funcPtr, long arg0, long arg1, long arg2);

	static native long callJ4(long funcPtr, long arg0, long arg1, long arg2,
			long arg3);

	private static native long callDirectNoFpReturnInt64(long funcPtr,
			int sizeDirect, long[] args);

	private static native long callDirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int registers);

	private static native long callDirectReturnFloat(long funcPtr,
			int sizeDirect, long[] args, int registers);

	private static native long callDirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args, int registers);

	private static native long callIndirectNoFpReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int[] ptrArray);

	private static native long callIndirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray);

	private static native long callIndirectReturnFloat(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray);

	private static native long callIndirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray);

	private static native long callVariableIndirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray,
			Object[] viArray, int[] viInst);

	private static native long callVariableIndirectReturnFloat(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray,
			Object[] viArray, int[] viInst);

	private static native long callVariableIndirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args, int registers, int[] ptrArray,
			Object[] viArray, int[] viInst);

	private static native void callVariableIndirectReturnVariableIndirect(
			long funcPtr, int sizeDirect, long[] args, int registers,
			int[] ptrArray, Object[] viArray, int[] viInst);
}
