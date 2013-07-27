package suneido.language.jsdi.dll;

/**
 * Quick-and-dirty pointer to a <code>stdcall</code> function in a DLL, for
 * testing purposes only.
 *
 * @author Victor Schappert
 * @since 20130723
 * @see TestCall
 */
final class QuickDll {

	private final long hModule;
	final long ptr;

	public QuickDll(String libraryName, String funcName) {
		hModule = DllFactory.loadLibrary(libraryName);
		if (0 == hModule) {
			throw new RuntimeException("FuncPtr: failed to load library '" + libraryName + "'");
		}
		ptr = DllFactory.getProcAddress(hModule, funcName);
		if (0 == ptr) {
			throw new RuntimeException("FuncPtr: failed to get address of '" + funcName + "'");
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			DllFactory.freeLibrary(hModule);
		} finally {
			super.finalize();
		}
	}
}
