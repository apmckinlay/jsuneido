/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.DIRECT;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.INDIRECT;
import static suneido.jsdi.marshall.ReturnTypeGroup.DOUBLE;
import static suneido.jsdi.marshall.ReturnTypeGroup.INTEGER;
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
		this(storageCategory, returnTypeGroup, -1, hasFpParams,
				is32BitIEEEFloatReturn, false);
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
			throw SuInternalError.unhandledEnum(NativeCall64.class);
		}
	}

	//
	// STATICS
	//

	
	public static final int MAX_LONGMARSHALL_PARAMS = 4;

	private static final int MAX_NUMPARAMS_VALUE = MAX_LONGMARSHALL_PARAMS + 1;

	private static final int rtIndex(ReturnTypeGroup rtg, boolean hasFpParams,
			boolean is32BitIEEEFloatReturn) {
		int result = 0;
		switch (rtg) {
		case INTEGER:
			assert !is32BitIEEEFloatReturn;
			break;
		case DOUBLE:
			result = is32BitIEEEFloatReturn ? 1 : 2;
			break;
		case VARIABLE_INDIRECT:
			assert !is32BitIEEEFloatReturn;
			result = 3;
		}
		if (hasFpParams) {
			result += 4;
		}
		return result;
	}

	// map[rtIndex 0..7][StorageCategory][numParams 0..5]
	private static final NativeCall64[][][] map;
	static {
		final ReturnTypeGroup[] rt = ReturnTypeGroup.values();
		final boolean B[] = { false, true };
		final StorageCategory[] sc = StorageCategory.values();
		final NativeCall64[] nc = NativeCall64.values();
		final int N_sc = sc.length;
		map = new NativeCall64[4][N_sc][MAX_NUMPARAMS_VALUE + 1];
		// Put the generic values
		for (final ReturnTypeGroup rtg : rt) {
			for (final boolean hasFpParams : B) {
				for (final boolean is32BitIEEEFloatReturn : B) {
					for (int i = 0; i < N_sc; ++i) {
						inner: for (final NativeCall64 n : nc) {
							if (rtg == n.returnTypeGroup
									&& is32BitIEEEFloatReturn == n.is32BitIEEEFloatReturn
									&& hasFpParams == n.hasFpParams
									&& sc[i] == n.storageCategory) {
								for (int j = 0; j <= MAX_NUMPARAMS_VALUE; ++j) {
									map[rtIndex(rtg, hasFpParams, is32BitIEEEFloatReturn)][i][j] = n;
								}
								break inner;
							}
						}
					}

				}
			}
		}
		// Put the specific fastcall values
		final int SC_DIRECT = DIRECT.ordinal();
		map[0][SC_DIRECT][0] = J0_RETURN_INT64;
		map[0][SC_DIRECT][1] = J1_RETURN_INT64;
		map[0][SC_DIRECT][2] = J2_RETURN_INT64;
		map[0][SC_DIRECT][3] = J3_RETURN_INT64;
		map[0][SC_DIRECT][4] = J4_RETURN_INT64;
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
			boolean hasFpParams, boolean is32BitIEEEFloatReturn) {
		assert 0 <= numParams;
		numParams = Math.max(numParams, MAX_NUMPARAMS_VALUE);
		return map[rtIndex(returnTypeGroup, hasFpParams, is32BitIEEEFloatReturn)][storageCategory
				.ordinal()][numParams];
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
