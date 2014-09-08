/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

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
	 * @return
	 */
	public Callstack getCallstack();

}
