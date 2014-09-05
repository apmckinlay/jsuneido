/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suneido.SuInternalError;

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

	private final List<LocalVariable> locals;

	//
	// CONSTRUCTORS
	//

	protected Frame(LocalVariable[] locals) {
		if (null == locals) {
			throw new SuInternalError("locals cannot be null");
		}
		this.locals = Collections.unmodifiableList(Arrays.asList(locals));
	}

	//
	// ACCESSORS
	//

	public abstract Object getFrame();

	public abstract int getLineNumber();

	public final List<LocalVariable> getLocals() {
		return locals;
	}
}
