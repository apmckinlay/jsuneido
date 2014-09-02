/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
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
@ThreadSafe
public final class DebugManager {

	//
	// DATA
	//

	private DebugModel actualModel;

	//
	// CONSTRUCTORS
	//

	private DebugManager() {
		actualModel = DebugModel.STACK;
	}

	//
	// INTERNALS
	//

	private DebugModel tryToInitFullDebugging() {
		if (!testIfLocalsAvailable()) {
			String jdwpClientPort = DebugUtil.getJDWPAgentClientPort();
			if (null != jdwpClientPort) {
				if (false)
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
		if (true)
			return false;
		throw new Error("not implemented yet"); // FIXME: TODO: implement me!
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the actual debug model in use.
	 *
	 * @return Debug model
	 * @see #setDebugModel(DebugModel)
	 */
	public DebugModel getDebugModel() {
		return actualModel;
	}

	/**
	 * Changes the debug model.
	 *
	 * @param requestedModel Debug model requested
	 * @return Debug model actually set
	 * @see #getDebugModel()
	 */
	public synchronized DebugModel setDebugModel(DebugModel requestedModel) {
		if (null == requestedModel) {
			throw new NullPointerException();
		}
		if (DebugModel.ALL == requestedModel) {
			this.actualModel = tryToInitFullDebugging();
		} else {
			this.actualModel = requestedModel;
		}
		return this.actualModel;
	}

	//
	// SINGLETON
	//

	private static DebugManager instance = new DebugManager();

	/**
	 * <p>
	 * Returns the singleton instance.
	 * </p>
	 *
	 * @return Singleton instance
	 */
	public static DebugManager getInstance() {
		return instance;
	}
}
