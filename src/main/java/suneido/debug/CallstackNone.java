/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.debug;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

import suneido.SuInternalError;
import suneido.SuValue;
import suneido.TheDbms;
import suneido.compiler.AstCompile;
import suneido.database.server.Dbms.LibGet;
import suneido.runtime.BuiltinMethods;
import suneido.runtime.CallableType;
import suneido.runtime.Ops;
import suneido.runtime.Pack;

/**
 * <p>
 * Call stack with the data required for "none" debugging. Corresponds to
 * {@link DebugModel#OFF}.
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
@Immutable
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

	@Immutable
	private static final class StackTraceElementWrapper extends Frame {

		private final PseudoFunction pf;

		StackTraceElementWrapper(StackTraceElement ste) {
			super(CallstackNone.EMPTY_LOCAL_ARRAY);
			this.pf = new PseudoFunction(ste);
		}

		//
		// ANCESTOR CLASS: Frame
		//

		@Override
		public SuValue getFrame() {
			return pf;
		}

		@Override
		public int getLineNumber() {
			return pf.ste.getLineNumber();
		}

		@Override
		public CallableType getCallableType() {
			for (CallableType ct : CallableType.values()) {
				final String suffix = ct.compilerNameSuffix();
				if (null != suffix && pf.ste.getClassName().endsWith(suffix)) {
					return ct;
				}
			}
			if (0 <= pf.ste.getMethodName()
			        .indexOf(AstCompile.METHOD_SEPARATOR)) {
				return CallableType.METHOD;
			}
			return CallableType.UNKNOWN;
		}

		//
		// ANCESTOR CLASS: Object
		//

		@Override
		public String toString() {
			return pf.ste.toString();
		}
	}

	@Immutable
	public static final class PseudoFunction extends SuValue {

		private final StackTraceElement ste;

		private PseudoFunction(StackTraceElement ste) {
			this.ste = ste;
		}

		// private static final Pattern LIBPAT = Pattern.compile(
		// "library\\[([^\\]]+)\\]->(.+)");
		private Object sourceCode() {
			final Pattern LIBPAT = Pattern
			        .compile("library\\[([^\\]]+)\\]->(.+)");
			Matcher m = LIBPAT.matcher(ste.getFileName());
			if (m.matches()) {
				final String library = m.group(1);
				final String item = m.group(2);
				List<LibGet> libgets = TheDbms.dbms().libget(item);
				for (LibGet libget : libgets) {
					if (library.equals(libget.library)) {
						return Pack.unpack(libget.text);
					}
				}
			}
			return Boolean.FALSE;
		}

		//
		// ANCESTOR CLASS: SuValue
		//

		@Override
		public String typeName() {
			return "aPseudoFunction";
		}

		@Override
		public String display() {
			StringBuilder sb = new StringBuilder(128);
			sb.append(ste.getClassName()).append('.')
			        .append(ste.getMethodName());
			if (null != ste.getFileName() || 0 < ste.getLineNumber()) {
				sb.append(" (");
				if (null != ste.getFileName()) {
					sb.append(ste.getFileName());
				}
				if (0 < ste.getLineNumber()) {
					sb.append(':').append(ste.getLineNumber());
				}
			}
			return sb.append(')').toString();
		}

		private static final BuiltinMethods builtins = new BuiltinMethods(
		        PseudoFunction.class);

		@Override
		public SuValue lookup(String method) {
			return builtins.lookup(method);
		}

		//
		// BUILT-IN METHODS
		//

		public static Object Source(Object self) {
			return ((PseudoFunction) self).sourceCode();
		}

		public static Object Disasm(Object self) {
			return Boolean.FALSE;
		}
	}

	//
	// INTERNALS
	//

	// The reason for having statically-typed references to the Ops "exception"
	// method (when all we really need for this class is strings) is so that
	// this class will survive refactoring if, e.g., the Ops class or
	// "exception" method get renamed.
	private static final Class<Ops> OPS_CLASS = Ops.class;
	private static Method OPS_EXCEPT_METHOD;

	private static synchronized Method getOpsExceptionMethod() {
		if (null == OPS_EXCEPT_METHOD) {
			try {
				OPS_EXCEPT_METHOD = OPS_CLASS.getDeclaredMethod("exception",
				        Object.class);
			} catch (NoSuchMethodException e) {
				throw new SuInternalError("can't get Ops exception method", e);
			}
		}
		return OPS_EXCEPT_METHOD;
	}

	private static String getOpsExceptionMethodName() {
		return getOpsExceptionMethod().getName();
	}

	//
	// ANCESTOR CLASS: Callstack
	//

	@Override
	protected List<Frame> makeFrames() {
		final StackTraceElement[] stackTrace = throwable.getStackTrace();
		final Frame[] frames = new Frame[stackTrace.length];
		int i = 0; // stackTraceElement #
		int j = 0; // frame #
		if (0 < stackTrace.length
		        && stackTrace[0].getClassName().equals(OPS_CLASS.getName())
		        && stackTrace[0].getMethodName().equals(
		                getOpsExceptionMethodName())) {
			++i; // Skip over first stack trace element if it is Ops.exception()
		}
		for (; i < stackTrace.length; ++i, ++j) {
			frames[j] = new StackTraceElementWrapper(stackTrace[i]);
		}
		return Arrays.asList(frames);
	}
}
