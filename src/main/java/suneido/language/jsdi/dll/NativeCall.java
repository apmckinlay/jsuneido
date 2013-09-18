/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.dll;

import static suneido.language.jsdi.dll.CallGroup.DIRECT;
import static suneido.language.jsdi.dll.CallGroup.FAST;
import static suneido.language.jsdi.dll.CallGroup.INDIRECT;
import static suneido.language.jsdi.dll.CallGroup.VARIABLE_INDIRECT;
import static suneido.language.jsdi.dll.ReturnTypeGroup.DOUBLE;
import static suneido.language.jsdi.dll.ReturnTypeGroup.INTEGER;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Marshaller;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130717
 *
 */
@DllInterface
enum NativeCall {

	//
	// ENUMERATORS
	//

	RETURN_INT64(FAST, INTEGER, 0),
	L_RETURN_INT64(FAST, INTEGER, 1),
	LL_RETURN_INT64(FAST, INTEGER, 2),
	LLL_RETURN_INT64(FAST, INTEGER, 3),
	DIRECT_RETURN_INT64(DIRECT, INTEGER, 0),
	DIRECT_RETURN_DOUBLE(DIRECT, DOUBLE, 0),
	DIRECT_RETURN_VARIABLE_INDIRECT(
			DIRECT, ReturnTypeGroup.VARIABLE_INDIRECT, 0),
	INDIRECT_RETURN_INT64(INDIRECT, INTEGER, 0),
	INDIRECT_RETURN_DOUBLE(INDIRECT, DOUBLE, 0),
	INDIRECT_RETURN_VARIABLE_INDIRECT(
			INDIRECT, ReturnTypeGroup.VARIABLE_INDIRECT, 0),
	VARIABLE_INDIRECT_RETURN_INT64(VARIABLE_INDIRECT, INTEGER, 0),
	VARIABLE_INDIRECT_RETURN_DOUBLE(VARIABLE_INDIRECT, DOUBLE, 0),
	VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT(
				VARIABLE_INDIRECT, ReturnTypeGroup.VARIABLE_INDIRECT, 0);

	//
	// PUBLIC CONSTANTS
	//

	public static final int MAX_FAST_MARSHALL_PARAMS = 3;

	//
	// DATA
	//

	private final CallGroup callGroup;
	private final ReturnTypeGroup returnTypeGroup;
	private final int numLongs;

	//
	// CONSTRUCTORS
	//

	private NativeCall(CallGroup callGroup, ReturnTypeGroup returnTypeGroup,
			int numLongs) {
		assert (FAST == callGroup && 0 <= numLongs && numLongs <= MAX_FAST_MARSHALL_PARAMS)
				|| (FAST != callGroup && 0 == numLongs);
		this.callGroup = callGroup;
		this.returnTypeGroup = returnTypeGroup;
		this.numLongs = numLongs;
	}

	//
	// ACCESSORS
	//

	public boolean isDirectOrFast() {
		return DIRECT == callGroup || FAST == callGroup;
	}

	// TODO: docs -- since 20130808
	public boolean isFloatingPointReturn() {
		return DOUBLE == returnTypeGroup;
	}

	public CallGroup getCallGroup() {
		return callGroup;
	}

	public ReturnTypeGroup getReturnTypeGroup() {
		return returnTypeGroup;
	}

	public long invoke(long funcPtr, int sizeDirect, Marshaller marshaller) {
		switch (this) {
		case RETURN_INT64:              // fall through
		case L_RETURN_INT64:            // fall through
		case LL_RETURN_INT64:           // fall through
		case LLL_RETURN_INT64:          // fall through
		case DIRECT_RETURN_INT64:
			return callDirectReturnInt64(funcPtr, sizeDirect, marshaller.getData());
		case DIRECT_RETURN_DOUBLE:
			return callDirectReturnDouble(funcPtr, sizeDirect,
					marshaller.getData());
		case INDIRECT_RETURN_INT64:
			return callIndirectReturnInt64(funcPtr, sizeDirect, marshaller.getData(),
					marshaller.getPtrArray());
		case INDIRECT_RETURN_DOUBLE:
			return callDirectReturnDouble(funcPtr, sizeDirect,
					marshaller.getData());
		case VARIABLE_INDIRECT_RETURN_INT64:
			return callVariableIndirectReturnInt64(funcPtr, sizeDirect,
					marshaller.getData(), marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case VARIABLE_INDIRECT_RETURN_DOUBLE:
			return callVariableIndirectReturnDouble(funcPtr, sizeDirect,
					marshaller.getData(), marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case DIRECT_RETURN_VARIABLE_INDIRECT:             // fall through
		case INDIRECT_RETURN_VARIABLE_INDIRECT:           // fall through
		case VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT:
			callVariableIndirectReturnVariableIndirect(funcPtr, sizeDirect,
					marshaller.getData(), marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
			return 0L;
		default:
			throw new IllegalStateException("unhandled NativeCall type in switch");
		}
	}

	//
	// STATICS
	//

	// map[CallGroup][ReturnTypeGroup][nLongs]
	private static final NativeCall[][][] map;
	static {
		final CallGroup[] cg = CallGroup.values();
		final ReturnTypeGroup[] rt = ReturnTypeGroup.values();
		final NativeCall[] nc = NativeCall.values();
		final int N_cg = cg.length;
		final int N_rt = rt.length;
		map = new NativeCall[N_cg][][];
		for (int i = 0; i < N_cg; ++i) {
			map[i] = new NativeCall[N_rt][];
			for (int j = 0; j < N_rt; ++j) {
				map[i][j] = new NativeCall[MAX_FAST_MARSHALL_PARAMS + 1];
				for (int k = 0; k <= MAX_FAST_MARSHALL_PARAMS; ++k) {
					inner: for (NativeCall n : nc) {
						if (cg[i] == n.callGroup &&
							rt[j] == n.returnTypeGroup &&
							k == n.numLongs) {
							map[i][j][k] = n;
							break inner;
						}
					}
				}
			}
		}
	}

	public static NativeCall get(CallGroup callGroup,
			ReturnTypeGroup returnTypeGroup, int numParams) {
		if (FAST == callGroup) {
			if (MAX_FAST_MARSHALL_PARAMS < numParams
					|| INTEGER != returnTypeGroup) {
				callGroup = DIRECT;
				numParams = 0;
			}
		} else {
			numParams = 0;
		}
		return map[callGroup.ordinal()][returnTypeGroup.ordinal()][numParams];
	}

	//
	// NATIVE METHODS. These functions are available to specific instances of
	//     DllBase which are derived from this class.
	//

	static native long callReturnInt64(long funcPtr);

	static native long callLReturnInt64(long funcPtr, int arg0);

	static native long callLLReturnInt64(long funcPtr, int arg0, int arg1);

	static native long callLLLReturnInt64(long funcPtr, int arg0, int arg1,
			int arg2);

	private static native long callDirectReturnInt64(long funcPtr,
			int sizeDirect, byte[] args);

	private static native long callIndirectReturnInt64(long funcPtr,
			int sizeDirect, byte[] args, int[] ptrArray);

	private static native long callVariableIndirectReturnInt64(long funcPtr,
			int sizeDirect, byte[] args, int[] ptrArray, Object[] viArray,
			int[] viInst);

	private static native long callDirectReturnDouble(long funcPtr,
			int sizeDirect, byte[] args);

	private static native long callIndirectReturnDouble(long funcPtr,
			int sizeDirect, byte[] args, int[] ptrArray);

	private static native long callVariableIndirectReturnDouble(long funcPtr,
			int sizeDirect, byte[] args, int[] ptrArray, Object[] viArray,
			int[] viInst);

	private static native void callVariableIndirectReturnVariableIndirect(
			long funcPtr, int sizeDirect, byte[] args, int[] ptrArray,
			Object[] viArray, int[] viInst);
}