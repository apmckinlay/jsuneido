/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.SuInternalError;
import suneido.jsdi.Dll;
import suneido.jsdi.DllFactory;
import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDI;
import suneido.jsdi.type.Type;
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
	protected Dll makeRealDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		throw new SuInternalError("not implemented"); // TODO: implement me
	}
}
