package suneido.language.jsdi.dll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import suneido.language.FunctionSpec;
import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDI;
import suneido.language.jsdi.JSDIException;
import suneido.language.jsdi.type.Type;
import suneido.language.jsdi.type.TypeList;

/**
 * Manages creation and allocation of {@code dll} objects.
 * @author Victor Schappert
 * @since 20130708
 */
@DllInterface
public final class DllFactory {

	//
	// TYPES
	//

	private static final class LoadedLibrary {
		public final long hModule;
		public int refCount;
		public LoadedLibrary(long hModule) {
			assert 0 != hModule : "Library hModule cannot be NULL";
			this.hModule = hModule;
			this.refCount = 1;
		}
	}

	/**
	 * Observer which is notified whenever a new {@link Dll} is compiled. This
	 * is for the purpose of plugging in diagnostics.
	 * @author Victor Schappert
	 * @since 20130709
	 * @see DllFactory#addObserver(DllMakeObserver)
	 */
	public static interface DllMakeObserver {
		/**
		 * Fired when a new {@link Dll} is compiled. 
		 * @param dll Newly-created {@code dll}.
		 */
		public void madeDll(Dll dll);
	}

	//
	// DATA
	//

	private final JSDI jsdi;
	private final Object lock = new Object();
	private final Map<String, LoadedLibrary> libraries =
		new HashMap<String, LoadedLibrary>();
	private final ArrayList<DllMakeObserver> observers =
		new ArrayList<DllMakeObserver>();

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs the factory. Clients do not need to use this constructor
	 * directly and should use {@link JSDI#getDllFactory()}.
	 * @param jsdi JSDI instance
	 */
	public DllFactory(JSDI jsdi) {
		if (null == jsdi) {
			throw new IllegalArgumentException("jsdi cannot be null");
		}
		this.jsdi = jsdi;
	}

	//
	// FACTORY METHODS
	//

	/**
	 * Constructs a {@link Dll} capable of invoking the given library function.
	 * @param suTypeName The Suneido (<em>ie</em> user-assigned type name)
	 * @param libraryName Name of the DLL library module
	 * @param userFuncName Function name to load within the library. If no
	 * function with this name is found, the name {@code userFuncName + 'A'} is
	 * also tried.
	 * @param params {@link TypeList} describing the names, types, and positions
	 * of the {@link Dll}'s parameters.
	 * @param returnType {@link Type} describing the return type of the
	 * {@link Dll}.
	 * @return Constructed {@link Dll}
	 * @throws JSDIException If {@code libraryName} cannot be loaded with the
	 * API call {@code LoadLibrary}; or if the address of the function
	 * {@code userFuncName} (or {@code userFuncName + 'A'}) can't be located
	 * with the API call {@code GetProcAddress}; or if some other error occurs.
	 */
	public Dll makeDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		//
		// Load the library (or fetch the already-loaded library).
		//
		final long hModule = getLibraryHandle(libraryName);
		boolean success = false;
		try {
			//
			// Get the function address
			//
			String funcName = userFuncName;
			long funcPtr = getProcAddress(hModule, funcName);
			if (0 == funcPtr) {
				funcName += 'A';
				funcPtr = getProcAddress(hModule, funcName);
				if (0 == funcPtr) {
					throw new JSDIException("can't get address of "
							+ libraryName + ":" + userFuncName + " or "
							+ libraryName + ":" + funcName);
				}
			}
			//
			// Make a FunctionSpec of the DLL so the compiler knows how to
			// handle call expressions.
			//
			FunctionSpec funcSpec = makeFunctionSpec(params);
			//
			// Determine the native call characteristics.
			//
			ReturnTypeGroup rtg = ReturnTypeGroup.fromType(returnType);
			CallGroup cg = CallGroup.fromTypeList(params);
			NativeCall nc = null;
			if (null != cg) {
				nc = NativeCall.get(cg, rtg, params.size());
			}
			//
			// Build the Dll object to return
			//
			final Dll result = new Dll(funcPtr, params, returnType, rtg, nc,
					suTypeName, this, libraryName, userFuncName, funcName,
					funcSpec);
			success = true;
			for (DllMakeObserver observer : observers) {
				observer.madeDll(result);
			}
			return result;
		} finally {
			//
			// If an exception was thrown before we were able to instantiate the
			// object, reduce the reference count on the loaded library module.
			//
			if (! success) {
				releaseLibraryHandle(libraryName);
			}
		}
	}

	/**
	 * <p>
	 * Deliberately package-internal. This method should only be called from the
	 * finalizer of a {@link Dll} in order to decrease the reference count
	 * associated with {@code dll} object's backing library.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong>: The idea of reference-counting loaded DLL
	 * libraries may seem bizarre, since Windows already maintains a reference
	 * count which is incremented on every call to {@code LoadLibrary()} and
	 * decremented on every call to {@code FreeLibrary()}. However, by keeping
	 * our own count in Java-land, we can avoid making JNI calls every time we
	 * simply want a handle to an already-loaded module. 
	 * </p>
	 * @param dll Dll to free
	 */
	void freeDll(Dll dll) {
		releaseLibraryHandle(dll.libraryName);
	}

	/**
	 * Adds to the list of observers to be notified when a new {@code dll} is
	 * compiled. 
	 * @param observer Observer to add to list
	 */
	public void addObserver(DllMakeObserver observer) {
		if (null == observer) {
			throw new IllegalArgumentException("observer cannot be null");
		}
		observers.add(observer);
	}

	//
	// INTERNALS
	//

	private static FunctionSpec makeFunctionSpec(TypeList params) {
		return new FunctionSpec(params.getEntryNames());
	}

	private static String normalizedLibraryName(String libraryName) {
		return libraryName.toLowerCase();
	}

	private long getLibraryHandle(String libraryName) {
		final String libraryName_ = normalizedLibraryName(libraryName);
		LoadedLibrary ll;
		synchronized(lock) {
			ll = libraries.get(libraryName_);
			if (null != ll)
				++ll.refCount;
			else {
				long hModule = loadLibrary(libraryName_);
				if (0 == hModule) {
					throw new JSDIException("can't LoadLibrary " + libraryName);
				}
				ll = new LoadedLibrary(hModule);
				libraries.put(libraryName_, ll);
			}
		}
		return ll.hModule;
	}

	private void releaseLibraryHandle(String libraryName) {
		final String libraryName_ = normalizedLibraryName(libraryName);
		synchronized(lock) {
			final LoadedLibrary ll = libraries.get(libraryName_);
			if (0 == --ll.refCount) {
				freeLibrary(ll.hModule);
				libraries.remove(libraryName_);
			}
		}
	}

	static native long loadLibrary(String libraryName);

	static native void freeLibrary(long hModule);

	static native long getProcAddress(long hModule, String procName);
}
