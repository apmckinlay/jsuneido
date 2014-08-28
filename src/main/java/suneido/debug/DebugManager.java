package suneido.debug;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.Suneido;

/**
 * <p>
 * Singleton class for managing the current debug model. This class is
 * responsible for initializing the debug model&dagger; as well as reporting the
 * current debug model to any interested callers.
 * </p>
 * <p>
 * &dagger;: <em>Initializing includes testing if {@link DebugModel#ALL}
 * debugging works if it is requested and falling back to
 * {@link DebugModel#STACK} if it does not; and starting a JDI client if
 * {@link DebugModel#ALL} needs to be implemented via an in-process JDI
 * daemon.</em>
 * </p>
 *
 * @author Victor Schappert
 * @since 20140828
 */
public final class DebugManager {

	//
	// DATA
	//

	private final DebugModel model;

	//
	// CONSTRUCTORS
	//

	private DebugManager(DebugModel model) {
		if (null == model) {
			throw new NullPointerException();
		}
		if (DebugModel.ALL == model) {
			this.model = tryToInitFullDebugging();
		} else {
			this.model = model;
		}
	}

	//
	// INTERNALS
	//

	private DebugModel tryToInitFullDebugging() {
		if (!testIfLocalsAvailable()) {
			String jdwpClientPort = DebugUtil.getJDWPAgentClientPort();
			if (null != jdwpClientPort) {
				if (true)
					throw new Error("not implemented yet: try to start JDI"); // FIXME:
				// TODO:
				// implement
				// me!
				if (!testIfLocalsAvailable()) {
					Suneido.errlog("unable to initialize 'all' debugging - falling back to 'stack' debugging");
					return DebugModel.STACK;
				}
			}
		}
		return DebugModel.ALL;
	}

	private static boolean testIfLocalsAvailable() {
		final SuException test = new SuException("test");
		throw new Error("not implemented yet"); // FIXME: TODO: implement me!
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the actual debug model in use.
	 *
	 * @return Debug model
	 */
	public DebugModel getDebugModel() {
		return model;
	}

	//
	// SINGLETON
	//

	private static DebugManager instance = null;

	/**
	 * Initializes the debug manager.
	 *
	 * @param requestedModel
	 *            Requested debug model (the actual model that ends up being
	 *            used may be different)
	 * @see #getInstance()
	 * @see #getDebugModel()
	 */
	public static synchronized void init(DebugModel requestedModel) {
		if (null != instance) {
			throw new SuInternalError("init() already called");
		}
		instance = new DebugManager(requestedModel);
	}

	/**
	 * <p>
	 * Returns the singleton instance.
	 * </p>
	 *
	 * <p>
	 * This method should not be called before {@link #init(DebugModel)}.
	 * </p>
	 *
	 * @return Singleton instance
	 * @see #init(DebugModel)
	 */
	public static DebugManager getInstance() {
		if (null != instance) {
			synchronized (DebugManager.class) {
				if (null != instance) {
					Suneido.errlog("init() not called; defaulting to stack debugging");
					init(DebugModel.STACK);
				}
			}
		}
		return instance;
	}
}
