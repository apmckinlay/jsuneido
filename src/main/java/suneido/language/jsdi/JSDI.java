/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import java.io.File;

import suneido.language.jsdi.dll.DllFactory;
import suneido.language.jsdi.type.TypeFactory;

/**
 * <p>
 * Class responsible for instantiating all JSDI objects which are available
 * outside of the package.
 * </p>
 * <p>
 * This class fulfills three functions. First, it constructs JSDI objects.
 * Second, it ensures that the JSDI DLL is loaded. Third, it ensures that the
 * JSDI DLL is correctly initialized.
 * </p> 
 * @author Victor Schappert
 * @since 20130624 
 */
@DllInterface
public final class JSDI {

	//
	// SINGLETON
	//

	private static final JSDI instance;
	public static JSDI getInstance()
	{
		return instance;
	}

	//
	// DLL INITIALIZATION
	//

	private static native void init();
	static
	{
		File path = new File("lib\\jsdi.dll");
		System.load(path.getAbsolutePath());
		// TODO: Figure out how JNA picks the correct DLL right out of the JAR
		//       and do that.
		init();
		instance = new JSDI();
	}

	//
	// DATA and CONSTRUCTORS
	//

	private final String       whenBuilt;   // when the native DLL was built
	private final TypeFactory  typeFactory; // todo: delete??
	private final DllFactory   dllFactory;
	private final ThunkManager thunkManager;
	private static native String when();
	private JSDI()
	{
		whenBuilt = when();
		typeFactory = new TypeFactory(this);
		dllFactory = new DllFactory(this);
		thunkManager = new ThunkManager(this);
	}

	//
	// ACCESSORS
	//

	public TypeFactory getTypeFactory() {
		return typeFactory;
	}

	public DllFactory getDllFactory() {
		return dllFactory;
	}

	public ThunkManager getThunkManager() {
		return thunkManager;
	}
}
