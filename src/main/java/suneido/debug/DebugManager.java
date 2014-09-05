/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import javax.annotation.concurrent.ThreadSafe;

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
@ThreadSafe
public final class DebugManager {

	//
	// DATA
	//

	private DebugModel actualModel;
	private int jsdebugAgentState;

	//
	// CONSTRUCTORS
	//

	private DebugManager() {
		actualModel = DebugModel.STACK;
		jsdebugAgentState = JSDEBUG_AGENT_NOT_CHECKED;
	}

	//
	// INTERNALS
	//

	private static final int JSDEBUG_AGENT_NOT_CHECKED = -1;
	private static final int JSDEBUG_AGENT_NOT_AVAILABLE = 0;
	private static final int JSDEBUG_AGENT_AVAILABLE = 1;

	private DebugModel tryToInitFullDebugging() {
		if (!testIf_jsdebugAgent_Available()) {
			String jdwpClientPort = DebugUtil.getJDWPAgentClientPort();
			if (null != jdwpClientPort) {
				if (false)
					throw new Error("not implemented yet: try to start JDI"); // FIXME:
				// TODO:
				// implement
				// me!
				if (!testIfStackInfoAvailable()) {
					Suneido.errlog("unable to initialize 'all' debugging - falling back to 'stack' debugging");
					return DebugModel.STACK;
				}
			}
		}
		return DebugModel.ALL;
	}

	private void tryToStopFullDebuggingByJDWP() {
		// Stop full debugging if implemented by way of a JDWP client running in
		// this process...
		if (false)
			throw new Error(
		        "not implemented yet: try to stop full debugging by JDWP");
		// TODO: implement this as part of implementing JDWP version of jsdebug
		//       agent...
	}

	private static boolean testIfStackInfoAvailable() {
		return (new StackInfo()).isInitialized();
	}

	private boolean testIf_jsdebugAgent_Available() {
		if (JSDEBUG_AGENT_NOT_CHECKED == jsdebugAgentState) {
			jsdebugAgentState = testIfStackInfoAvailable() ? JSDEBUG_AGENT_AVAILABLE
			        : JSDEBUG_AGENT_NOT_AVAILABLE;
		}
		return JSDEBUG_AGENT_AVAILABLE == jsdebugAgentState;
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
	 * Creates and returns a call stack for execution stack of the current
	 * thread with content determined by the actual debug model in use.
	 *
	 * @param throwable
	 *            Throwable that is associated with the returned call stack
	 * @return Call stack for the execution stack of the current thread
	 * @see #makeCallstackFromThrowable(Throwable)
	 */
	public Callstack makeCallstackForCurrentThread(Throwable throwable) {
		switch (actualModel) {
		case ALL:
			return new CallstackAll();
		case STACK:
			return new CallstackStack(throwable);
		case NONE:
			return new CallstackNone(throwable);
		default:
			throw SuInternalError.unhandledEnum(actualModel);
		}
	}

	/**
	 * Creates and returns a call stack whose content is derived from the stack
	 * trace of a {@link Throwable}.
	 *
	 * @param throwable
	 *            Throwable object whose stack trace will provide the content
	 *            for the return value; may not be <b>{@code null}</b> and must
	 *            not be an instance of {@link CallstackProvider}
	 * @return Call stack derived from {@code throwable}'s stack trace
	 */
	public Callstack makeCallstackFromThrowable(Throwable throwable) {
		switch (actualModel) {
		case ALL:
			throw new SuInternalError("can't make 'all' Callstack from throwable");
		case STACK:
			return new CallstackStack(throwable);
		case NONE:
			return new CallstackNone(throwable);
		default:
			throw SuInternalError.unhandledEnum(actualModel);
		}
	}

	/**
	 * Changes the debug model.
	 *
	 * @param requestedModel
	 *            Debug model requested
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
			if (this.actualModel == DebugModel.ALL) {
				tryToStopFullDebuggingByJDWP();
			}
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
