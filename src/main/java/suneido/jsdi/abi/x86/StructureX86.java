/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.x86;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuContainer;
import suneido.jsdi.Buffer;
import suneido.jsdi.DllInterface;
import suneido.jsdi.MarshallPlan;
import suneido.jsdi.PrimitiveSize;
import suneido.jsdi.type.Structure;
import suneido.jsdi.type.TypeList;

/**
 * Structure logic customized for the x86 platform.
 * 
 * @author Victor Schappert
 * @since 20140719
 */
@DllInterface
final class StructureX86 extends Structure {

	//
	// CONSTRUCTORS
	//

	StructureX86(String valueName, TypeList members) {
		super(valueName, members);
	}

	//
	// ANCESTOR CLASS: Structure
	//

	@Override
	protected Object copyOut(long structAddr) {
		final MarshallPlanX86 p = (MarshallPlanX86) getMarshallPlan();
		final MarshallerX86 m = p.makeMarshallerX86();
		putMarshallOutInstruction(m);
		switch (p.getStorageCategory()) {
		case DIRECT: // intentional fall through
			copyOutDirect(structAddr, m.getData(), p.getSizeDirect());
			break;
		case INDIRECT:
			copyOutIndirect(structAddr, m.getData(), p.getSizeDirect(),
					m.getPtrArray());
			break;
		case VARIABLE_INDIRECT:
			copyOutVariableIndirect(structAddr, m.getData(), p.getSizeDirect(),
					m.getPtrArray(), m.getViArray(), m.getViInstArray());
			break;
		default:
			throw unhandledEnum(MarshallPlan.StorageCategory.class);
		}
		m.rewind();
		return typeList.marshallOutMembers(m, null);
	}

	@Override
	protected Buffer toBuffer(SuContainer value, MarshallPlan plan) {
		MarshallerX86 m = ((MarshallPlanX86) plan).makeMarshallerX86();
		typeList.marshallInMembers(m, value);
		final int N = plan.getSizeDirect();
		final Buffer result = new Buffer(N);
		new ByteCopierX86(m.getData(), 0, result.getInternalData())
				.copyFromIntArr(N);
		return result;
	}

	@Override
	protected Object fromBuffer(Buffer data, MarshallPlan plan) {
		final int N = plan.getSizeDirect();
		// TODO: For this case, it would be more efficient to make a dedicated
		//       byte[] marshaller in the normal jsdi package and use the
		//       Buffer's internal data they way we were doing before.
		final int[] intData = new int[PrimitiveSize.numWholeWords(N)];
		new ByteCopierX86(intData, 0, data.getInternalData()).copyToIntArr(N);
		MarshallerX86 m = ((MarshallPlanX86) plan).makeUnMarshaller(intData);
		return marshallOut(m, null);
	}

	//
	// NATIVE METHODS
	//

	private static native void copyOutDirect(long structAddr, int[] data,
			int sizeDirect);

	private static native void copyOutIndirect(long structAddr, int[] data,
			int sizeDirect, int[] ptrArray);

	private static native void copyOutVariableIndirect(long structAddr,
			int[] data, int sizeDirect, int[] ptrArray, Object[] viArray,
			int[] viInstArray);
}
