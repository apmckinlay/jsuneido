/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.compiler.ClassGen;
import suneido.runtime.Args;
import suneido.runtime.ArgsArraySpec;
import suneido.runtime.ArgsIterator;
import suneido.runtime.CallableType;
import suneido.runtime.FunctionSpec;
import suneido.runtime.SuCallable;

/**
 * Call stack with all the data required for "full" debugging. Corresponds to
 * {@link DebugModel#ON}.
 *
 * @author Victor Schappert
 * @since 20140903
 */
public final class CallstackAll extends Callstack {

	//
	// DATA
	//

	private final StackInfo stackInfo;

	//
	// CONSTRUCTORS
	//

	CallstackAll(StackInfo stackInfo, Throwable throwable) {
		super(throwable);
		this.stackInfo = stackInfo.fetchInfo();
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
				|| stackInfo.localsNames.length != stackInfo.isCall.length
				|| stackInfo.localsNames.length != stackInfo.lineNumbers.length) {
			throw new SuInternalError("array length mismatch");
		}
	}

	private static final String ARGS_CLASS_NAME = Args.class.getName().intern();

	private boolean isArgsProblem() {
		// Determine if the issue is too many arguments, insufficient arguments,
		// or a problem massaging the arguments.
		if (!(throwable instanceof SuException)) {
			return false;
		}
		StackTraceElement[] trace = throwable.getStackTrace();
		return 0 < trace.length
				&& ARGS_CLASS_NAME.equals(trace[0].getClassName());
	}

	private int findFirstFrame(int startIndex) {
		for (; startIndex < stackInfo.localsValues.length; ++startIndex) {
			if (null != stackInfo.localsValues[startIndex]) {
				break;
			}
		}
		return startIndex;
	}

	private static void addLocal(String name, Object value,
			ArrayList<LocalVariable> locals) {
		if (null != value) {
			locals.add(new LocalVariable(name, value));
		}
	}

	private SuCallable collectLocalsAndCallable(boolean isCall, String[] names,
			Object[] values, ArrayList<LocalVariable> locals) {
		SuCallable javaThis = null;
		CallableType javaThisType = null;
		FunctionSpec fs = null;
		Object[] argsArray = null;
		// Pick out the SuCallable whose method was called (Java's "this"), the
		// "self" argument to the method (Suneido's "this"), and the args array,
		// if any.
		int localIndex = 0;
		for (; localIndex < values.length; ++localIndex) {
			if ("this".equals(names[localIndex])
					&& values[localIndex] instanceof SuCallable) {
				javaThis = (SuCallable) values[localIndex];
				javaThisType = javaThis.callableType();
				if (null == javaThisType) {
					throw new SuInternalError(
							"Java \"this\" of class "
									+ javaThis.getClass().getName()
									+ "has no callable type (anonymous subclass of SuCallable?)");
				}
				// Don't display locals for closures, bound methods, and any
				// other callables that are just a wrappers around the "true"
				// callable, which we already extracted in the stack frame
				// above.
				if (javaThisType.isWrapper()) {
					return null;
				}
				fs = javaThis.getParams();
			} else if (!isCall
					&& (ClassGen.SELF_VAR_NAME.equals(names[localIndex]) || "self"
							.equals(names[localIndex]))) {
				assert null != values[localIndex];
				locals.add(new LocalVariable("this", values[localIndex]));
			} else if ((fs instanceof ArgsArraySpec || null == fs)
					&& values[localIndex] instanceof Object[]) {
				argsArray = (Object[]) values[localIndex];
			} else {
				break;
			}
		}
		if (null == javaThis || null == javaThisType) {
			throw new SuInternalError("No Java \"this\" local variable found",
					throwable);
		} else if (null == fs && !javaThisType.isBuiltin()) {
			throw new SuInternalError(
					"No FunctionSpec found for non-builtin Java \"this\": "
							+ javaThis, throwable);
		}
		if (null == fs) {
			// At the moment, some built-ins have no FunctionSpec. This
			// situation should be fixed in the long run by creating a more
			// descriptive syntax for the @Params annotation; improving the
			// FunctionSpec hierarchy; and forcing all built-ins to carry a
			// FuntionSpec. For the time being, however, we'll just call the
			// unnamed arguments "arg0", "arg1", and so on.
if (null == argsArray) {
System.out.println(javaThis + " ; " + javaThis.getClass().getName() + " ; has a NULL args array");
throwable.printStackTrace();
}
			assert null != argsArray;
			ArgsIterator iter = new ArgsIterator(argsArray);
			localIndex = 0;
			while (iter.hasNext()) {
				Object arg = iter.next();
				if (arg instanceof Map.Entry<?, ?>) {
					Map.Entry<?, ?> entry = (Map.Entry<?, ?>)arg;
					addLocal(entry.getKey().toString(), entry.getValue(), locals);
				} else {
					// Technically these made-up names could collide with
					// the named arguments...
					addLocal("arg" + localIndex++, arg, locals);
				}
			}
		}
		else if (null == argsArray) {
			final int NPARAMS = fs.getParamCount();
			// First traverse the FunctionSpec to get parameter names...
			for (int paramIndex = 0; paramIndex < NPARAMS
					&& localIndex < values.length; ++paramIndex, ++localIndex) {
				addLocal(fs.getParamName(paramIndex), values[localIndex],
						locals);
			}
			// Then add any remaining local variables as ordinary locals.
			for (; localIndex < values.length; ++localIndex) {
				addLocal(names[localIndex], values[localIndex], locals);
			}
		} else /* argsArray isn't null */{
			final int NPARAMS = fs.getParamCount();
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
	protected List<Frame> makeFrames() {
		checkStackInfo();
		int nFrames = 0;
		// Count the number of frames. This number is approximate because we
		// may still need to drop certain frames (e.g. closures).
		for (Object o : stackInfo.localsValues) {
			if (null != o) {
				++nFrames;
			}
		}
		// Find the first frame
		int srcFrame = findFirstFrame(0);
		// If the problem was caused by the arguments to the callable, then drop
		// the first frame since from a JVM execution model perspective, the
		// exception was thrown within the method that implements the callable.
		// However from a "logical" Suneido perspective, the exception is really
		// thrown at point of call.
		if (isArgsProblem() && 0 < nFrames) {
			--nFrames;
			srcFrame = findFirstFrame(srcFrame + 1);
		}
		// Create a list to hold the frames
		List<Frame> frames = new ArrayList<>(nFrames);
		// Create temporary storage for the local variables
		ArrayList<LocalVariable> locals = new ArrayList<>(8);
		// Add the frames
		for (; srcFrame < stackInfo.localsValues.length; ++srcFrame) {
			if (null == stackInfo.localsValues[srcFrame]) {
				continue;
			}
			// Extract the callable and the local variables
			locals.clear();
			SuCallable javaThis = collectLocalsAndCallable(
					stackInfo.isCall[srcFrame],
					stackInfo.localsNames[srcFrame],
					stackInfo.localsValues[srcFrame], locals);
			if (null != javaThis) {
				// Add the frame
				frames.add(new FrameAll(javaThis,
						stackInfo.lineNumbers[srcFrame], locals
								.toArray(new LocalVariable[locals.size()])));
			}
		}
		return frames;
	}
}