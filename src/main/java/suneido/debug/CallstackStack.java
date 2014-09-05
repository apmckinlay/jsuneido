/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

/**
 * <p>
 * Call stack with the data required for "stack" debugging. Corresponds to
 * {@link DebugModel#STACK}.
 * </p>
 *
 * <p>
 * This call stack implementation filters the Java stack trace as well as adding
 * some additional information gleaned from the classes and methods named in the
 * stack trace.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140904
 */
public final class CallstackStack extends CallstackNone {

	//
	// CONSTRUCTORS
	//

	CallstackStack(Throwable throwable) {
		super(throwable);
	}

	// TODO: implement getFrames() appropriately
}
