/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.PrintWriter;

import suneido.debug.Callstack;
import suneido.debug.CallstackProvider;
import suneido.debug.DebugManager;
import suneido.debug.Frame;
import suneido.runtime.Ops;

/**
 * <p>
 * Encapsulates a Suneido "runtime" exception that well-written Suneido code
 * must either avoid (<i>eg</i> syntax error in source code) or anticipate and
 * catch (<i>eg</i> problem reading a file).
 * </p>
 *
 * <p>
 * This class may be contrasted with {@link SuInternalError} which represents
 * serious unexpected conditions within the runtime system itself and which no
 * Suneido programmer can be expected to anticipate.
 * </p>
 *
 * @author Andrew McKinlay, Victor Schappert
 * @since 20140903
 * @see SuInternalError
 */
public class SuException extends RuntimeException implements CallstackProvider {

	//
	// SERIALIZATION
	//

	/**
	 * Required to silence Java compiler warning.
	 */
	private static final long serialVersionUID = 1L;

	//
	// DATA
	//

	private final Callstack callstack;

	//
	// CONSTRUCTORS
	//

	/**
	 * <p>
	 * Constructs a new Suneido runtime exception.
	 * </p>
	 *
	 * <p>
	 * The {@code isSuneidoRethrown} parameter is used for two purposes:
	 * <ol>
	 * <li>
	 * The minor purpose is to help format the constructed exception's error
	 * message where a cause is provided. If the construct exception has a
	 * non-<b>{@code null}</b> {@code cause} and {@code isSuneidoRethrown} is
	 * <b>{@code false}</b>, the cause's error message is appended (in
	 * parentheses) to {@code message} to help make the exception cause
	 * immediately obvious.</li>
	 * <li>
	 * The major purpose is to help control the extended {@link Callstack} data
	 * attached to the newly constructed exception. If {@code isSuneidoRethrown}
	 * is <b>{@code true}</b>, the constructed exception simply inherits the
	 * {@link Callstack} data of {@code cause}. This is how Suneido language
	 * <code><b>try</b>/<b>catch</b></code> blocks work. On the other hand, if
	 * If {@code isSuneidoRethrown} is <b>{@code false}</b>, the newly
	 * constructed exception gets {@link Callstack} data representing the
	 * current thread's execution stack at the time the exception was
	 * constructed.</li>
	 * </ol>
	 * </p>
	 * 
	 * @param message
	 *            Message explaining exception
	 * @param cause
	 *            Previous exception which triggered this one; may be <b>
	 *            {@code null}</b> unless {@code isSuneidoRethrown} is <b>
	 *            {@code true}</b> in which case a valid cause must be provided
	 * @param isSuneidoRethrown
	 *            Whether this exception was caught and rethrown by the Suneido
	 *            programmer
	 */
	public SuException(String message, Throwable cause,
	        boolean isSuneidoRethrown) {
		super(makeMessage(message, cause, isSuneidoRethrown), cause);
		if (isSuneidoRethrown) {
			assert null != cause : "Cause cannot be null if this is a rethrown exception";
			if (cause instanceof CallstackProvider) {
				callstack = ((CallstackProvider) cause).getCallstack();
			} else {
				callstack = DebugManager.getInstance().makeCallstackFromThrowable(cause);
			}
		} else {
			super.fillInStackTrace();
			callstack = DebugManager.getInstance()
			        .makeCallstackForCurrentThread(this);
		}
	}

	/**
	 * Constructs a non-rethrown Suneido exception with the given message.
	 *
	 * @param message
	 *            Message explaining exception
	 */
	public SuException(String message) {
		this(message, null, false);
	}

	/**
	 * Constructs a non-rethrown Suneido exception caused by some other
	 * throwable.
	 *
	 * @param cause
	 *            Non-<b>{@code null}</b> cause
	 */
	public SuException(Throwable cause) {
		this(cause.getMessage(), cause, false);
	}

	/**
	 * Constructs a non-rethrown Suneido exception.
	 *
	 * @param message
	 *            Message explaining exception
	 * @param cause
	 *            Previous exception which triggered this one (may be <b>
	 *            {@code null}</b>)
	 */
	public SuException(String message, Throwable cause) {
		this(message, cause, false);
	}

	//
	// INTERNALS
	//

	private static String makeMessage(String message, Throwable cause,
	        boolean isSuneidoRethrown) {
		if (isSuneidoRethrown || null == cause) {
			return message;
		} else {
			return message + " (" + cause + ")";
		}
	}

	//
	// ACCESSORS
	//

	// TODO: If removing this doesn't break anything, delete it
	// public Object get() {
	// return getMessage();
	// }

	//
	// STATICS
	//

	/**
	 * Convenience method for creating a "method not found" error.
	 * 
	 * @param object
	 *            Type of the object on which the method call was attempted
	 * @param method
	 *            Name of the method
	 * @return An exception that can be thrown to indicate that a message was
	 *         not found
	 */
	public static SuException methodNotFound(Object object, String method) {
		return new SuException("method not found: " + Ops.typeName(object)
		        + "." + method + " (" + object + ")");
	}

	//
	// INTERFACE: CallstackProvider
	//

	@Override
	public Callstack getCallstack() {
		return callstack;
	}

	@Override
	public void printCallstack(PrintWriter p) {
		p.println(getMessage());
		for (Frame frame : callstack) {
			p.print("\t at ");
			p.println(frame);
		}
	}

	//
	// ANCESTOR CLASS: Throwable
	//

	@Override
	public Throwable fillInStackTrace() {
		return this; // We will fill it in when needed.
	}

	//
	// ANCESTOR CLASS: Object
	//

	@Override
	public String toString() {
		return getMessage();
	}
}
