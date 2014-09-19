/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import suneido.SuContainer;
import suneido.SuInternalError;
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

	public abstract Object getFrame();

	public abstract int getLineNumber();

	public final SuContainer getLocalsContainer() {
		SuContainer locals = new SuContainer();
		for (LocalVariable local : this.locals) {
			locals.put(local.getName(), local.getValue());
		}
		return locals;
	}

	public abstract CallableType getCallableType();
}
