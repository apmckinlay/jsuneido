/* Copyright 2013 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.jsdi.com;

import suneido.jsdi.DllInterface;
import suneido.jsdi.JSDIException;

/**
 * Exception thrown when there's a problem using a Suneido {@code COMobject}.
 *
 * @author Victor Schappert
 * @since 20131023
 */
@DllInterface
public final class COMException extends JSDIException {

	//
	// SERIALIZATION
	//

	/**
	 * Required to silence Java compiler warning.
	 */
	private static final long serialVersionUID = 8480803631452870823L;

	//
	// CONSTRUCTORS
	//

	public COMException(String message) {
		super(message);
	}

	public COMException(String message, Throwable cause) {
		super(message, cause);
	}
}
