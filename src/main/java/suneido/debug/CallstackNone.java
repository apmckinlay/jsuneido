/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.Arrays;
import java.util.List;

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
	// CONSTANTS
	//

	static final LocalVariable[] EMPTY_LOCAL_ARRAY = new LocalVariable[0];

	//
	// CONSTRUCTORS
	//

	CallstackNone(Throwable throwable) {
		super(throwable);
	}

	//
	// TYPES
	//

	private static final class StackTraceElementWrapper extends Frame {
		private final StackTraceElement ste;

		StackTraceElementWrapper(StackTraceElement ste) {
			super(CallstackNone.EMPTY_LOCAL_ARRAY);
			this.ste = ste;
		}

		public Object getFrame() {
			return ste.getClassName() + "." + ste.getMethodName() + " ("
			        + ste.getFileName() + ")";
		}

		public int getLineNumber() {
			return ste.getLineNumber();
		}

		@Override
		public String toString() {
			return ste.toString();
		}
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected List<Frame> makeFrames() {
		final StackTraceElement[] stackTrace = throwable.getStackTrace();
		final Frame[] frames = new Frame[stackTrace.length];
		for (int k = 0; k < stackTrace.length; ++k) {
			frames[k] = new StackTraceElementWrapper(stackTrace[k]);
		}
		return Arrays.asList(frames);
	}
}
