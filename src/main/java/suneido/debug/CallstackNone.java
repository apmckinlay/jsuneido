/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

/**
 * <p>
 * Call stack with the data required for "none" debugging. Corresponds to
 * {@link DebugModel#NONE}.
 * </p>
 *
 * <p>
 * This call stack implementation simply regurgitates whatever Java stack frame
 * data is available.
 * </p>
 *
 * @author Victor Schappert
 * @since 20140904
 */
public class CallstackNone extends Callstack {

	//
	// DATA
	//

	protected final Throwable throwable;

	protected static LocalVariable[] EMPTY_LOCAL_ARRAY = new LocalVariable[0];

	//
	// CONSTRUCTORS
	//

	CallstackNone(Throwable throwable) {
		this.throwable = throwable;
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected Frame[] makeFrames() {
		final StackTraceElement[] stackTrace = throwable.getStackTrace();
		final Frame[] frames = new Frame[stackTrace.length];
		for (int k = 0; k < stackTrace.length; ++k) {
			frames[k] = new Frame(null, stackTrace[k].getLineNumber(),
			        EMPTY_LOCAL_ARRAY);
		}
		return frames;
	}

}
