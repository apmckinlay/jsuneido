/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.jsdi;

import suneido.SuException;

/**
 * TODO: docs
 * @author Victor Schappert
 * @since 20130624
 */
@DllInterface
public class JSDIException extends SuException {

	//
	// SERIALIZATION
	//

	/**
	 * Required to silence Java compiler warning.
	 */
	private static final long serialVersionUID = 7476235241017377212L;

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
