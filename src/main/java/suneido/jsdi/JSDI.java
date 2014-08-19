/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static suneido.SuInternalError.unreachable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import suneido.SuInternalError;
import suneido.util.FileFinder;

/**
 * <p>
 * Class responsible for instantiating all JSDI objects which are available
 * outside of the package.
 * </p>
 * <p>
 * This class fulfills three functions. First, it ensures that the JSDI DLL is
 * loaded. Second, it ensures that the JSDI DLL is correctly initialized. Third,
 * it provides access to factory and manager objects needed to construct JSDI
 * types.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130624
 */
@DllInterface
public final class JSDI {

	//
	// CONSTANTS
	//

	private static final String LIBRARY_PATH_PROPERTY_NAME = "suneido.library.path";
		// NOTE: We will want to move this library path property name constant
		//       into the Suneido class i we end up adding any more native
		//       libraries besides jsdi.
	private static final String LIBRARY_NAME = "jsdi.dll";
	private static final String PACKAGE_X86 = "suneido.jsdi.abi.x86";
	private static final String PACKAGE_AMD64 = "suneido.jsdi.abi.amd64";
	private static final String RELPATH_X86 = "lib/x86";
	private static final String RELPATH_AMD64 = "lib/amd64";

	//
	// SINGLETON
	//

	private static final JSDI instance;
	private static final Throwable initError;

	/**
	 * <p>
	 * Returns the singleton instance if available, or throws an internal error
	 * otherwise.
	 * </p>
	 *
	 * @return Singleton
	 * @throws SuInternalError
	 *             If not {@link #isInitialized()}
	 */
	public static JSDI getInstance() {
		if (null != instance) {
			return instance;
		} else {
			throw new SuInternalError("failed to initialize JSDI", initError);
		}
	}

	/**
	 * <p>
	 * Indicates whether the singleton instance is available.
	 * </p>
	 *
	 * @return Whether {@link #getInstance()} will return the singleton instance
	 *         rather than throwing
	 */
	public static boolean isInitialized() {
		return null != instance;
	}

	/**
	 * <p>
	 * Returns the initialization error, if any.
	 * </p>
	 *
	 * @return Initialization error, if not {@link #isInitialized()}, or
	 *         {@code null} if intialized OK
	 * @since 20140731
	 */
	public static Throwable getInitError() {
		return initError;
	}

	//
	// DLL INITIALIZATION
	//

	private static native void init();

	static {
		Throwable initError_ = null;
		JSDI instance_ = null;
		try {
			Platform platform = Platform.getPlatform();
			// Load the factory and thunk manager Java classes before trying to
			// load the native library. This is because if these classes aren't
			// present at all (e.g. in the JAR), we will error out and simply
			// end up in a state where the JSDI Java class is properly
			// initialized at a Java level, but getInstance() throws an
			// exception because actual initialization is impossible. Whereas,
			// if you try to load the native library first, it will cause
			// difficult-to-explain NoClassDefFound errors because init() asks
			// for global references to various classes that don't exist.
			Class<? extends Factory> factoryClass = loadFactoryClass(platform);
			Class<? extends ThunkManager> thunkManagerClass = loadThunkManagerClass(platform);
			// Now load and initialize the DLL
			File path = findLibrary(platform); // throws if can't find
			System.load(path.getAbsolutePath());
			init();
			// Finally instantiate the JSDI instance
			instance_ = new JSDI(platform, path, factoryClass,
					thunkManagerClass);
		} catch (Throwable t) {
			initError_ = t; /* squelch, but it is available if needed */
		} finally {
			initError = initError_;
			instance = instance_;
		}
	}

	private static final File findLibrary(Platform platform) throws IOException {
		final FileFinder finder = new FileFinder(true);
		finder.addSearchPathPropertyNames(LIBRARY_PATH_PROPERTY_NAME);
		switch (platform) {
		case WIN32_X86:
			finder.addRelPaths(RELPATH_X86);
			break;
		case WIN32_AMD64:
			finder.addRelPaths(RELPATH_AMD64);
			break;
		case UNSUPPORTED_PLATFORM:
			throw new SuInternalError("unsupported platform");
		default:
			throw SuInternalError.unhandledEnum(Platform.class);
		}
		final FileFinder.SearchResult result = finder.find(LIBRARY_NAME);
		if (!result.success()) {
			throw new SuInternalError("can't find '" + LIBRARY_NAME
					+ "': result => " + result);
		}
		if (FileFinder.SearchStage.RELPATH_RELATIVE_TO_CLASSPATH == result.stage) {
			result.file.deleteOnExit();
		}
		return result.file;
	}

	//
	// DATA and CONSTRUCTORS
	//

	private final Platform platform;
	private final File path; // path to native DLL
	private final String whenBuilt; // when the native DLL was built
	private final Factory factory;
	private final ThunkManager thunkManager;
	private boolean isFastMode;
	private LogLevel logThreshold;

	private JSDI(Platform platform, File path,
			Class<? extends Factory> factoryClass,
			Class<? extends ThunkManager> thunkManagerClass) {
		this.platform = platform;
		this.path = path;
		this.whenBuilt = when();
		this.factory = makeSubclass(factoryClass, "factory");
		this.thunkManager = makeSubclass(thunkManagerClass, "thunk manager");
		this.isFastMode = true;
		this.logThreshold = logThreshold(null);
	}

	//
	// INTERNALS
	//

	private static native String when();

	private static native LogLevel logThreshold(LogLevel level);

	private static Class<? extends Factory> loadFactoryClass(Platform platform) {
		String className = null;
		switch (platform) {
		case WIN32_X86:
			className = PACKAGE_X86 + ".FactoryX86";
			break;
		case WIN32_AMD64:
			className = PACKAGE_AMD64 + ".Factory64";
			break;
		case UNSUPPORTED_PLATFORM:
			throw notSupported();
		default:
			throw unreachable();
		}
		return loadSubclass(Factory.class, className, "factory");
	}

	private static Class<? extends ThunkManager> loadThunkManagerClass(
			Platform platform) {
		String className = null;
		switch (platform) {
		case WIN32_X86:
			className = PACKAGE_X86 + ".ThunkManagerX86";
			break;
		case WIN32_AMD64:
			className = PACKAGE_AMD64 + ".ThunkManager64";
			break;
		case UNSUPPORTED_PLATFORM:
			throw notSupported();
		default:
			throw unreachable();
		}
		return loadSubclass(ThunkManager.class, className, "thunk manager");
	}

	private static JSDIException notSupported() {
		return new JSDIException("JSDI not supported on this platform");
	}

	private static Error cantInstantiate(String infoName, Exception cause) {
		final String errMsg = "can't instantiate " + infoName + " class";
		return new SuInternalError(errMsg, cause);
	}

	private static <T> Class<? extends T> loadSubclass(Class<T> superclass,
			String className, String infoName) {
		try {
			return Class.forName(className).asSubclass(superclass);
		} catch (ClassNotFoundException x) {
			final String errMsg = "can't find " + infoName + " class";
			throw new SuInternalError(errMsg, x);
		}
	}

	private <T> T makeSubclass(Class<? extends T> subclass, String infoName) {
		Constructor<? extends T> ctor = null;
		try {
			ctor = subclass.getDeclaredConstructor(JSDI.class);
		} catch (NoSuchMethodException x) {
			final String errMsg = "can't find " + infoName + " constructor";
			throw new SuInternalError(errMsg, x);
		}
		ctor.setAccessible(true);
		try {
			return ctor.newInstance(this);
		} catch (InvocationTargetException x) {
			throw cantInstantiate(infoName, x);
		} catch (IllegalAccessException x) {
			throw cantInstantiate(infoName, x);
		} catch (InstantiationException x) {
			throw cantInstantiate(infoName, x);
		}
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the factory required to instantiate JSDI types in an
	 * implementation-neutral fashion.
	 * 
	 * @return Factory
	 * @since 20140718
	 */
	public Factory getFactory() {
		return factory;
	}

	/**
	 * Returns the thunk manager used to map between bound Suneido values and
	 * callbacks.
	 * 
	 * @return Thunk manager
	 */
	public ThunkManager getThunkManager() {
		return thunkManager;
	}

	/**
	 * Returns the path to the loaded native DLL. 
	 *
	 * @return Library path
	 * @since 20140731
	 */
	public File getLibraryPath() {
		return path;
	}

	/**
	 * Returns a string indicating JSDI library version information.
	 *
	 * @return Library version information
	 */
	public String whenBuilt() {
		return whenBuilt;
	}

	/**
	 * <p>
	 * Queries whether certain optimizations are enabled.
	 * </p>
	 *
	 * <p>
	 * Optimizations are on by default.
	 * </p
	 *
	 * @return Whether optimizations are enabled both in Java and on the native
	 *         side.
	 * @since 20140730
	 * @see #setFastMode(boolean)
	 */
	public boolean isFastMode() {
		return isFastMode;
	}

	/**
	 * <p>
	 * Turns certain optimizations, both in Java and on the native side, on or
	 * off.
	 * </p>
	 *
	 * <p>
	 * Optimizations are on by default.
	 * </p
	 *
	 * @param inFastMode
	 *            Set {@code true} iff optimizations should be on
	 * @since 20140730
	 * @see #isFastMode()
	 */
	public void setFastMode(boolean inFastMode) {
		isFastMode = inFastMode;
	}

	/**
	 * Queries the native side log level.
	 *
	 * @return Logging level
	 * @see #setLogThreshold(LogLevel)
	 * @since 20140730
	 */
	public LogLevel getLogThreshold() {
		return logThreshold;
	}

	/**
	 * <p>
	 * Attempts to set the native side logging level.
	 * </p>
	 *
	 * <p>
	 * It may not be possible to set the log level to the desired value if the
	 * DLL was build with a static log threshold of lower verbosity than the
	 * desired level. In this case, the log threshold will be set to the maximum
	 * verbosity level that is less than or equal to the requested level. After
	 * calling this method, query {@link #getLogThreshold()} to determine the actual
	 * logging level set.
	 * </p>
	 *
	 * @param level
	 *            Non-NULL logging level to set
	 * @see #getLogThreshold()
	 * @since 20140730
	 */
	public void setLogThreshold(LogLevel level) {
		if (null == level) {
			throw new SuInternalError("log level cannot be null");
		}
		logThreshold = logThreshold(level);
	}
}
