package suneido.language.jsdi.dll;

import static suneido.language.jsdi.dll.CallGroup.*;
import static suneido.language.jsdi.dll.ReturnTypeGroup.*;

import java.util.ArrayList;

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

	RETURN_V(FAST, VOID, 0),
	L_RETURN_V(FAST, VOID, 1),
	LL_RETURN_V(FAST, VOID, 2),
	LLL_RETURN_V(FAST, VOID, 3),
	RETURN_L(FAST, _32_BIT, 0),
	L_RETURN_L(FAST, _32_BIT, 1),
	LL_RETURN_L(FAST, _32_BIT, 2),
	LLL_RETURN_L(FAST, _32_BIT, 3),
	DIRECT_ONLY_RETURN_V(DIRECT, VOID, -1),
	DIRECT_ONLY_RETURN_32_BIT(DIRECT, _32_BIT, -1),
	DIRECT_ONLY_RETURN_64_BIT(DIRECT, _64_BIT, -1),
	INDIRECT_RETURN_V(INDIRECT, VOID, -1),
	INDIRECT_RETURN_32_BIT(INDIRECT, _32_BIT, -1),
	INDIRECT_RETURN_64_BIT(INDIRECT, _64_BIT, -1),
	VARIABLE_INDIRECT_RETURN_V(VARIABLE_INDIRECT, VOID, -1),
	VARIABLE_INDIRECT_RETURN_32_BIT(VARIABLE_INDIRECT, _32_BIT, -1),
	VARIABLE_INDIRECT_RETURN_64_BIT(VARIABLE_INDIRECT, _64_BIT, -1);

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
		assert (FAST == callGroup && 0 <= numLongs) ||
		       (FAST != callGroup && -1 == numLongs);
		this.callGroup = callGroup;
		this.returnTypeGroup = returnTypeGroup;
		this.numLongs = numLongs;
	}

	//
	// ACCESSORS
	//

	public long invoke(long funcPtr, int sizeDirect, Marshaller marshaller) {
		switch (this) {
		case RETURN_V:     // fall through
		case L_RETURN_V:   // fall through
		case LL_RETURN_V:  // fall through
		case LLL_RETURN_V: // fall through
		case DIRECT_ONLY_RETURN_V:
			callDirectOnlyReturnV(funcPtr, sizeDirect, marshaller.getData());
			return 0L;
		case RETURN_L:     // fall through
		case L_RETURN_L:   // fall through
		case LL_RETURN_L:  // fall through
		case LLL_RETURN_L: // fall through
		case DIRECT_ONLY_RETURN_32_BIT:
			return (long) callDirectOnlyReturn32bit(funcPtr, sizeDirect,
					marshaller.getData());
		case DIRECT_ONLY_RETURN_64_BIT:
			return callDirectOnlyReturn64bit(funcPtr, sizeDirect,
					marshaller.getData());
		case INDIRECT_RETURN_V:
			callIndirectReturnV(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData());
			return 0L;
		case INDIRECT_RETURN_32_BIT:
			return (long) callIndirectReturn32bit(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData());
		case INDIRECT_RETURN_64_BIT:
			return (long) callIndirectReturn64bit(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData());
		case VARIABLE_INDIRECT_RETURN_V:
			callVariableIndirectReturnV(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case VARIABLE_INDIRECT_RETURN_32_BIT:
			return (long) callVariableIndirectReturn32bit(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData(),
					marshaller.getViArray(), marshaller.getViInstArray());
		case VARIABLE_INDIRECT_RETURN_64_BIT:
			return callVariableIndirectReturn64bit(funcPtr, sizeDirect,
					marshaller.getPtrArray(), marshaller.getData(),
					marshaller.getViArray(), marshaller.getViInstArray());
		default:
			throw new IllegalStateException("unhandled NativeCall type in switch");
		}
	}

	//
	// STATICS
	//

	private static final NativeCall[][] map;
	static {
		final CallGroup[] cg = CallGroup.values();
		final NativeCall[] nc = NativeCall.values();
		final int N_cg = cg.length;
		final int N_nc = nc.length;
		map = new NativeCall[N_cg][];
		ArrayList<NativeCall> calls = new ArrayList<NativeCall>();
		for (int i = 0; i < N_cg; ++i) {
			calls.clear();
			for (int j = 0; j < N_nc; ++j) {
				if (cg[i] == nc[j].callGroup)
					calls.add(nc[j]);
			}
			map[i] = calls.toArray(new NativeCall[calls.size()]);
		}
	}

	public static NativeCall get(CallGroup callGroup,
			ReturnTypeGroup returnTypeGroup, int numParams) {
		if (FAST == callGroup) {
			if (MAX_FAST_MARSHALL_PARAMS < numParams
					|| _64_BIT == returnTypeGroup) {
				callGroup = DIRECT;
				numParams = -1;
			}
		} else {
			numParams = -1;
		}
		for (NativeCall nc : map[callGroup.ordinal()]) {
			if (nc.returnTypeGroup == returnTypeGroup
					&& nc.numLongs == numParams)
				return nc;
		}
		assert false : "control should never pass here";
		return null;
	}

	//
	// NATIVE METHODS. These functions are available to specific instances of
	//     DllBase which are derived from this class.
	//

	static native void callReturnV(long funcPtr);

	static native void callLReturnV(long funcPtr, int arg0);

	static native void callLLReturnV(long funcPtr, int arg0, int arg1);

	static native void callLLLReturnV(long funcPtr, int arg0, int arg1, int arg2);

	static native int callReturnL(long funcPtr);

	static native int callLReturnL(long funcPtr, int arg0);

	static native int callLLReturnL(long funcPtr, int arg0, int arg1);

	static native int callLLLReturnL(long funcPtr, int arg0, int arg1, int arg2);

	private static native void callDirectOnlyReturnV(long funcPtr,
			int sizeDirect, byte[] args);

	private static native int callDirectOnlyReturn32bit(long funcPtr,
			int sizeDirect, byte[] args);

	private static native long callDirectOnlyReturn64bit(long funcPtr,
			int sizeDirect, byte[] args);

	private static native void callIndirectReturnV(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	private static native int callIndirectReturn32bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	private static native long callIndirectReturn64bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args);

	private static native void callVariableIndirectReturnV(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, Object[] viArray,
			boolean[] viInst);

	private static native int callVariableIndirectReturn32bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, Object[] viArray,
			boolean[] viInst);

	private static native long callVariableIndirectReturn64bit(long funcPtr,
			int sizeDirect, int[] ptrArray, byte[] args, Object[] viArray,
			boolean[] viInst);
}