/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import static suneido.SuInternalError.unhandledEnum;
import suneido.SuContainer;
import suneido.language.jsdi.Buffer;
import suneido.language.jsdi.CallGroup;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.MarshallPlan;
import suneido.language.jsdi.type.Structure;
import suneido.language.jsdi.type.TypeList;

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
		switch (getCallGroup()) {
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
			throw unhandledEnum(CallGroup.class);
		}
		m.rewind();
		return typeList.marshallOutMembers(m, null);
	}

	@Override
	protected Buffer toBuffer(SuContainer value, MarshallPlan plan) {
		MarshallerX86 m = ((MarshallPlanX86) plan).makeMarshallerX86();
		typeList.marshallInMembers(m, value);
		byte[] data = m.getData();
		return new Buffer(data, 0, data.length);
	}

	@Override
	protected Object fromBuffer(Buffer data, MarshallPlan plan) {
		MarshallerX86 m = ((MarshallPlanX86) plan).makeUnMarshaller(data
				.getInternalData());
		return marshallOut(m, null);
	}

	//
	// NATIVE METHODS
	//

	static native void copyOutDirect(long structAddr, byte[] data,
			int sizeDirect);

	static native void copyOutIndirect(long structAddr, byte[] data,
			int sizeDirect, int[] ptrArray);

	static native void copyOutVariableIndirect(long structAddr, byte[] data,
			int sizeDirect, int[] ptrArray, Object[] viArray, int[] viInstArray);
}
