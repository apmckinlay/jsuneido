/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import suneido.SuValue;
import suneido.TheDbms;
import suneido.compiler.AstCompile;
import suneido.compiler.ClassGen;
import suneido.database.server.Dbms.LibGet;
import suneido.jsdi.type.Structure;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.CallableType;
import suneido.runtime.Pack;

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
		CallableType callableTypeAbove = CallableType.UNKNOWN;
		CallableType callableTypeCur = CallableType.UNKNOWN;
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

		private boolean hasCompilerNameSuffix(String fullyQualifiedClassName) {
			for (CallableType callableType : CallableType.values()) {
				if (fullyQualifiedClassName.endsWith(callableType
						.compilerNameSuffix())) {
					callableTypeCur = callableType;
					return true;
				}
			}
			return false;
		}

		private static final Pattern libraryPattern = Pattern
				.compile("library\\[(^\\]]+)\\]->(.*)");

		private static String[] extractLibraryFromFromFile(String sourceFileName) {
			// NOTE: For this to be really useful, it would be nice to be able
			// to include a version tag in the compiled code "file name"
			// so we can look up the correct version of the source code.
			Matcher m = libraryPattern.matcher(sourceFileName);
			String[] result = new String[2];
			if (m.matches()) {
				result[0] = m.group(1);
				result[1] = m.group(2);
			}
			return result;
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
				callableTypeCur = CallableType.METHOD;
				valueNameCur = fullyQualifiedClassName.substring(lastDot + 1,
						methodSep)
						+ '.'
						+ fullyQualifiedClassName.substring(methodSep + 1);
			} else if (hasCompilerNameSuffix(fullyQualifiedClassName)) {
				valueNameCur = methodNameCur.isCall() ? "call" : "eval";
			} else {
				// Library function
				callableTypeCur = CallableType.FUNCTION;
				valueNameCur = fullyQualifiedClassName.substring(lastDot + 1);
			}
			if (isNewSuneidoFrame()) {
				String[] libraryAndItem = extractLibraryFromFromFile(element
						.getFileName());
				addFrame(new FrameStack(valueNameCur, callableTypeCur,
						libraryAndItem[0], libraryAndItem[1],
						element.getLineNumber()));
			}
		}
	}

	private static final class FrameStack extends Frame {

		private final String valueName;
		private final CallableType callableType;
		private final String library;
		private final String libraryItemName;
		private final int lineNumber;

		public FrameStack(String valueName, CallableType callableType,
				String library, String libraryItemName, int lineNumber) {
			super(CallstackNone.EMPTY_LOCAL_ARRAY);
			this.valueName = valueName;
			this.callableType = callableType;
			this.library = library;
			this.libraryItemName = libraryItemName;
			this.lineNumber = lineNumber;
		}

		//
		// ANCESTOR CLASS: Frame
		//

		@Override
		public Object getFrame() {
			return new PseudoFunction(this);
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		//
		// INTERNAL TYPES
		//

		private static final class PseudoFunction extends SuValue {

			//
			// DATA
			//

			private final FrameStack frame;

			//
			// CONSTRUCTORS
			//

			private PseudoFunction(FrameStack frame) {
				this.frame = frame;
			}

			//
			// INTERNALS
			//

			private String sourceCode() {
				if (null != frame.library && null != frame.libraryItemName) {
					List<LibGet> candidates = TheDbms.dbms().libget(
							frame.libraryItemName);
					for (LibGet lg : candidates) {
						if (lg.library.equals(frame.library)) {
							return (String) Pack.unpack(lg.text);
						}
					}
				}
				return "// SOURCE NOT AVAILABLE"; // TODO improve this
			}

			//
			// ANCESTOR CLASS: SuValue
			//

			@Override
			public SuValue lookup(String method) {
				return builtins.lookup(method);
			}

			@Override
			public String display() {

			}

			//
			// BUILT-IN METHODS
			//

			private static final BuiltinMethods builtins = new BuiltinMethods(
					PseudoFunction.class);

			public static Object Source(Object self) {
				return ((PseudoFunction) self).sourceCode();
			}
		}
	}
}
