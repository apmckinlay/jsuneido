/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import static suneido.SuInternalError.unreachable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import suneido.SuInternalError;
import suneido.Suneido;

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

	private static final String PACKAGE_X86 = "suneido.jsdi.abi.x86";

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
	 * @throws SuInternalError If not {@link #isInitialized()}
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

	//
	// DLL INITIALIZATION
	//

	private static native void init();

	static {
		Throwable initError_ = null;
		JSDI instance_ = null;
		try {
			File path = new File("lib\\jsdi.dll");
			System.load(path.getAbsolutePath());
			// TODO: Figure out how JNA picks the correct DLL right out of the JAR
			// and do that.
			init();
			instance_ = new JSDI();
		} catch (Throwable t) {
			initError_ = t;
		} finally {
			initError = initError_;
			instance = instance_;
		}
	}

	//
	// DATA and CONSTRUCTORS
	//

	private final Platform platform;
	private final String whenBuilt; // when the native DLL was built
	private final Factory factory;
	private final ThunkManager thunkManager;

	private static native String when();

	private JSDI() {
		platform = Platform.getPlatform();
		whenBuilt = when();
		factory = makeFactory();
		thunkManager = makeThunkManager();
	}

	//
	// INTERNALS
	//

	private Factory makeFactory() {
		String className = null;
		switch (platform) {
		case WIN32_X86:
			className = PACKAGE_X86 + ".FactoryX86";
			break;
		case WIN32_AMD64:
			throw new SuInternalError("not implemented yet");
		case UNSUPPORTED_PLATFORM:
			throw new JSDIException("JSDI not supported on this platform");
		default:
			throw unreachable();
		}
		return makeSubclass(Factory.class, className, "factory");
	}

	private ThunkManager makeThunkManager() {
		String className = null;
		switch (platform) {
		case WIN32_X86:
			className = PACKAGE_X86 + ".ThunkManagerX86";
			break;
		case WIN32_AMD64:
			throw new SuInternalError("not implemented yet");
		case UNSUPPORTED_PLATFORM:
			throw new JSDIException("JSDI not supported on this platform");
		default:
			throw unreachable();
		}
		return makeSubclass(ThunkManager.class, className, "thunk manager");
	}

	private static Error cantInstantiate(String infoName,
			Exception cause) {
		final String errMsg = "can't instantiate " + infoName + " class";
		Suneido.errlog(errMsg, cause);
		return new SuInternalError(errMsg, cause);
	}

	private <T> T makeSubclass(Class<T> superclass, String className,
			String infoName) {
		Class<? extends T> clazz = null;
		try {
			clazz = Class.forName(className).asSubclass(superclass);
		} catch (ClassNotFoundException x) {
			final String errMsg = "can't find " + infoName + " class";
			Suneido.errlog(errMsg, x);
			throw new SuInternalError(errMsg, x);
		}
		Constructor<? extends T> ctor = null;
		try {
			ctor = clazz.getDeclaredConstructor(JSDI.class);
		} catch (NoSuchMethodException x) {
			final String errMsg = "can't find " + infoName + " constructor";
			Suneido.errlog(errMsg, x);
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
	 * Returns a string indicating JSDI library version information. 
	 *
	 * @return Library version information
	 */
	public String whenBuilt() {
		return whenBuilt;
	}
}
