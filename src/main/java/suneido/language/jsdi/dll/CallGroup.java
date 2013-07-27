package suneido.language.jsdi.dll;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.type.TypeList;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
enum CallGroup {

	FAST,
	DIRECT,
	INDIRECT,
	VARIABLE_INDIRECT;

	public static CallGroup fromTypeList(TypeList typeList) {
		return fromTypeList(typeList, false);
	}

	public static CallGroup fromTypeList(TypeList typeList, boolean resolved) {
		if (typeList.isFastMarshallable() &&
				typeList.size() <= NativeCall.MAX_FAST_MARSHALL_PARAMS) {
			return FAST;
		} else if (typeList.isClosed() || resolved) {
			if (0 < typeList.getVariableIndirectCount())
				return VARIABLE_INDIRECT;
			else if (0 < typeList.getSizeIndirect())
				return INDIRECT;
			else
				return DIRECT;
		} else {
			return null;
		}
	}
}
