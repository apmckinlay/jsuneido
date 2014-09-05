/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import suneido.SuInternalError;
import suneido.runtime.SuCallable;

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
			frames[frame++] = new Frame(callable, stackInfo.lineNumbers[i],
			        locals);
		}
		return frames;
	}
}
