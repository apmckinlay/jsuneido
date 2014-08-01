package suneido.language.builtin;

import suneido.SuException;
import suneido.jsdi.DllInterface;
import suneido.jsdi.LogLevel;
import suneido.language.BuiltinClass;
import suneido.language.Params;

/**
 * <p>
 * Simple built-in class for inspecting JSDI.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140731
 */
@DllInterface
public final class JSDI extends BuiltinClass {
	public static final JSDI singleton = new JSDI();

	private JSDI() {
		super(JSDI.class);
	}

	private static boolean isInitialized() {
		return suneido.jsdi.JSDI.isInitialized();
	}

	private static suneido.jsdi.JSDI getInstance() {
		return suneido.jsdi.JSDI.getInstance();
	}

	@Override
	protected Object newInstance(Object... args) {
		throw new SuException("cannot create instances of JSDI");
	}

	public static Object Built(Object self) {
		return isInitialized() ? getInstance().whenBuilt() : false;
	}

	public static Object Error(Object self) {
		if (isInitialized()) {
			return false;
		} else {
			Throwable t = suneido.jsdi.JSDI.getInitError();
			if (null == t) {
				return "";
			} else {
				return t.toString();
			}
		}
	}

	public static Object Path(Object self) {
		return isInitialized() ? getInstance().getLibraryPath().toString()
				: false;
	}

	@Params("level=null")
	public static Object LogThreshold(Object self, Object levelArg) {
		if (!isInitialized()) {
			throw new SuException("JSDI not available",
					suneido.jsdi.JSDI.getInitError());
		} else if (null != levelArg) {
			final String str = levelArg.toString();
			LogLevel level = null;
			try {
				level = LogLevel.valueOf(str);
			} catch (IllegalArgumentException e) {
				throw new SuException("invalid level: '" + str + '\'', e);
			}
			if (level != null) {
				getInstance().setLogThreshold(level);
			}
		}
		// Always get the value, regardless of whether we set it
		return JSDI.getInstance().getLogThreshold().toString();
	}

}