/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.abi.amd64;

import suneido.jsdi.Dll;
import suneido.jsdi.DllInterface;
import suneido.jsdi.Factory;
import suneido.jsdi.JSDI;
import suneido.jsdi.type.Callback;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;
import suneido.jsdi.type.TypeList.Args;

/**
 * Concrete factory for instantiating objects for the Windows x64 ABI.
 * 
 * @author Victor Schappert
 * @since 20140730
 */
@DllInterface
final class Factory64 extends Factory {

	//
	// DATA
	//

	private final DllFactory64 dllFactory;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a new amd64 factory.
	 * 
	 * @param jsdi
	 *            Non-NULL reference a JSDI instance.
	 */
	Factory64(JSDI jsdi) {
		super(jsdi);
		dllFactory = new DllFactory64(jsdi);
	}

	//
	// ANCESTOR CLASS: Factory
	//

	@Override
	public TypeList makeTypeList(Args args) {
		return args.isParams() ? new ParamsTypeList(args)
				: new TypeList64(args);
	}

	@Override
	public Callback makeCallback(String valueName, TypeList params) {
		return new Callback64(valueName, (ParamsTypeList) params,
				jsdi.getThunkManager());
	}

	@Override
	public Dll makeDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		return dllFactory.makeDll(suTypeName, libraryName, userFuncName,
				params, returnType);
	}
}
