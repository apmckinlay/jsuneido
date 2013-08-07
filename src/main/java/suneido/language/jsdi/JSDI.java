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
		// NOTE: JVM seems to only be capable of loading DLLs with the same
		//       "bittyness" as it: x64 JVM only loads x64 DLLs, x86 JVM only
		//       loads x86 DLLs. Since my version of MinGW only builds for x86,
		//       I found it easiest just to install a separate x86 JRE for
		//       testing.
		File path = new File("lib\\jsdi.dll");
		System.load(path.getAbsolutePath());
		// TODO: Set this to the proper path, and possibly call loadLibrary
		//       instead of load.
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

	public native boolean isTraceEnabled();

	public TypeFactory getTypeFactory() {
		return typeFactory;
	}

	public DllFactory getDllFactory() {
		return dllFactory;
	}

	public ThunkManager getThunkManager() {
		return thunkManager;
	}

	//
	// MUTATORS
	//

	public native void setTraceEnabled(boolean isEnabled);
}
