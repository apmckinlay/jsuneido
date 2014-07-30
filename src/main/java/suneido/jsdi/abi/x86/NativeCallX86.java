/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static suneido.SuInternalError.unhandledEnum;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.DIRECT;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.INDIRECT;
import static suneido.jsdi.marshall.MarshallPlan.StorageCategory.VARIABLE_INDIRECT;
import static suneido.jsdi.marshall.ReturnTypeGroup.DOUBLE;
import static suneido.jsdi.marshall.ReturnTypeGroup.INTEGER;
import suneido.jsdi.DllInterface;
import suneido.jsdi.marshall.MarshallPlan;
import suneido.jsdi.marshall.Marshaller;
import suneido.jsdi.marshall.ReturnTypeGroup;

/**
 * Contains logic for describing and making {@code dll} calls on x86.
 *
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
enum NativeCallX86 {

	//
	// ENUMERATORS
	//

	DIRECT_RETURN_INT64(DIRECT, INTEGER), DIRECT_RETURN_DOUBLE(DIRECT, DOUBLE), DIRECT_RETURN_VARIABLE_INDIRECT(
			DIRECT, ReturnTypeGroup.VARIABLE_INDIRECT), INDIRECT_RETURN_INT64(
			INDIRECT, INTEGER), INDIRECT_RETURN_DOUBLE(INDIRECT, DOUBLE), INDIRECT_RETURN_VARIABLE_INDIRECT(
			INDIRECT, ReturnTypeGroup.VARIABLE_INDIRECT), VARIABLE_INDIRECT_RETURN_INT64(
			VARIABLE_INDIRECT, INTEGER), VARIABLE_INDIRECT_RETURN_DOUBLE(
			VARIABLE_INDIRECT, DOUBLE), VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT(
			VARIABLE_INDIRECT, ReturnTypeGroup.VARIABLE_INDIRECT);

	//
	// DATA
	//

	private final StorageCategory storageCategory;
	private final ReturnTypeGroup returnTypeGroup;

	//
	// CONSTRUCTORS
	//

	private NativeCallX86(StorageCategory storageCategory,
			ReturnTypeGroup returnTypeGroup) {
		this.storageCategory = storageCategory;
		this.returnTypeGroup = returnTypeGroup;
	}

	//
	// ACCESSORS
	//

	// TODO: docs -- since 20130808
	public boolean isFloatingPointReturn() {
		return DOUBLE == returnTypeGroup;
	}

	public StorageCategory getStorageCategory() {
		return storageCategory;
	}

	public ReturnTypeGroup getReturnTypeGroup() {
		return returnTypeGroup;
	}

	public long invoke(long funcPtr, int sizeDirect, Marshaller marshaller) {
		switch (this) {
		case DIRECT_RETURN_INT64:
			return callDirectReturnInt64(funcPtr, sizeDirect,
					marshaller.getData());
		case DIRECT_RETURN_DOUBLE:
			return callDirectReturnDouble(funcPtr, sizeDirect,
					marshaller.getData());
		case INDIRECT_RETURN_INT64:
			return callIndirectReturnInt64(funcPtr, sizeDirect,
					marshaller.getData(), marshaller.getPtrArray());
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
		case DIRECT_RETURN_VARIABLE_INDIRECT: // fall through
		case INDIRECT_RETURN_VARIABLE_INDIRECT: // fall through
		case VARIABLE_INDIRECT_RETURN_VARIABLE_INDIRECT:
			callVariableIndirectReturnVariableIndirect(funcPtr, sizeDirect,
					marshaller.getData(), marshaller.getPtrArray(),
					marshaller.getViArray(), marshaller.getViInstArray());
			return 0L;
		default:
			throw unhandledEnum(NativeCallX86.class);
		}
	}

	//
	// STATICS
	//

	// map[CallGroup][ReturnTypeGroup]
	private static final NativeCallX86[][] map;
	static {
		final StorageCategory[] sc = StorageCategory.values();
		final ReturnTypeGroup[] rt = ReturnTypeGroup.values();
		final NativeCallX86[] nc = NativeCallX86.values();
		final int N_cg = sc.length;
		final int N_rt = rt.length;
		map = new NativeCallX86[N_cg][];
		for (int i = 0; i < N_cg; ++i) {
			map[i] = new NativeCallX86[N_rt];
			for (int j = 0; j < N_rt; ++j) {
				inner: for (NativeCallX86 n : nc) {
					if (sc[i] == n.storageCategory
							&& rt[j] == n.returnTypeGroup) {
						map[i][j] = n;
						break inner;
					}
				}
			}
		}
	}

	public static NativeCallX86 get(
			MarshallPlan.StorageCategory storageCategory,
			ReturnTypeGroup returnTypeGroup) {
		return map[storageCategory.ordinal()][returnTypeGroup.ordinal()];
	}

	//
	// NATIVE METHODS. These functions are available to specific instances of
	// DllBase which are derived from this class.
	//

	private static native long callDirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args);

	private static native long callIndirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int[] ptrArray);

	private static native long callVariableIndirectReturnInt64(long funcPtr,
			int sizeDirect, long[] args, int[] ptrArray, Object[] viArray,
			int[] viInst);

	private static native long callDirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args);

	private static native long callIndirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args, int[] ptrArray);

	private static native long callVariableIndirectReturnDouble(long funcPtr,
			int sizeDirect, long[] args, int[] ptrArray, Object[] viArray,
			int[] viInst);

	private static native void callVariableIndirectReturnVariableIndirect(
			long funcPtr, int sizeDirect, long[] args, int[] ptrArray,
			Object[] viArray, int[] viInst);
}