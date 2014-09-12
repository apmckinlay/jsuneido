/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.ArrayList;
import java.util.List;

import suneido.compiler.AstCompile;
import suneido.compiler.ClassGen;

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
public final class CallstackStack extends Callstack {

	//
	// CONSTRUCTORS
	//

	CallstackStack(Throwable throwable) {
		super(throwable);
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected List<Frame> makeFrames() {
		return new FrameFilterer(throwable).filter();
	}

	//
	// INTERNALS
	//

	// private static final Pattern

	private static final class FrameFilterer {
		final StackTraceElement[] stackTrace;
		int stackTraceIndex;
		final List<Frame> frames;
		MethodName methodNameAbove = MethodName.UNKNOWN;
		MethodName methodNameCur = MethodName.UNKNOWN;
		String callableTypeAbove = null; // "callableType" means function,
											// method, etc.
		String callableTypeCur = null; // (there should really be an enum in
										// runtime for this)
		String valueNameAbove = null;
		String valueNameCur = null;

		public FrameFilterer(Throwable throwable) {
			stackTrace = throwable.getStackTrace();
			stackTraceIndex = 0;
			frames = new ArrayList<>(stackTrace.length);
		}

		public List<Frame> filter() {
			for (StackTraceElement element : stackTrace) {
				filter(element);
				++stackTraceIndex;
			}
			return frames;
		}

		private StackTraceElement lookBelow() {
			// Return the next stack frame below this one, if any
			return stackTraceIndex + 1 < stackTrace.length ? stackTrace[stackTraceIndex + 1]
					: null;
		}

		private boolean isNewSuneidoFrame() {
			return valueNameCur != null
					&& (!valueNameCur.equals(valueNameAbove) || methodNameCur != methodNameAbove);
		}

		private void addFrame(FrameStack frame) {
			frames.add(frame);
		}

		private String determineTypeOfFunction(String fullyQualifiedClassName) {
			throw new Error("not implemented yet");
		}

		private void filter(StackTraceElement element) {
			methodNameAbove = methodNameCur;
			methodNameCur = MethodName.getMethodName(element.getMethodName());
			// Skip obvious non-stack frames...
			if (MethodName.UNKNOWN == methodNameCur) {
				return;
			}
			final String fullyQualifiedClassName = element.getClassName();
			// Skip non-Suneido stack frames
			if (!fullyQualifiedClassName.startsWith("suneido.")) {
				return;
			}
			// Handle compiled Suneido code
			if (fullyQualifiedClassName
					.startsWith(ClassGen.COMPILED_CODE_PACKAGE_DOTS)) {
				filterCompiledSuneidoCode(element);
			}
		}

		private void filterCompiledSuneidoCode(StackTraceElement element) {
			final String fullyQualifiedClassName = element.getClassName();
			final int lastDot = fullyQualifiedClassName.lastIndexOf('.');
			assert 0 < lastDot;
			callableTypeCur = null;
			valueNameCur = null;
			final int methodSep = fullyQualifiedClassName.indexOf(
					AstCompile.METHOD_SEPARATOR, lastDot + 1);
			if (0 <= methodSep) {
				callableTypeCur = "method";
				valueNameCur = fullyQualifiedClassName.substring(lastDot + 1,
						methodSep)
						+ '.'
						+ fullyQualifiedClassName.substring(methodSep + 1);
			} else if (fullyQualifiedClassName.endsWith("$f")) {
				// Function or block. At the moment, the only way to distinguish
				// between the two is the following:
				// 1) If the next frame below is a closure, it's a block.
				// 2) Otherwise, load the class and determine if the
				// immediate superclass is SuFunctionN or just SuCallable.
				callableTypeCur = determineTypeOfFunction(fullyQualifiedClassName);
				valueNameCur = methodNameCur.isCall() ? "call" : "eval";
			} else {
				// Library function
				callableTypeCur = "function";
				valueNameCur = fullyQualifiedClassName.substring(lastDot + 1);
			}
			if (isNewSuneidoFrame()) {
				addFrame(new FrameStack(valueNameCur, callableTypeCur, element.getFileName(), element.getLineNumber()));
			}
		}
	}

	private static final class FrameStack extends Frame {

		final int lineNumber;

		public FrameStack(String valueName, String callableType,
				String library, int lineNumber) {
			super(CallstackNone.EMPTY_LOCAL_ARRAY);
			this.lineNumber = lineNumber;
		}

		@Override
		public Object getFrame() {
			return "abc";
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}
	}
}
