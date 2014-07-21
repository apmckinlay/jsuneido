/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import suneido.language.jsdi.type.TypeList;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130717
 */
@DllInterface
public enum CallGroup {

	DIRECT,
	INDIRECT,
	VARIABLE_INDIRECT;

	public static CallGroup fromTypeList(TypeList typeList) {
		return fromTypeList(typeList, false);
	}

	public static CallGroup fromTypeList(TypeList typeList, boolean resolved) {
		if (typeList.isClosed() || resolved) {
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
