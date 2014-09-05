/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suneido.SuInternalError;
import suneido.runtime.SuCallable;
import suneido.runtime.SuClass;

/**
 * Represents one frame in a Suneido {@link Callstack}.
 *
 * @author Victor Schappert
 * @since 20140903
 */
public final class Frame {

	//
	// DATA
	//

	private final SuCallable callable; // Null if not determinable
	private final String className; // Null if not a member function
	private final String functionName; // Never null
	private final int lineNumber; // Valid only if >0
	private final List<LocalVariable> locals;

	//
	// CONSTRUCTORS
	//

	Frame(SuCallable callable, int lineNumber, LocalVariable[] locals) {
		if (null == callable) {
			throw new SuInternalError("callable cannot be null");
		}
		if (null == locals) {
			throw new SuInternalError("locals cannot be null");
		}
		this.callable = callable;
		String[] typeAndFunctionName = callableToNames(callable);
		this.className = typeAndFunctionName[0];
		this.functionName = typeAndFunctionName[1];
		this.lineNumber = lineNumber;
		this.locals = Collections.unmodifiableList(Arrays.asList(locals));
	}

	//
	// INTERNALS
	//

	private static String[] callableToNames(SuCallable callable) {
		String className = null;
		String functionName = null;
		// Get the class name
		SuClass clazz = callable.suClass();
		if (null != clazz) {
			className = clazz.valueName();
		}
		// Get the callable function name
		functionName = callable.valueName();
		return new String[] { className, functionName };
	}

	//
	// ACCESSORS
	//

	public String getFrameName() {
		if (null != className) {
			return className + '.' + functionName;
		} else {
			return functionName;
		}
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public List<LocalVariable> getLocals() {
		return locals;
	}
}
