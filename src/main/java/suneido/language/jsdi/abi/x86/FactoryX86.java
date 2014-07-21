/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi.abi.x86;

import suneido.language.jsdi.Dll;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.Factory;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.type.Callback;
import suneido.language.jsdi.type.Structure;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;
import suneido.language.jsdi.type.TypeList.Args;

/**
 * Concrete factory for instantiating objects for the Win32 x86
 * {@code __stdcall} calling convention.
 * 
 * @author Victor Schappert
 * @since 20140718
 */
@DllInterface
public final class FactoryX86 extends Factory {

	//
	// DATA
	//

	private final DllFactoryX86 dllFactory;

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs a new x86 factory.
	 * 
	 * @param jsdi
	 *            Non-NULL reference a JSDI instance.
	 */
	FactoryX86(JSDI jsdi) {
		super(jsdi);
		dllFactory = new DllFactoryX86(jsdi);
	}

	//
	// ANCESTOR CLASS: Factory
	//

	@Override
	public TypeList makeTypeList(Args args) {
		return new TypeListX86(args);
	}

	@Override
	public Structure makeStruct(String valueName, TypeList members) {
		return new StructureX86(valueName, members);
	}

	@Override
	public Callback makeCallback(String valueName, TypeList params) {
		return new CallbackX86(valueName, params, jsdi.getThunkManager());
	}

	@Override
	public Dll makeDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		return dllFactory.makeDll(suTypeName, libraryName, userFuncName,
				params, returnType);
	}
}
