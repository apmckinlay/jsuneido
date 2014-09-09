/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.ArrayList;

import suneido.SuInternalError;
import suneido.runtime.ArgsArraySpec;
import suneido.runtime.FunctionSpec;
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
	// TYPES
	//

	private static final class FrameAll extends Frame {
		private final SuCallable callable; // Null if not determinable
		private final int lineNumber; // Valid only if >0

		FrameAll(SuCallable callable, int lineNumber, LocalVariable[] locals) {
			super(locals);
			if (null == callable) {
				throw new SuInternalError("callable cannot be null");
			}
			this.callable = callable;
			this.lineNumber = lineNumber;
		}

		public Object getFrame() {
			return callable;
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
				|| stackInfo.localsNames.length != stackInfo.annotations.length) {
			throw new SuInternalError("array length mismatch");
		}
	}

	private static void addLocal(String name, Object value,
			ArrayList<LocalVariable> locals) {
		if (null != value) {
			locals.add(new LocalVariable(name, value));
		}
	}

	private static SuCallable collectLocalsAndCallable(Locals annotation,
			String[] names, Object[] values, ArrayList<LocalVariable> locals) {
		SuCallable javaThis = null;
		Object[] argsArray = null;
		// Pick out the SuCallable whose method was called (Java's "this"), the
		// "self" argument to the method (Suneido's "this"), and the args array,
		// if any.
		int localIndex = 0;
		for (; localIndex < values.length; ++localIndex) {
			if ("this".equals(names[localIndex])
					&& values[localIndex] instanceof SuCallable) {
				javaThis = (SuCallable) values[localIndex];
			} else if (annotation.isSelfCall()
					&& "_self_".equals(names[localIndex])) {
				assert null != values[localIndex];
				locals.add(new LocalVariable("this", values[localIndex]));
			} else if (!annotation.argsArray().isEmpty()
					&& annotation.argsArray().equals(names[localIndex])
					&& values[localIndex] instanceof Object[]) {
				argsArray = (Object[]) values[localIndex];
			} else {
				break;
			}
		}
		if (null == javaThis) {
			if (Locals.SourceLanguage.SUNEIDO == annotation.sourceLanguage()) {
				throw new SuInternalError(
						"No \"this\" local found in bytecode compiled from Suneido source");
			} else {
				throw new SuInternalError(
						"No \"this\" local found in bytecode compiled from Java source - recompile with 'javac -g' or '-g:vars'?");
			}
		}
		// Get the FunctionSpec, which will tell us the parameter names.
		FunctionSpec fs = javaThis.getParams();
		final int NPARAMS = fs.getParamCount();
		if (null == argsArray) {
			// First traverse the FunctionSpec to get parameter names...
			assert annotation.argsArray().isEmpty();
			for (int paramIndex = 0; paramIndex < NPARAMS
					&& localIndex < values.length; ++paramIndex, ++localIndex) {
				addLocal(fs.getParamName(paramIndex), values[localIndex],
						locals);
			}
			if (!annotation.ignoreNonParams()) {
				// Then add any remaining local variables as ordinary locals.
				for (; localIndex < values.length; ++localIndex) {
					addLocal(names[localIndex], values[localIndex], locals);
				}
			}
		} else /* argsArray isn't null */{
			ArgsArraySpec aas = (ArgsArraySpec) fs;
			for (int paramIndex = 0; paramIndex < NPARAMS; ++paramIndex) {
				addLocal(aas.getParamName(paramIndex),
						aas.getParamValueFromArgsArray(argsArray, paramIndex),
						locals);
			}
			final int NUPVALUES = aas.getUpvalueCount();
			for (int upvalueIndex = 0; upvalueIndex < NUPVALUES; ++upvalueIndex) {
				addLocal(aas.getUpvalueName(upvalueIndex),
						aas.getUpvalueFromArgsArray(argsArray, upvalueIndex),
						locals);
			}
			final int NLOCALS = aas.getLocalCount();
			for (localIndex = 0; localIndex < NLOCALS; ++localIndex) {
				final String name = aas.getLocalName(localIndex);
				if ('_' == name.charAt(0)) {
					continue;
				}
				addLocal(aas.getLocalName(localIndex),
						aas.getLocalValueFromArgsArray(argsArray, localIndex),
						locals);
			}
		}
		// Done
		return javaThis;
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected Frame[] makeFrames() {
		checkStackInfo();
		int nFrames = 0;
		// Count the number of frames
		for (Object annotation : stackInfo.annotations) {
			if (null != annotation) {
				++nFrames;
			}
		}
		// Create an array to hold the frames
		Frame[] frames = new Frame[nFrames];
		// Create temporary storage for the local variables
		ArrayList<LocalVariable> locals = new ArrayList<>(8);
		// Add the frames
		int frame = 0;
		for (int i = 0; i < stackInfo.annotations.length; ++i) {
			if (null == stackInfo.annotations[i]) {
				continue;
			}
			// Extract the callable and the local variables
			locals.clear();
			SuCallable javaThis = collectLocalsAndCallable(
					(Locals) stackInfo.annotations[i],
					stackInfo.localsNames[i], stackInfo.localsValues[i], locals);
			// Add the frame
			frames[frame++] = new FrameAll(javaThis, stackInfo.lineNumbers[i],
					locals.toArray(new LocalVariable[locals.size()]));
		}
		return frames;
	}
}
