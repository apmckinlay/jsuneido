package suneido.language.jsdi;

import suneido.SuException;

@DllInterface
public final class JSDIException extends SuException {

	//
	// SERIALIZATION
	//

	/**
	 * 
	 */
	private static final long serialVersionUID = 7476235241017377212L;

	//
	// CONSTRUCTORS
	//

	public JSDIException(String message) {
		super(message);
	}

	public JSDIException(Throwable cause) {
		super(cause);
	}
}
