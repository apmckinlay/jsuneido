/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import suneido.SuInternalError;
import suneido.runtime.SuCallable;
import suneido.runtime.SuClass;

/**
 * Call stack with all the data required for "full" debugging. Corresponds to
 * {@link DebugModel#ALL}.
 *
 * @author Victor Schappert
 * @since 20140903
 */
public final class CallstackAll extends Callstack {

	//
	// DATA
	//

	private StackInfo stackInfo;

	//
	// CONSTRUCTORS
	//

	CallstackAll() {
		stackInfo = new StackInfo();
	}

	//
	// TYPES
	//

	private static final class FrameAll extends Frame {
		private final SuCallable callable; // Null if not determinable
		private final String className; // Null if not a member function
		private final String functionName; // Never null
		private final int lineNumber; // Valid only if >0

		FrameAll(SuCallable callable, int lineNumber, LocalVariable[] locals) {
			super(locals);
			if (null == callable) {
				throw new SuInternalError("callable cannot be null");
			}
			this.callable = callable;
			String[] typeAndFunctionName = callableToNames(callable);
			this.className = typeAndFunctionName[0];
			this.functionName = typeAndFunctionName[1];
			this.lineNumber = lineNumber;
		}

		private static String[] callableToNames(SuCallable callable) {
			String className = null;
			String functionName = null;
			// Get the class name
			SuClass clazz = callable.suClass();
			if (null != clazz) {
				className = clazz.valueName();
			}
			// Get the callable function name
			functionName = callable.valueName();
			return new String[] { className, functionName };
		}

		public Object getFrame() {
			return callable;
//			if (null != className) {
//				return className + '.' + functionName;
//			} else {
//				return functionName;
//			}
		}

		public int getLineNumber() {
			return lineNumber;
		}
	
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(getFrame());
			if (0 < lineNumber) {
				builder.append(':').append(lineNumber);
			}
			return builder.toString();
		}
	}

	//
	// INTERNALS
	//

	private void checkStackInfo() {
		if (!stackInfo.isInitialized()) {
			throw new SuInternalError(
			        "it should not be possible to instantiate CallstackAll without initializing stackInfo");
		}
		if (stackInfo.localsNames.length != stackInfo.localsValues.length
		        || stackInfo.localsNames.length != stackInfo.frameObjects.length) {
			throw new SuInternalError("array length mismatch");
		}
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected Frame[] makeFrames() {
		checkStackInfo();
		int nFrames = 0;
		// Count the number of frames
		for (Object method : stackInfo.frameObjects) {
			if (null != method) {
				++nFrames;
			}
		}
		// Create an array to hold the frames
		Frame[] frames = new Frame[nFrames];
		// Add the frames
		int frame = 0;
		for (int i = 0; i < stackInfo.frameObjects.length; ++i) {
			if (null == stackInfo.frameObjects[i]) {
				continue;
			}
			SuCallable callable = (SuCallable) stackInfo.frameObjects[i];
			// Count the number of actual local variables
			final String[] localsNames = stackInfo.localsNames[i];
			final Object[] localsValues = stackInfo.localsValues[i];
			int nLocals = 0;
			for (; nLocals < localsNames.length && null != localsNames[nLocals]; ++nLocals)
				;
			// Create an array to hold the locals
			final LocalVariable[] locals = new LocalVariable[nLocals];
			for (int local = 0; local < nLocals; ++local) {
				locals[local] = new LocalVariable(localsNames[local],
				        localsValues[local]);
			}
			// TODO: Line number required for frame
			// Add the frame
			frames[frame++] = new FrameAll(callable, stackInfo.lineNumbers[i],
			        locals);
		}
		return frames;
	}
}
