/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.DllInterface;
import suneido.jsdi.ThunkManager;
import suneido.jsdi.type.Callback;

/**
 * Minor customization of callback type for AMD64: exposes a type list directly
 * to other package code, in particular to {@link ThunkManager64}.
 *
 * @author Victor Schappert
 * @since 20140801
 */
@DllInterface
final class Callback64 extends Callback {

	//
	// DATA
	//

	final ParamsTypeList params;

	//
	// CONSTRUCTORS
	//

	public Callback64(String valueName, ParamsTypeList parameters,
			ThunkManager thunkManager) {
		super(valueName, parameters, thunkManager);
		this.params = parameters;
	}
}
