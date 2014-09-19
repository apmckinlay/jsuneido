/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

/**
 * <p>
 * Encapsulates serious unexpected errors within the Suneido backend system.
 * In other words, if and when this exception is thrown it indicates a system-
 * level bug, not anything that user code could anticipate or avoid.
 * </p>
 * <p>
 * This class was introduced as a result of rationalizing the exception system
 * in June/July 2014 with the goal of providing "human-readable" exceptions to
 * the Suneido programmer that are equivalent to the exceptions available in
 * cSuneido (<em>ie</em> <code>Locals()</code>,
 * <code>&lt;exception<&gt;.Callstack()</code>). It takes over the parts of
 * SuException that were formerly dedicated to internal errors (<em>ie</em>
 * <code>SuException.unreachable =></code> InternalError#unreachable()).
 * </p>
 *
 * @author Victor Schappert
 * @since 20140622
 */
@SuppressWarnings("serial")
public final class SuInternalError extends Error {

	//
	// CONSTRUCTORS
	//

	/**
	 * <p>
	 * Constructs an internal error having the given message.
	 * </p>
	 * <p>
	 * Where possible, please use the appropriate static method instead of
	 * direct construction. <em>eg</em> {@link #unreachable()},
	 * {@link #unhandledEnum(Class)}.
	 * </p>
	 * 
	 * @param message Error message string
	 */
	public SuInternalError(String message) {
		super(message);
	}

	/**
	 * <p>
	 * Constructs an internal error whose cause is another throwable event.
	 * </p>
	 *
	 * @param message Error message string
	 * @param cause Cause of the internal error
	 */
	public SuInternalError(String message, Throwable cause) {
		super(message, cause);
	}

	//
	// STATICS
	//

	/**
	 * <p>
	 * Constructs and returns an internal error to throw when code that is
	 * assumed to be unreachable is reached.
	 * </p>
	 * <p>
	 * Prior to 20140622, this method lived in {@code SuException.unreachable()}.
	 * <p>
	 *
	 * @return Unreachable error to throw
	 * @see #unhandledEnum(Class)
	 */
	public static final SuInternalError unreachable() {
		return new SuInternalError("should not reach here");
	}

	/**
	 * Constructs and returns an internal error to throw when a switch statement
	 * unexpectedly fails to handle an {@code enum} enumerator.
	 *
	 * @param e Enumerator that wasn't handled 
	 * @return Unhandled enumerator error to throw
	 * @see #unreachable()
	 */
	public static final <E extends Enum<E>> SuInternalError unhandledEnum(E e) {
		return new SuInternalError("unhandled " + e.getClass().getSimpleName()
				+ " enumerator in switch: " + e);
	}
}
