/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import suneido.SuContainer;
import suneido.SuInternalError;
import suneido.SuValue;
import suneido.runtime.CallableType;

/**
 * Represents one frame in a Suneido {@link Callstack}.
 *
 * @author Victor Schappert
 * @since 20140903
 */
public abstract class Frame {

	//
	// DATA
	//

	private final LocalVariable[] locals;

	//
	// CONSTRUCTORS
	//

	protected Frame(LocalVariable[] locals) {
		if (null == locals) {
			throw new SuInternalError("locals cannot be null");
		}
		this.locals = locals;
	}

	//
	// ACCESSORS
	//

	/**
	 * <p>
	 * Returns a value representing, as best as possible for the current
	 * {@link DebugModel debug model}, the callable object for the current stack
	 * frame.
	 * </p>
	 * <p>
	 * For {@link DebugModel#ON full debugging}, this will be the actual
	 * {@link SuCallable} that was called. For lesser debug modes, it will be
	 * some other value with, at a minimum, a meaningful display value.
	 * </p>
	 *
	 * @return Object representing the callable object for this frame
	 * @see #getCallableType()
	 */
	public abstract SuValue getFrame();

	/**
	 * <p>
	 * Returns the source code line number containing the execution point
	 * represented by this stack frame, or a number less than 1 if this
	 * information is not available.
	 * </p>
	 *
	 * @return A positive line number, or a number less than 1 if there is no
	 *         available line number
	 * @see StackTraceElement#getLineNumber()
	 */
	public abstract int getLineNumber();

	/**
	 * <p>
	 * Returns the {@link CallableType callable type} of the callable object
	 * at the execution point represented by this stack frame, or
	 * {@link CallableType#UNKNOWN} if this information is unavailable.
	 * </p>
	 *
	 * @return Callable type of {@link #getFrame()}
	 * @see #getFrame()
	 */
	public abstract CallableType getCallableType();

	/**
	 * <p>
	 * Returns a {@link SuContainer container} whose dictionary contains the
	 * local variables for the current stack frame. The dictionary key is the
	 * local variable name and the dictionary value is the local variable value.
	 * </p>
	 *
	 * <p>
	 * If there is no local variable information available, the result is an
	 * empty container.
	 * </p>
	 *
	 * @return Local variables dictionary for this frame
	 */
	public final SuContainer getLocalsContainer() {
		SuContainer locals = new SuContainer();
		for (LocalVariable local : this.locals) {
			locals.put(local.getName(), local.getValue());
		}
		return locals;
	}
}
