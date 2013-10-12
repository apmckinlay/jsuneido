package suneido.language.jsdi.com;

import suneido.language.jsdi.DllInterface;
import suneido.language.jsdi.JSDIException;

/**
 * Exception thrown when a Suneido type cannot be marshalled into a canonical
 * Java type which can be marshalled into a value to send to a COM function, or
 * when a value returned by a COM function cannot be marshalled into a canonical
 * type used by Suneido.
 *
 * @author Victor Schappert
 * @since 20131012
 * @see Canonifier
 */
@DllInterface
public final class COMMarshallingException extends JSDIException {

	//
	// SERIALIZATION
	//

	/**
	 * Required to silence Java compiler warning.
	 */
	private static final long serialVersionUID = 5447318541868336650L;

	//
	// CONSTRUCTORS
	//

	public COMMarshallingException(String message) {
		super(message);
	}

	public COMMarshallingException(String message, Throwable cause) {
		super(message, cause);
	}
}
