/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.io.PrintWriter;

/**
 * Interface implemented by a class that can provide a Suneido stack trace
 * ({@link Callstack}).
 * 
 * @author Victor Schappert
 * @since 20140903
 */
public interface CallstackProvider {

	/**
	 * Returns the stack trace attached to this provider.
	 *
	 * @return Call stack
	 */
	public Callstack getCallstack();

	/**
	 * Prints this call stack to the specified stream.
	 *
	 * @param p Stream to print the call stack too.
	 * @see Throwable#printStackTrace(java.io.PrintWriter)
	 */
	public void printCallstack(PrintWriter p);
}
