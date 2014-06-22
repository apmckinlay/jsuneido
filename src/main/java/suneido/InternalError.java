package suneido;

/**
 * Encapsulates serious unexpected errors within the Suneido backend system.
 * In other words, if and when this exception is thrown it indicates a system-
 * level bug, not anything that user code could anticipate or avoid.
 *
 * This class was introduced as a result of rationalizing the exception system
 * in June/July 2014 with the goal of providing "human-readable" exceptions to
 * the Suneido programmer that are equivalent to the exceptions available in
 * cSuneido (<em>ie</em> <code>Locals()</code>,
 * <code>&lt;exception<&gt;.Callstack()</code>). It takes over the parts of
 * SuException that were formerly dedicated to internal errors (<em>ie</em>
 * <code>SuException.unreachable =></code> InternalError#unreachable()).
 *
 * @author Victor Schappert
 * @since 20140622
 */
public final class InternalError extends Error {

	/**
	 * Automatically-generated serialization version number.
	 */
	private static final long serialVersionUID = 5735199310821065450L;

	//
	// CONSTRUCTORS
	//

	private InternalError(String message) {
		super(message);
	}

	//
	// STATICS
	//

	public static final InternalError unreachable() {
		return new InternalError("should not reach here");
	}
}
