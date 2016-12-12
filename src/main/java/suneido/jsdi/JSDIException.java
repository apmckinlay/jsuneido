/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi;

import suneido.SuException;

/**
 * <p>
 * Runtime-type exception thrown when Suneido programmer uses a JSDI facility
 * incorrectly.
 * </p>
 *
 * <p>
 * Like {@link SuException}, this class is meant to encapsulate error conditions
 * that Suneido programmers cause and should anticipate. It is not designed to
 * represent internal errors in the Suneido runtime system that are opaque to
 * the Suneido programmer. Such situations should be addressed with
 * {@link SuInternalError} or a derived class thereof.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20130624
 */
@DllInterface
@SuppressWarnings("serial")
public class JSDIException extends SuException {

	//
	// CONSTRUCTORS
	//

	public JSDIException(String message) {
		super(message);
	}

	public JSDIException(String message, Throwable cause) {
		super(message, cause);
	}

	public JSDIException(Throwable cause) {
		super(cause);
	}
}
