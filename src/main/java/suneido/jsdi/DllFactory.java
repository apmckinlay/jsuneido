/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import java.util.HashMap;
import java.util.Map;

import suneido.SuInternalError;
import suneido.jsdi.type.Type;
import suneido.jsdi.type.TypeList;
import suneido.jsdi.type.VoidType;

/**
 * Manages creation and allocation of {@code dll} objects.
 * @author Victor Schappert
 * @since 20130708
 */
@DllInterface
public abstract class DllFactory {

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

	//
	// DATA
	//

	private final JSDI jsdi;
	private final Object lock = new Object();
	private final Map<String, LoadedLibrary> libraries =
		new HashMap<>();

	//
	// CONSTRUCTORS
	//

	/**
	 * Constructs the factory.
	 * @param jsdi JSDI instance
	 */
	protected DllFactory(JSDI jsdi) {
		if (null == jsdi) {
			throw new SuInternalError("jsdi cannot be null");
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
	public final Dll makeDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType) {
		return VoidType.IDENTIFIER.equals(libraryName) ? new VoidDll(params,
				returnType, suTypeName, this, userFuncName) : makeRealDll(
				suTypeName, libraryName, userFuncName, params, returnType);
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
	final void freeDll(Dll dll) {
		releaseLibraryHandle(dll.libraryName);
	}

	//
	// INTERNALS
	//

	protected abstract Dll makeRealDll(String suTypeName, String libraryName,
			String userFuncName, TypeList params, Type returnType);

	protected long getFuncPtr(String libraryName, String userFuncName) {
		final long hModule = getLibraryHandle(libraryName);
		boolean success = false;
		try {
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
			success = true;
			return funcPtr;
		} finally {
			if (!success) releaseLibraryHandle(libraryName);
		}
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
